# Product Requirements Document — Rorfost School Platform

## Purpose & Problem

Rorfost School Platform provides primary and secondary schools with a professional, mobile-friendly Gujarati public website and a focused principal administration panel. It replaces manual or fragmented sharing of notices, photos, circulars, and marks with a secure, unified platform.

- **Platform Product:** Rorfost School Platform (shared multi-tenant architecture)
- **Reference School Deployment:** Dhadhana Primary School Portal

## Users & Primary Personas

1. **School Principal / Administrator:** Manages school profile, branding, academic setup (standards, subjects), notices, study materials, photo gallery, and examination results.
2. **Students (Standard 1–8):** Use a phone-first Gujarati interface to access study materials, circulars, downloads, and their individual exam marksheets.
3. **Parents & Community Members:** View school achievements, staff details, notices, photo albums, and official contact information.
4. **Platform Operator:** Provisions new schools, rotates gateway tokens, configures feature flags, and monitors system health without writing custom code per school.

## Modules & Core Capabilities

- **Public School Portal (Gujarati-first):** Home, About School, Principal's Desk, Notice Board, Photo Gallery, Student Corner, School Calculators, and Contact.
- **Principal Admin Panel (English):** Secure session login, operational dashboard, school settings, academic configuration, Results & Marks management, content publishing, and security audit logs.
- **Multi-Tenant Routing (Target Architecture):** Independent Cloudflare Worker origins per school connecting to a unified backend and shared database, protected by cryptographic gateway tokens.
- **Feature Flags:** Per-school enablement of platform features (e.g., annual vs. ekam kasoti results, gallery, materials).

## Scope Boundaries

- **Included:** School information, academic configuration, Excel result import, printable marksheets, notices, gallery albums, downloads, and multi-tenant isolation.
- **Excluded:** Complex ERP features (payroll, fee payment gateways, bus tracking, student biometric attendance), external chat systems, and student account creation.

## Privacy Principles

The platform collects strictly the minimum necessary data:
- No government identifiers (such as Aadhaar UID) are ever requested, processed, or stored.
- Results are accessible only individually by standard and roll number with `Cache-Control: no-store`.
- Class-wide marks are never exposed to public viewers.
