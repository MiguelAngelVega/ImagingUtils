# Changelog

All notable changes, enhancements, and the reasoning behind them are recorded
here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/);
dates are ISO‑8601 (`YYYY-MM-DD`).

## [Unreleased] — 2026-09-10

### Performance — thumbnail loading (wide-window lag)

**Problem.** On wide/maximized windows the thumbnail grid became sluggish and
heavy on system resources. `LazyVerticalGrid` only composes *visible* cells, so
the cost was per‑cell work multiplied by how many cells are visible at once:

- Unbounded column count (`GridCells.Adaptive(160.dp)`) → a wide window shows
  many columns × rows simultaneously.
- Each visible cell launched its own decode on `Dispatchers.IO` (up to 64
  threads) → dozens of concurrent full‑resolution decodes saturating CPU/IO/RAM.
- No cache and no stable list keys → sorting, filtering, or scrolling re‑decoded
  images from disk every time.

**Change (Step 1 of the performance plan).**

- **Added** `ThumbnailCache.kt` — a shared loader that:
  - decodes on a bounded dispatcher, `Dispatchers.IO.limitedParallelism(n)` with
    `n = CPU count` (clamped 2–8), so a wider window queues work instead of
    saturating the machine;
  - keeps results in an access‑ordered LRU (`LinkedHashMap`, max 256 entries,
    ≈67 MB at 256 px) keyed by **path + size + last‑modified + target size**, so
    scroll‑back / re‑sort / re‑filter are instant, and files edited on disk are
    re‑decoded rather than served stale.
- **Changed** `ThumbnailGrid.kt`:
  - grid items now use a stable key, `items(entries, key = { it.file.path })`,
    so re‑ordering/filtering reuses cells instead of re‑decoding;
  - cells load via `ThumbnailCache.get(entry)` instead of an ad‑hoc
    `withContext(Dispatchers.IO) { loadThumbnail(entry) }`;
  - removed the now‑unused `Dispatchers` / `withContext` imports.

**Decisions & rationale.**

- *Bound concurrency to CPU count.* Decoding is CPU‑bound (scaling), so allowing
  the full 64‑thread IO pool was the direct cause of the wide‑window spikes.
  A limited view over `Dispatchers.IO` fixes it without a custom thread pool.
- *In‑memory LRU over an on‑disk cache (for now).* Simplest change with the
  biggest win; an on‑disk cache can come later if cold‑start matters.
- *Key by size + mtime.* Cheap correctness guard so edited files invalidate.
- *Kept the existing flat package.* The larger architecture refactor
  (layered + state holder) is deferred; this change is intentionally contained
  to `Thumbnails`/`ThumbnailGrid` to isolate the fix.

### UI / UX enhancements (same session)

- **Theme‑aware scrollbars.** `AppTheme` now provides a `LocalScrollbarStyle`
  derived from `onSurface`, fixing scrollbars that were invisible in dark mode
  (Compose Desktop's default is black‑based). Applies app‑wide.
- **Metadata panel scrollbar.** Added a `VerticalScrollbar` to `MetadataPanel`,
  which previously scrolled with no visible indicator.
- **Persistent header filter.** The metadata "Filter headers" text now survives
  switching between images (state hoisted above the null check and no longer
  keyed on the selection) instead of resetting on each click.
- **Component showcase.** Added a standalone gallery app (`Showcase.kt`,
  `ShowcaseComponents.kt`) previewing every Material color role (with hex), the
  type scale, and the app's reusable components under a live light/dark toggle.
  Run with `mvn exec:java -Dmain.class=com.imagingutils.ShowcaseKt`.

## Planned / roadmap

Remaining steps of the thumbnail‑performance plan, in priority order:

1. **Downscaled decoding.** Replace full‑res `ImageIO.read` + `getScaledInstance`
   with `ImageReader` + `setSourceSubsampling`, and prefer embedded EXIF/RAW JPEG
   thumbnails where present; subsample FITS during read. (Cuts CPU + memory.)
2. **Thumbnail size / density control** (small · medium · large) plus a max‑column
   cap, so wide windows yield larger thumbnails rather than more of them.
3. **Load‑on‑settle** (debounced) decoding for smoother fast‑scroll, if still
   needed after steps 1–2.

### Other candidates (not yet scheduled)

- Layered architecture + `LibraryState` state holder (see prior discussion).
- Unit tests for `sortEntries`, `readMetadata`/`friendlyType`, and the cache key.
- RAW/XISF real previews (embedded JPEG extraction) to replace placeholders.
