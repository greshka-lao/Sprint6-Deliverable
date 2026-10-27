# Implementation Plan: Admin Login

**Branch**: `001-admin-login` | **Date**: 2026-10-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [spec.md](./spec.md)

## Summary

This feature introduces a secure admin sign-in and role-based authorization foundation for the bank application. The work will protect the existing customer and account management features behind a valid admin session and support both bank teller and bank manager roles, while also establishing a scalable pattern for future customer-facing roles. Because the platform does not yet have a production identity system, the release will include a database-backed test admin path for login validation and feature testing.

## Technical Context

**Language/Version**: Java 17, Spring Boot 4.1.1, React 19.2.8, Vite 8.3.0, Node.js 20+

**Primary Dependencies**: Spring Boot JPA/WebMVC, MySQL Connector/J, Springdoc OpenAPI, React Router DOM, Vitest, Testing Library, ESLint

**Storage**: MySQL persistence through Spring Data JPA, with new identity, role, session, and access-audit tables

**Testing**: Maven for backend tests, Vitest for frontend tests, plus targeted integration tests for auth and route protection

**Target Platform**: Browser-based web application for internal administrative staff

**Project Type**: Web application with separate frontend and backend services

**Performance Goals**: Successful login and protected route validation should complete within normal business-use latency expectations; the majority of sign-ins should complete in under 5 seconds under normal conditions

**Constraints**: Use existing project libraries and patterns only; no external identity provider in this release; account permissions must be role-based and extendable; temporary login route must remain clearly test-only

**Scale/Scope**: Current scope covers bank teller and bank manager access to existing customer/account management workflows; future role expansion should fit into the same framework without redesigning the login foundation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project has no active constitution rules defined in the repository, so there are no explicit governance conflicts to block this feature. The design remains consistent with the existing repository structure, uses the current React frontend and Spring Boot backend layout, and keeps the feature focused on the auth/authz foundation needed for the existing admin workflows.

## Project Structure

### Documentation (this feature)

```text
specs/001-admin-login/
├── plan.md              # This file
├── research.md          # Phase 0 design notes
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Quick validation guide
├── contracts/           # API documentation
├── spec.md              # Approved feature specification
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/fdmgroup/sprint4Deliverable/
│   ├── Config/
│   ├── Controller/
│   ├── Exception/
│   ├── Model/
│   ├── Repository/
│   ├── Service/
│   ├── Util/
│   └── Sprint4DeliverableApplication.java
└── src/test/java/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   ├── App.jsx
│   ├── App.css
│   └── main.jsx
├── public/
├── index.html
├── package.json
└── vite.config.js
```

**Structure Decision**: Keep the feature within the current split architecture: backend auth services and controllers in [backend/](../../backend), frontend login and protected-route UI in [frontend/](../../frontend), and supporting design artifacts under [specs/001-admin-login/](.).

## Complexity Tracking

No constitution violations or architecture exceptions are required for this feature.
