---
name: angular21-scaffold
description: Angular CLI v21 scaffold differences — Vitest default, Karma setup, @angular/build:karma config, SCSS @use ordering
metadata:
  type: feedback
---

Angular CLI v21 (npx -p @angular/cli) scaffolds with **Vitest** as default test runner, not Jasmine/Karma. This deviates from older "CLI default = Karma" assumption.

To use Karma/Jasmine in Angular 21:
1. Install: `karma karma-chrome-launcher karma-coverage karma-jasmine karma-jasmine-html-reporter jasmine-core @types/jasmine`
2. Set `angular.json` test builder to `@angular/build:karma` (it IS available in Angular v21 — check via `node_modules/@angular/build/builders.json`)
3. `tsconfig.spec.json` — change `types` from `["vitest/globals"]` to `["jasmine"]`
4. `karma.conf.js` — do NOT `require('@angular/build/plugins/karma')` — that path is NOT exported. The builder adds its own plugins. Only include standard karma plugins: `karma-jasmine`, `karma-chrome-launcher`, `karma-jasmine-html-reporter`, `karma-coverage`.
5. frameworks: `['jasmine']` only — NOT `['jasmine', '@angular/build/karma']`

**Why:** `@angular/build/plugins/karma` subpath is not listed in `@angular/build` package.json `exports`, so Node.js throws `ERR_PACKAGE_PATH_NOT_EXPORTED`. The builder injects Angular-specific plugins automatically.

**How to apply:** Whenever scaffolding Angular projects with CLI v21+ and Jasmine/Karma is required, follow this pattern.

---

Angular Material must match Angular version:
- Angular 21 → `@angular/material@21.x`, `@angular/cdk@21.x`
- Use `npm show @angular/material@"^21.0.0" version` to find latest patch

---

`styles.scss` SCSS ordering: `@use` rules MUST appear BEFORE any `:root {}` blocks or `@import`. Angular M3 theme pattern:
```scss
@use '@angular/material' as mat;  // FIRST
@include mat.core();
$theme: mat.define-theme((...));
html { @include mat.all-component-themes($theme); }
// THEN :root {} and @import url(...)
```
