-- Read-only MySQL preflight. Run against a backed-up copy of the current schema.
-- Exact matching compares complete, trimmed address values byte-for-byte.

-- Each source login and its number of eligible legacy customer matches.
SELECT
    ca.id AS legacy_account_id,
    ca.username,
    ca.email,
    COUNT(c.customer_id) AS exact_match_count,
    GROUP_CONCAT(c.customer_id ORDER BY c.customer_id) AS matching_customer_ids
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
GROUP BY ca.id, ca.username, ca.email
HAVING COUNT(c.customer_id) <> 1
ORDER BY ca.id;

-- Source accounts which collide onto one central customer (one login per profile).
SELECT c.customer_id, COUNT(ca.id) AS source_account_count,
       GROUP_CONCAT(ca.id ORDER BY ca.id) AS legacy_account_ids
FROM customer_accounts ca
JOIN address account_address ON account_address.address_id = ca.address_id
JOIN customer c
  ON BINARY c.customer_name = BINARY ca.name
 AND EXISTS (
     SELECT 1
     FROM address profile_address
     WHERE profile_address.address_id = c.fk_address_id
       AND TRIM(account_address.street_number) <> ''
       AND TRIM(account_address.postal_code) <> ''
       AND TRIM(account_address.city) <> ''
       AND TRIM(account_address.province) <> ''
       AND BINARY account_address.street_number = BINARY profile_address.street_number
       AND BINARY account_address.postal_code = BINARY profile_address.postal_code
       AND BINARY account_address.city = BINARY profile_address.city
       AND BINARY account_address.province = BINARY profile_address.province
 )
GROUP BY c.customer_id
HAVING COUNT(ca.id) > 1;

-- Customer profiles with no type or with conflicting Person/Company rows.
SELECT c.customer_id, c.customer_name,
       (p.customer_id IS NOT NULL) AS has_person,
       (co.customer_id IS NOT NULL) AS has_company
FROM customer c
LEFT JOIN person p ON p.customer_id = c.customer_id
LEFT JOIN company co ON co.customer_id = c.customer_id
WHERE (p.customer_id IS NULL AND co.customer_id IS NULL)
   OR (p.customer_id IS NOT NULL AND co.customer_id IS NOT NULL)
ORDER BY c.customer_id;

-- A one-to-one customer/address relationship requires each address to be
-- referenced by at most one customer.
SELECT fk_address_id, COUNT(*) AS customer_count,
       GROUP_CONCAT(customer_id ORDER BY customer_id) AS customer_ids
FROM customer
WHERE fk_address_id IS NOT NULL
GROUP BY fk_address_id
HAVING COUNT(*) > 1
ORDER BY fk_address_id;

-- Duplicate usernames and case-insensitive email collisions in source logins.
SELECT LOWER(TRIM(username)) AS normalized_username, COUNT(*) AS duplicate_count
FROM customer_accounts
GROUP BY LOWER(TRIM(username))
HAVING COUNT(*) > 1;

SELECT LOWER(TRIM(email)) AS normalized_email, COUNT(*) AS duplicate_count
FROM customer_accounts
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;

-- Bank-account references that do not point to an existing customer.
SELECT a.account_id, a.fk_cust_id
FROM `account` a
LEFT JOIN customer c ON c.customer_id = a.fk_cust_id
WHERE a.fk_cust_id IS NOT NULL AND c.customer_id IS NULL
ORDER BY a.account_id;

-- Legacy profiles missing a complete address cannot support safe login matching.
SELECT c.customer_id, c.customer_name, c.fk_address_id
FROM customer c
LEFT JOIN address a ON a.address_id = c.fk_address_id
WHERE a.address_id IS NULL
   OR TRIM(COALESCE(a.street_number, '')) = ''
   OR TRIM(COALESCE(a.postal_code, '')) = ''
   OR TRIM(COALESCE(a.city, '')) = ''
   OR TRIM(COALESCE(a.province, '')) = ''
ORDER BY c.customer_id;
