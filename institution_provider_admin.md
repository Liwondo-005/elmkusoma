# ELMKUSOMA — INSTITUTION / PROVIDER ADMIN
## Master Reference & Architecture Specification

**Document Type:** Authoritative Product, Architecture & Implementation Reference  
**Scope:** Institution / Provider Administration  
**Status:** Production Architecture Reference  
**Product:** ELMKUSOMA  
**Primary Principle:** ONE ORGANIZATION ADMINISTRATION FOUNDATION → MULTIPLE CONTEXT-AWARE EXPERIENCES

---

# 1. PURPOSE

This document defines the architecture, responsibilities, boundaries, roles, permissions, organization contexts, workflows, security model, UX/UI principles, integrations, and production requirements for the **ELMKUSOMA Institution / Provider Admin** layer.

It is the organization-level control plane inside ELMKUSOMA.

It must allow an authorized organization to manage its own activities, people, learning services, content, events, live learning, resources, certificates, payments, reporting, and operations according to:

- Organization Type
- Enabled Services
- Admin Role
- Permissions
- Organization Scope
- Resource Ownership

This document is a reference for:

- Product decisions
- Software architecture
- Backend implementation
- Frontend implementation
- Database design
- Authorization design
- API design
- QA/testing
- Security auditing
- OpenCode implementation
- Future developers
- Integration with Platform Admin
- Integration with Education Authority
- Integration with Teacher/Lecturer/Instructor
- Integration with Learner
- Integration with Parent

---

# 2. CORE PRODUCT PRINCIPLE

ELMKUSOMA must NOT create a completely separate administration system for every organization type.

The preferred architecture is:

```text
Organization
    +
Organization Type
    +
Enabled Services
    +
Admin Role
    +
Permissions
    +
Scope
    ↓
Adaptive Institution / Provider Admin Workspace
