# Implementation Plan: Customer Account Consolidation

**Branch**: `004-customer-account-management` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [spec.md](./spec.md)

## Summary

Consolidate login identity and operational customer data into the `customer` table while retaining the existing joined `Person`/`Company` inheritance and bank-account relationships. Extend the existing customer authentication and admin workflows to use this central record. Customer deletion clears credentials and marks the central row `DELETED` so its joined subtype row remains intact. Use a staged, operator-controlled MySQL migration with exact unique matching and manual resolution for unmatched data. Customer registration selects a type; an exact match to an unclaimed administrator-created profile requires administrator approval before the customer can set credentials.

## Technical Context

**Language/Version**: Java 17; React 19.2.8 and JavaScript ES modules

**Primary Dependencies**: Spring Boot 4.1.1, Spring Data JPA, Spring Web MVC, Spring Security, MySQL Connector/J, React 19, React Router DOM, Vitest, Testing Library

**Storage**: Existing MySQL `sprint4` schema. The final customer identity/profile table is `customer`; existing `person`, `company`, `address`, and bank-account relationships are retained. The separate `customer_accounts` table is a migration source and is retired only after verification.

**Testing**: Backend Maven tests using existing Spring/JPA test dependencies; frontend Vitest and Testing Library tests; manual MySQL migration and end-to-end validation as described in [quickstart.md](./quickstart.md). Customer auth currently uses Fetch; existing admin customer pages import Axios, which is not listed as a direct frontend dependency.

**Target Platform**: Existing browser frontend and Spring Boot web service

**Project Type**: Web application with separate `frontend/` and `backend/` projects

**Performance Goals**: No feature-specific latency or throughput target is specified. Preserve the current customer sign-in and admin-list experience under normal project usage; validate correctness and pagination-free behavior against representative project data.

**Constraints**:
- Retain the existing `person` and `company` associations and the bank `Account.customer` relationship.
- Keep customer authentication separate from administrator authentication; only an approved claim may attach credentials to an existing unclaimed profile.
- Match legacy account/profile data automatically only for exact, unique name-and-complete-address pairs. Preserve and report all unmatched or ambiguous source rows; resolve them before cutover so no existing login is lost.
- Customer type is chosen at creation and is immutable afterward. Username and password remain customer-managed; admin-created profiles may await a claim.
- Keep the existing Java, React, Maven, and npm test tools. Use an explicit deployment-run SQL data migration rather than relying on Hibernate schema update to merge populated tables.
- Existing production schema validation remains authoritative; change development schema generation to validation after the migration is in place.

**Scale/Scope**: One existing MySQL application schema; customer registration and sign-in; claim review/approval; admin customer CRUD and active-customer listing; safe conversion of existing customer data. No external email service, password recovery, banking-account workflows, or customer self-service profile editing is introduced.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The checked-in constitution contains only unfilled placeholders and no adopted principles or enforceable gates. No active constitution rule conflicts with the feature.

**Pre-research gate**: PASS — no active constitution constraints. Existing framework and test tooling are retained.

**Post-design gate**: PASS — the design uses the existing frontend/backend structure, preserves role boundaries, and makes data migration explicit and verifiable. The separate claim-request record is required to support administrator approval without granting premature login access.

## Project Structure

### Documentation (this feature)

```text
specs/004-customer-account-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── customer-management-api.md
├── spec.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/fdmgroup/sprint4Deliverable/
├── Config/
│   ├── AdminSecurityConfig.java
│   └── CustomerSecurityConfig.java
├── Controller/
│   ├── CustomerAuthController.java
│   ├── CustomerClaimAdminController.java
│   └── CustomerController.java
├── Model/
│   ├── Customer.java
│   ├── CustomerClaimRequest.java
│   ├── Person.java
│   └── Company.java
├── Repository/
│   ├── CustomerRepository.java
│   └── CustomerClaimRequestRepository.java
└── Service/
    ├── CustomerAuthService.java
    ├── CustomerClaimService.java
    └── CustomerService.java

backend/src/main/resources/
├── application*.properties
└── db/manual/
    ├── customer-consolidation-preflight.sql
    └── customer-consolidation.sql

backend/src/test/java/com/fdmgroup/sprint4Deliverable/
├── Controller/
├── Model/
├── Repository/
└── Service/

frontend/src/
├── pages/auth/CustomerRegistration.jsx
├── pages/admin/customer/
│   ├── Customers.jsx
│   ├── AddCustomer.jsx
│   ├── UpdateCustomer.jsx
│   └── CustomerClaims.jsx
├── services/
│   ├── customerAuthService.js
│   └── customerService.js
└── __test__/
    ├── CustomerRegistration.test.jsx
    ├── CustomerClaims.test.jsx
    ├── Customers.test.jsx
    ├── AddCustomer.test.jsx
    └── UpdateCustomer.test.jsx
```

**Structure Decision**: Continue using the current Spring Boot backend and React frontend. `Customer` remains the central JPA entity using `JOINED` inheritance; `Person` and `Company` are its subtypes, and both subtype tables retain `customer_id` as the shared primary/foreign key. Login fields live on the central customer table, while `customer_type` is derived from and validated against the subtype. Customer deletion clears credentials and marks the central row `DELETED`, preserving the joined subtype row and unassigning bank accounts. Customer bank-account associations continue to reference the central customer identifier. The customer auth controller/service continues to own registration and login; a separate admin claim controller/service handles claim review through the existing admin authorization boundary. Admin APIs and existing frontend service conventions remain in place. The customer list is exposed at `/admin/customer`; retain the existing plural route as a redirect if currently linked or bookmarked.

## Complexity Tracking

No constitution violations. The claim-request entity and decoupled profiles are the minimum data-model changes needed for administrator approval and profile preservation on customer deletion.
