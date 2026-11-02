-- Sanitized, isolated fixture data for migration preflight tests.
-- Every table is prefixed so this script cannot shadow or modify application tables.
CREATE TEMPORARY TABLE fixture_legacy_address (
    address_id BIGINT PRIMARY KEY,
    street_number VARCHAR(200) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    city VARCHAR(120) NOT NULL,
    province VARCHAR(120) NOT NULL
);

CREATE TEMPORARY TABLE fixture_legacy_customer (
    customer_id BIGINT PRIMARY KEY,
    customer_name VARCHAR(200) NOT NULL,
    fk_address_id BIGINT NOT NULL
);

CREATE TEMPORARY TABLE fixture_legacy_person (
    customer_id BIGINT PRIMARY KEY
);

CREATE TEMPORARY TABLE fixture_legacy_company (
    customer_id BIGINT PRIMARY KEY
);

CREATE TEMPORARY TABLE fixture_legacy_customer_accounts (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    username VARCHAR(260) NOT NULL,
    address_id BIGINT NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone_number VARCHAR(16) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TEMPORARY TABLE fixture_legacy_bank_account (
    account_id BIGINT PRIMARY KEY,
    fk_cust_id BIGINT NOT NULL
);

INSERT INTO fixture_legacy_address VALUES
    (1, '10 Example Street', 'A1A 1A1', 'Example City', 'Example Province'),
    (2, '20 Sample Road', 'B2B 2B2', 'Sample City', 'Sample Province'),
    (3, '30 Test Lane', 'C3C 3C3', 'Test City', 'Test Province'),
    (4, '20 Sample Road', 'B2B 2B2', 'Sample City', 'Sample Province');

INSERT INTO fixture_legacy_customer VALUES
    (101, 'Exact Match Customer', 1),
    (201, 'Ambiguous Match Customer', 2),
    (202, 'Ambiguous Match Customer', 4),
    (301, 'Unmatched Profile Customer', 3),
    (401, 'Company Match Customer', 4);

INSERT INTO fixture_legacy_person VALUES (101), (201), (202), (301);
INSERT INTO fixture_legacy_company VALUES (401);

INSERT INTO fixture_legacy_customer_accounts
    (id, name, username, address_id, email, phone_number, password_hash, role, status)
VALUES
    (1001, 'Exact Match Customer', 'exactmatch001', 1,
        'exact@example.test', '+15551234001', '{bcrypt}fixture-hash-1', 'CUSTOMER', 'ACTIVE'),
    (2001, 'Ambiguous Match Customer', 'ambiguous001', 2,
        'ambiguous@example.test', '+15551234002', '{bcrypt}fixture-hash-2', 'CUSTOMER', 'ACTIVE'),
    (3001, 'No Profile Customer', 'noprofile001', 3,
        'unmatched@example.test', '+15551234003', '{bcrypt}fixture-hash-3', 'CUSTOMER', 'ACTIVE'),
    (4001, 'Company Match Customer', 'companymatch001', 4,
        'company@example.test', '+15551234004', '{bcrypt}fixture-hash-4', 'CUSTOMER', 'ACTIVE');

INSERT INTO fixture_legacy_bank_account VALUES (9001, 101), (9002, 301), (9003, 401), (9004, 9999);
