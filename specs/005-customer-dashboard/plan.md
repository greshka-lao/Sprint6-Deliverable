# Implementation Plan: Customer Dashboard

**Branch**: `005-customer-dashboard` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-customer-dashboard/spec.md`

## Summary

Replace the customer home placeholder with a read-only banking overview showing the signed-in customer's profile, all of their accounts, and the account total. Add a customer-session-protected dashboard read under the existing customer authentication boundary. Resolve the customer from the authenticated server-side principal, return a purpose-built response rather than exposing persistence entities, and render loading, error, empty-account, and missing-profile states in the existing customer application shell.

## Technical Context

**Language/Version**: Java 17; JavaScript with React 19

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security; React, React Router 7, Vite

**Storage**: Existing MySQL database and JPA customer/account records; no new tables or schema migration planned

**Testing**: Maven tests and Spring MockMvc integration tests; Vitest and React Testing Library

**Target Platform**: Browser-based web application with the existing Spring Boot API

**Project Type**: Full-stack web application

**Performance Goals**: Return the current customer's dashboard data within 2 seconds under normal local application and database load.

**Constraints**:
- Keep dashboard reads inside the existing `/api/customer-auth/**` security boundary and require the authenticated customer role.
- Resolve ownership from the authenticated session principal; accept no customer identifier from the caller.
- Use the existing customer session cookie and credentialed frontend requests; dashboard reads do not require a CSRF token.
- Keep account and profile fields optional where existing data allows them to be missing, and report failed reads as errors rather than displaying fabricated current data.
- Account persistence has a `double` balance and no currency column. Existing admin balance displays use `$`, `en-NZ`, and two decimal places. For this single-currency feature, use NZD as the platform display currency, retain the two-decimal display convention, and do not add currency conversion or database fields. Sum monetary values through decimal arithmetic over the existing stored values.
- Add no dependencies and preserve admin routes and behavior.

**Scale/Scope**: One authenticated customer's profile and all accounts linked to that customer; no account mutations, transactions, transfers, or multi-currency support.

## Constitution Check

The checked-in constitution file contains only unfilled template placeholders; it defines no project principles or enforceable gates. No constitution-based violations are identified. The plan follows existing project dependencies and test runners and introduces no persistence migration or new project.

**Gate: PASS** — no ratified constraints are present to violate; proceed with existing authentication boundaries and focused tests.

## Project Structure

### Documentation (this feature)

```text
specs/005-customer-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── customer-dashboard.openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/fdmgroup/sprint4Deliverable/
├── Controller/
│   └── CustomerAuthController.java        # Add authenticated dashboard read
├── Model/
│   └── CustomerDashboardResponse.java     # Add profile/account/total response DTOs
└── Service/
    └── CustomerDashboardService.java      # Resolve current customer and compute total

backend/src/test/java/com/fdmgroup/sprint4Deliverable/
└── Controller/
    └── CustomerDashboardSecurityTests.java # Session ownership, response, and auth tests

frontend/src/
├── pages/customer/
│   └── CustomerHome.jsx                   # Render the customer banking overview
├── services/
│   └── customerAuthService.js             # Add credentialed dashboard request
└── __test__/
    └── CustomerHome.test.jsx              # Profile, accounts, totals, and UI states
```

**Structure Decision**: Extend the existing React customer page and Spring Boot customer-auth API. The existing `/customer/home` route and customer session provider remain the entry point; the dashboard endpoint is placed inside the existing customer-auth security matcher rather than reusing the legacy admin-oriented `/customer` or `/account` endpoints.

## Complexity Tracking

No constitution gate violations or additional project structures require justification.
