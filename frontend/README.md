# Frontend

The frontend is a React 19, TypeScript, and Vite static single-page application for Rorfost Primary School. It provides the full public school website (in natural Gujarati), Student Corner (materials, result lookup, downloads), and the complete Principal Admin Panel (in clear English).

## Features Implemented

- **Public School Website (Gujarati)**: Home, About School, Principal's Desk, Notices & Notice Details, Photo Gallery & Album Viewer, Contact Page, and Footer/Header navigation.
- **Student Corner (Gujarati)**: Study Materials (filtered by Standard & Subject), Examination Results Lookup (with privacy PIN verification & printable marksheet view), Downloads, and Student Notices.
- **Principal Admin Panel (English)**:
  - Authentication (Login, Session Restore, Password Change with min 12-char validation, Logout, Protected `RequireAdmin` wrapper).
  - Operational Dashboard with quick links.
  - School Settings & Principal Profile management.
  - Academic Configuration: Academic Years (create/update/set current/archive), Standards, Subjects, and Standard-Subject Mappings.
  - Generic Assessments CRUD, subject max/passing mark configuration, and DRAFT/PUBLISHED/ARCHIVED lifecycle controls.
  - Content Management: Study Materials (PDF upload), School Notices & Attachments, Photo Gallery Albums & Image Upload, Downloads management.
  - Account & Security management.

## Pending Inputs

- **Result Import Excel Schema**: Excel parsing logic is intentionally blocked per `docs/data/RESULT_IMPORT.md` until the official school workbook contract is approved.
- **Timetables**: Exam timetable and school timetable features remain pending backend model inputs.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run format:check
npm run typecheck
npm run test
npm run build
```

Copy `.env.example` to `.env.local` for local values. Every `VITE_*` value is embedded in the browser bundle and is public; never put secrets there.

## Structure

- `src/app` — application providers and route configuration (`AppRouter.tsx`)
- `src/api` — shared HTTP client foundation and TypeScript DTO definitions (`types.ts`)
- `src/components` — shared layout (`Header`, `Footer`, `MobileNav`, `AdminLayout`), UI components (`Button`, `Card`, `Input`, `Badge`, `ConfirmModal`), and feedback primitives
- `src/pages` — public pages and `src/pages/admin/` principal management pages
- `src/features` — domain hooks (`auth`, `public`, `school`)
- `src/styles` — global design tokens and base styling

## Deployment

Cloudflare Workers is the intended frontend host. The checked-in Worker configuration builds the SPA into `dist`, serves assets with SPA fallback, and proxies `/api/` requests to the configured backend origin. Use `npm run deploy` only with provider credentials supplied outside the repository. No frontend Docker deployment is required.
