---
name: feedback-playwright-file-upload-restriction
description: Playwright MCP file upload is restricted to worktree root — test fixtures must be inside the worktree
metadata:
  type: feedback
---

`browser_file_upload` in Playwright MCP only allows files within allowed roots:
- `<worktree>/.playwright-mcp`
- `<worktree>/`

Test fixture images (PNG, GIF, etc.) must be placed inside `app/e2e/fixtures/` or another path under the worktree root. Files in `C:\Temp\` or other system paths are rejected with "File access denied".

**Why:** Playwright MCP sandbox policy.

**How to apply:** Always put E2E test fixture assets in `app/e2e/fixtures/`. Reference them via `path.join(__dirname, '..', 'fixtures', 'test-image.png')`.

The hidden file input (`input[type="file"]`) must be made visible before `browser_file_upload` can work:
```js
await page.evaluate(() => {
  const input = document.querySelector('input[type="file"]');
  if (input) { input.style.display = 'block'; input.style.position = 'static'; }
});
const [chooser] = await Promise.all([
  page.waitForEvent('filechooser'),
  page.locator('input[type="file"]').click()
]);
await chooser.setFiles(path);
```
