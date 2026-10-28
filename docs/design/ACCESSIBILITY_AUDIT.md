# Accessibility (a11y) Audit & Conformance Baseline

This document tracks continuous compliance with **WCAG 2.2 Level AA** across the public portal and principal administration modules.

## Scoped Viewports & Modalities
- **Viewports evaluated:** 320px, 360px, 390px, 768px, 1024px, 1280px+
- **Input modalities:** Touch (min 44px hitbox), Mouse, Keyboard (tab order, focus ring, Escape dismiss)
- **Assistive tech:** Screen readers (ARIA landmarks, `aria-live` announcements, descriptive image alt text)

## Completed Remediations

### 1. Form Labels & Error Association
- All text, email, password, and search inputs link `<label htmlFor>` to `<input id>`.
- Error messages use `role="alert"` and are connected via `aria-describedby` and `aria-invalid`.
- Dynamic status banners use `role="status"` and `aria-live="polite"` so screen readers perceive async mutations.

### 2. Keyboard Navigation & Focus Trapping
- Top navigation includes accessible `SkipToContent` component linking to `#main-content`.
- Modal dialogs (`ConfirmModal`, `MobileNav`, `MobileDrawer`, gallery lightbox) enforce focus on open, trap focus within the boundary, close on `Escape`, and release background body scrolling upon unmount.
- Brand focus indicator (`focus-visible:ring-2 focus-visible:ring-brand-900`) is visible and consistent across all buttons, inputs, links, and cards.

### 3. Touch Targets & Responsive Layout
- Minimum interactive target size is >= 44x44 CSS pixels on all mobile navigation triggers, drawer close controls, and gallery next/prev actions.
- Touch interactions prevent pinch-zoom layout distortion while supporting text scaling up to 200%.

### 4. Tenant Media Alt Text
- Dynamic school logo elements compute localized descriptive alt text referencing the current school tenant (`${school.name} લોગો`).

## Continuous Open Areas
- Screen reader pronunciation verification across legacy mobile browsers for compound Gujarati conjunctions.
- Automated axe-core regression checks in CI pipeline.
