-- One-time MySQL 8 cutover. Take a restorable backup and complete the
-- customer-consolidation-preflight.sql review before running this script.
-- The legacy customer_accounts table is deliberately retained as a snapshot.

DROP TEMPORARY TABLE IF EXISTS customer_consolidation_matches;
CREATE TEMPORARY TABLE customer_consolidation_matches AS
SELECT
    ca.id AS legacy_account_id,
    MIN(c.customer_id) AS customer_id,
    COUNT(c.customer_id) AS match_count
FROM customer_accounts ca
LEFT JOIN address account_address
    ON account_address.address_id = ca.address_id
LEFT JOIN customer c
    ON BINARY c.customer_name = BINARY ca.name
   AND EXISTS (
       SELECT 1
       FROM address profile_address
       WHERE profile_address.address_id = c.fk_address_id
         AND account_address.address_id IS NOT NULL
         AND TRIM(account_address.street_number) <> ''
         AND TRIM(account_address.postal_code) <> ''
         AND TRIM(account_address.city) <> ''
         AND TRIM(account_address.province) <> ''
         AND TRIM(profile_address.street_number) <> ''
         AND TRIM(profile_address.postal_code) <> ''
         AND TRIM(profile_address.city) <> ''
         AND TRIM(profile_address.province) <> ''
         AND BINARY account_address.street_number = BINARY profile_address.street_number
         AND BINARY account_address.postal_code = BINARY profile_address.postal_code
         AND BINARY account_address.city = BINARY profile_address.city
         AND BINARY account_address.province = BINARY profile_address.province
   )
GROUP BY ca.id;

DELIMITER $$
DROP PROCEDURE IF EXISTS assert_customer_consolidation_ready$$
CREATE PROCEDURE assert_customer_consolidation_ready()
BEGIN
    IF EXISTS (SELECT 1 FROM customer_consolidation_matches WHERE match_count <> 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: one or more login rows have no unique exact profile match.';
    END IF;
    IF EXISTS (
        SELECT customer_id
        FROM customer_consolidation_matches
        GROUP BY customer_id
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: multiple login rows match one customer profile.';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM customer_accounts
        GROUP BY LOWER(TRIM(username))
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: duplicate usernames exist.';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM customer_accounts
        GROUP BY LOWER(TRIM(email))
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: duplicate normalized email addresses exist.';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM customer c
        LEFT JOIN person p ON p.customer_id = c.customer_id
        LEFT JOIN company co ON co.customer_id = c.customer_id
        WHERE (p.customer_id IS NULL AND co.customer_id IS NULL)
           OR (p.customer_id IS NOT NULL AND co.customer_id IS NOT NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: a customer has a missing or conflicting profile type.';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM customer c
        LEFT JOIN address a ON a.address_id = c.fk_address_id
        WHERE c.customer_name IS NULL OR TRIM(c.customer_name) = ''
           OR a.address_id IS NULL
           OR TRIM(COALESCE(a.street_number, '')) = ''
           OR TRIM(COALESCE(a.postal_code, '')) = ''
           OR TRIM(COALESCE(a.city, '')) = ''
           OR TRIM(COALESCE(a.province, '')) = ''
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: customer name or complete address data is missing.';
    END IF;
    IF EXISTS (
        SELECT fk_address_id
        FROM customer
        WHERE fk_address_id IS NOT NULL
        GROUP BY fk_address_id
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: an address is linked to multiple customers.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM `account` a
        LEFT JOIN customer c ON c.customer_id = a.fk_cust_id
        WHERE a.fk_cust_id IS NOT NULL AND c.customer_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cutover stopped: invalid bank-account customer references exist.';
    END IF;
END$$
DELIMITER ;

CALL assert_customer_consolidation_ready();
DROP PROCEDURE assert_customer_consolidation_ready;

-- Preserve the source-to-central identity mapping for verification and support.
CREATE TABLE customer_account_migration_map (
    legacy_account_id BIGINT NOT NULL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_account_map_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

INSERT INTO customer_account_migration_map (legacy_account_id, customer_id)
SELECT legacy_account_id, customer_id
FROM customer_consolidation_matches;

ALTER TABLE customer
    ADD COLUMN customer_type VARCHAR(16) NULL,
    ADD COLUMN username VARCHAR(260) NULL,
    ADD COLUMN email VARCHAR(320) NULL,
    ADD COLUMN phone_number VARCHAR(16) NULL,
    ADD COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_login_at TIMESTAMP NULL;

-- Person and company retain their joined-inheritance customer_id keys.
UPDATE customer c
LEFT JOIN person p ON p.customer_id = c.customer_id
LEFT JOIN company co ON co.customer_id = c.customer_id
SET c.customer_type = CASE
        WHEN p.customer_id IS NOT NULL THEN 'person'
        ELSE 'company'
    END;

UPDATE customer c
JOIN customer_account_migration_map m ON m.customer_id = c.customer_id
JOIN customer_accounts ca ON ca.id = m.legacy_account_id
SET c.username = ca.username,
    c.email = LOWER(TRIM(ca.email)),
    c.phone_number = ca.phone_number,
    c.password_hash = ca.password_hash,
    c.role = ca.role,
    c.status = ca.status,
    c.created_at = COALESCE(ca.created_at, c.created_at),
    c.last_login_at = ca.last_login_at;

ALTER TABLE customer
    MODIFY COLUMN customer_name VARCHAR(200) NOT NULL,
    MODIFY COLUMN customer_type VARCHAR(16) NOT NULL,
    MODIFY COLUMN fk_address_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_customer_address UNIQUE (fk_address_id),
    ADD CONSTRAINT uq_customer_username UNIQUE (username),
    ADD CONSTRAINT uq_customer_email UNIQUE (email);

CREATE TABLE customer_claim_request (
    claim_request_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    claim_reference_hash CHAR(64) NOT NULL UNIQUE,
    submitted_email VARCHAR(320) NOT NULL,
    submitted_phone_number VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    reviewed_by VARCHAR(260) NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_customer_claim_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE
);

SELECT
    (SELECT COUNT(*) FROM customer_accounts) AS source_login_count,
    (SELECT COUNT(*) FROM customer_account_migration_map) AS mapped_login_count,
    (SELECT COUNT(*) FROM customer WHERE username IS NOT NULL) AS central_login_count,
    (SELECT COUNT(*) FROM person) AS retained_person_profile_count,
    (SELECT COUNT(*) FROM company) AS retained_company_profile_count,
    (SELECT COUNT(*) FROM `account` WHERE fk_cust_id IS NOT NULL) AS assigned_bank_account_count;

-- Keep customer_accounts and this mapping table until migrated login,
-- profile, and bank-account checks are signed off. Remove them only in a
-- separately approved cleanup after the rollback window has closed.
