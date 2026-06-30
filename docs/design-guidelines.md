# Design System — Narodowy Bank Polski (NBP)

Extracted from [https://nbp.pl/](https://nbp.pl/) on 2026-06-24.
Use these tokens and assets to build UIs consistent with the NBP brand.

> Source of truth: [`assets/design-tokens.json`](../assets/design-tokens.json). All user-facing text must be in **Polish**.

---

## Assets

| Asset | Path | Notes |
|---|---|---|
| Homepage screenshot | [`assets/homepage.png`](../assets/homepage.png) | Reference for layout & visual tone |
| Logo (wordmark + mark) | [`assets/logo.svg`](../assets/logo.svg) | 205×64 SVG, NBP eagle mark + "NBP" |
| Favicon | [`assets/favicon.ico`](../assets/favicon.ico) | 16×16 ICO |
| Design tokens | [`assets/design-tokens.json`](../assets/design-tokens.json) | Machine-readable tokens |
| Brand fonts | [`assets/fonts/`](../assets/fonts/) | Self-hosted TTFs + [`fonts.css`](../assets/fonts/fonts.css) |

---

## Colors

The palette is anchored by a deep, institutional **navy** (the NBP brand color) paired with a muted **steel blue** for interactive elements and a warm **gold/sand** accent.

| Token | Hex | Usage |
|---|---|---|
| `brand.primary` | `#152E52` | Navy — header background, headings, primary brand surfaces |
| `brand.accent` | `#4A74B0` | Steel blue — links, primary buttons, interactive accents |
| `brand.gold` | `#E8D499` | Warm sand — highlights, decorative accents, badges |
| `background.default` | `#FFFFFF` | Page background |
| `background.light` | `#F7F7F7` | Section / card backgrounds |
| `background.dark` | `#152E52` | Dark sections, footer, header |
| `border.default` | `#C4C4C4` | Dividers, default borders |
| `border.subtle` | `#BFCEDD` | Input borders, subtle separators |
| `text.primary` | `#464646` | Body copy |
| `text.secondary` | `#333333` | Stronger body text |
| `text.heading` | `#152E52` | Headings (navy) |
| `text.link` | `#4A74B0` | Hyperlinks |
| `text.onDark` | `#FFFFFF` | Text on navy/dark backgrounds |

`error` (`#C0392B`) and `success` (`#2E7D32`) are sensible defaults for status states — the homepage did not expose explicit semantic colors.

---

## Typography

Two self-hosted typefaces (both available on Google Fonts):

- **Headings — "Brygada 1918"** (serif). A contemporary serif used for `h1`/`h2` and titles. Default weight **500 (medium)**. Lends an authoritative, institutional tone.
- **Body — "Libre Franklin"** (sans-serif). Used for all body text, navigation, buttons, and `h3`.

### @font-face (self-hosted)
Both families ship in [`assets/fonts/`](../assets/fonts/) as TTF files with a ready-to-use
[`fonts.css`](../assets/fonts/fonts.css) (all `@font-face` rules, `font-display: swap`). Both are licensed under the **SIL Open Font License 1.1**.

- **Brygada 1918** — `assets/fonts/brygada-1918/` — weights 400, 500, 600, 700 (+ italics) = 8 files
- **Libre Franklin** — `assets/fonts/libre-franklin/` — weights 100–900 (+ italics) = 18 files

Import once, then reference by family name:
```css
@import url("./assets/fonts/fonts.css");

font-family: "Brygada 1918", Georgia, "Times New Roman", serif;   /* headings */
font-family: "Libre Franklin", -apple-system, Arial, sans-serif;  /* body */
```

### Weight scale
| Name | Value |
|---|---|
| regular | 400 |
| medium | 500 |
| semibold | 600 |
| bold | 700 |

### Size scale
| Token | Size | Usage |
|---|---|---|
| `sm` | 13px | Buttons, captions, fine print |
| `base` | 15.5px | Body copy (line-height ~1.55) |
| `md` | 16.5px | Navigation links |
| `lg` | 20px | Sub-headings |
| `xl` | 24px | `h1` / `h2` (line-height 1.33) |
| `2xl` | 32px | Hero / page titles |

---

## Spacing

Base unit is **4px**. Common increments: `4, 8, 12, 16, 20, 24, 28, 32` px. Sections use generous vertical rhythm (nav block padding ≈ 85px top).

---

## Border radius

| Token | Value | Usage |
|---|---|---|
| `xs` | 2px | Small chips |
| `sm` | 4px | Buttons (primary) |
| `md` | 6px | Inputs (search field) |
| `lg` | 12px | Cards |
| `circle` | 50% | Avatars, icon buttons |
| `full` | 999px | Pills |

The brand is restrained — radii are small (2–6px). Avoid heavily rounded corners.

---

## Components

### Header
- Background: navy `#152E52`, full-bleed. Logo (`logo.svg`) sits top-left.
- Text/icons on header are white (`text.onDark`).

### Navigation
- Color: navy `#152E52`, font-size 16.5px, weight **500**, **UPPERCASE** for top-level items.
- Active/current item: weight **700**.
- Uses Libre Franklin.

### Primary button
```css
background: #4A74B0;
color: #FFFFFF;
border: 1px solid #4A74B0;
padding: 6px 12px;
border-radius: 4px;
font-size: 13px;
font-weight: 500;
```
Example label on site: "Więcej".

### Input (search)
```css
background: #FFFFFF;
border: 1px solid #BFCEDD;
border-radius: 6px;
padding: 10px;
```

### Headings
Navy `#152E52`, "Brygada 1918" serif, weight 500.

---

## Logo usage

- Use [`assets/logo.svg`](../assets/logo.svg) (205×64) — the NBP eagle mark with the "NBP" wordmark.
- On the navy header the logo renders in white/light form; on light backgrounds use the default (navy) version.
- Preserve clear space around the mark and never recolor or distort it.

---

## Visual style summary

NBP's brand reads as **institutional, authoritative, and trustworthy** — exactly what one expects from a central bank. A deep navy dominates, softened by a single steel-blue interactive accent and an occasional warm gold highlight. The pairing of a refined serif (Brygada 1918) for headings with a clean grotesque sans (Libre Franklin) for body copy creates a formal, editorial hierarchy. Generous whitespace, restrained corner radii, and a near-monochrome palette keep the experience calm, legible, and credible.
