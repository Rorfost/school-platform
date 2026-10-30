# Shared Design Tokens and Component System (Sprint 48)

**Document Version:** 1.0  
**Repository:** https://github.com/Rorfost/school-platform  
**Scope:** Standardization of typography, color tokens, spacing, component primitives, and accessible focus states across multi-tenant school portals.

---

## 1. Principles & Intent

1. **Gujarati-First Public UX, English Admin:** Public visitor/student/parent views use clear, accessible Gujarati typography. Admin operations, error logs, and APIs remain in English.
2. **Accessible Contrast (WCAG 2.1 AA):** Deep Navy (`#0d2461` / `brand-900`) delivers a contrast ratio exceeding 12:1 against white backgrounds, well above the 4.5:1 requirement for normal text.
3. **Motion-Reduced Accessibility:** All primitives feature `motion-reduce:transition-none` alongside the root `@media (prefers-reduced-motion: reduce)` rule, ensuring zero animated jitter for users requesting reduced motion.
4. **Predictable Free-Tier Budget:** No third-party heavy component library or runtime CSS-in-JS dependencies are introduced. The system builds entirely on Tailwind CSS v4 `@theme` tokens.

---

## 2. Design Tokens

### 2.1 Typography

- **Font Family:** `"Noto Sans Gujarati", "Gujarati Sangam MN", "Nirmala UI", "Shruti", system-ui, -apple-system, sans-serif`
- **Scale:**
    - `xs`: 12px / 0.75rem (meta badges, small labels)
    - `sm`: 14px / 0.875rem (form controls, secondary copy)
    - `base`: 16px / 1.0rem (body text, standard reading copy)
    - `lg`: 18px / 1.125rem (section subheadings, modal titles)
    - `xl`: 20px / 1.25rem (page headings)
    - `2xl`: 24px / 1.5rem (main hero/school titles)

### 2.2 Color Palette

- **Brand Palette (Deep Navy):**
    - `brand-50`: `#f0f5ff` (subtle tint, light card backgrounds)
    - `brand-100`: `#e0ebfe` (accent borders)
    - `brand-200`: `#bae0fd` (badge backgrounds)
    - `brand-600`: `#2563eb` (interactive links)
    - `brand-700`: `#1d4ed8` (link hover)
    - `brand-800`: `#1e40af` (active/focus)
    - `brand-900`: `#0d2461` (primary button, card hover border, brand emphasis)
    - `brand-950`: `#081a49` (primary button hover / active)
- **Accent (Alert / Danger):**
    - `accent-50`: `#fef2f2` / `accent-600`: `#dc2626` / `accent-700`: `#b91c1c`
- **Gold / Amber (Secondary / Trophy):**
    - `gold-50`: `#fffbeb` / `gold-600`: `#d97706` / `gold-700`: `#b45309`
- **Neutrals (Slate):**
    - `slate-50` (page canvas), `slate-100` (disabled/skeleton), `slate-200` (dividers/borders), `slate-500` (meta copy), `slate-700` (input labels), `slate-900` (primary headings).

### 2.3 Focus & Interaction Tokens

- **Focus Rings:** `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2`
- **Radius Scale:**
    - `rounded-md`: 6px (small action buttons)
    - `rounded-lg`: 8px (form inputs, selects, standard buttons)
    - `rounded-xl`: 12px (cards, modal containers, interactive cards)
    - `rounded-full`: (badges, avatar icons)
- **Shadow Scale:**
    - `shadow-xs`: subtle baseline elevation for cards and buttons
    - `shadow-sm`: active/pressed button state
    - `shadow-md`: interactive hover state for clickable cards

---

## 3. Standardized Primitives

### 3.1 Button (`frontend/src/components/ui/Button.tsx`)

- **Variants:**
    - `primary`: `bg-brand-900 text-white hover:bg-brand-950 shadow-sm`
    - `secondary`: `bg-amber-600 text-white hover:bg-amber-700 shadow-sm`
    - `outline`: `border border-slate-300 bg-white text-slate-800 hover:bg-slate-50`
    - `ghost`: `bg-transparent text-slate-700 hover:bg-slate-100`
    - `danger`: `bg-red-700 text-white hover:bg-red-800`
- **Sizes & Touch Targets:**
    - `sm`: `min-h-10 px-3 py-1.5 text-xs rounded-md` (40px minimum touch target)
    - `md`: `min-h-11 px-4 py-2 text-sm rounded-lg` (44px touch target)
    - `lg`: `min-h-12 px-6 py-3 text-base rounded-lg` (48px touch target)
- **Accessibility:** Includes `aria-busy` when `loading={true}`, preserves disabled pointer events, and provides `motion-reduce:transition-none`.

### 3.2 Card (`frontend/src/components/ui/Card.tsx`)

- **Variants:**
    - `default`: Clean white card with slate-200 border.
    - `interactive`: Hover-lift with elevation (`hover:shadow-md`) and brand border transition (`hover:border-brand-900`).
    - `accent`: Light tinted background (`bg-brand-50/50`) with subtle brand border.

### 3.3 Input (`frontend/src/components/ui/Input.tsx`)

- **Touch Target:** `min-h-11` (44px touch-accessible height).
- **Focus Indicator:** 2px high-contrast `ring-brand-900`.
- **States:** Accessible `aria-invalid`, error message bound via `aria-describedby`, placeholder contrast compliance.

---

## 4. Multi-Tenant Brand Safety

All colors, tokens, and primitives are tenant-neutral tokens defined globally. School identity is driven solely through:

1. School name and address headers.
2. Verified school logo and official seal URLs.
3. Feature flags (toggling modules on/off per tenant).

No tenant can inject arbitrary unvetted CSS or break high-contrast WCAG 2.1 AA ratios.
