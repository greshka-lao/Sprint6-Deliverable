-- Run in the same isolated MySQL connection after customer-consolidation-legacy.sql.
-- Expected match counts: unique 1, ambiguous 2, unmatched 0, company unique 1.
-- MySQL cannot reopen one temporary table under multiple aliases, so use a
-- separate temporary copy for profile-address matching.
DROP TEMPORARY TABLE IF EXISTS fixture_legacy_profile_address;
CREATE TEMPORARY TABLE fixture_legacy_profile_address AS
SELECT * FROM fixture_legacy_address;

SELECT
    ca.id AS legacy_account_id,
    COUNT(profile_address.address_id) AS exact_match_count,
    CASE
        WHEN ca.id IN (1001, 4001) AND COUNT(profile_address.address_id) = 1 THEN 'PASS'
        WHEN ca.id = 2001 AND COUNT(profile_address.address_id) = 2 THEN 'PASS'
        WHEN ca.id = 3001 AND COUNT(profile_address.address_id) = 0 THEN 'PASS'
        ELSE 'FAIL'
    END AS fixture_result
FROM fixture_legacy_customer_accounts ca
LEFT JOIN fixture_legacy_address account_address
    ON account_address.address_id = ca.address_id
LEFT JOIN fixture_legacy_customer c
    ON BINARY c.customer_name = BINARY ca.name
LEFT JOIN fixture_legacy_profile_address profile_address
    ON profile_address.address_id = c.fk_address_id
   AND TRIM(account_address.street_number) <> ''
   AND TRIM(account_address.postal_code) <> ''
   AND TRIM(account_address.city) <> ''
   AND TRIM(account_address.province) <> ''
   AND BINARY account_address.street_number = BINARY profile_address.street_number
   AND BINARY account_address.postal_code = BINARY profile_address.postal_code
   AND BINARY account_address.city = BINARY profile_address.city
   AND BINARY account_address.province = BINARY profile_address.province
GROUP BY ca.id
ORDER BY ca.id;

-- Only 9004 should be reported as an invalid account-to-customer reference.
SELECT bank.account_id, bank.fk_cust_id
FROM fixture_legacy_bank_account bank
LEFT JOIN fixture_legacy_customer customer ON customer.customer_id = bank.fk_cust_id
WHERE customer.customer_id IS NULL
ORDER BY bank.account_id;
