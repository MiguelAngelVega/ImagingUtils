# Changelog

All notable changes, enhancements, and the reasoning behind them are recorded
here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/);
dates are ISO‑8601 (`YYYY-MM-DD`).

## [Unreleased] — 2026-09-11

### Refactor — hybrid package layout

**Change.** The flat `com.imagingutils` package (21 source files) was reorganized
into a hybrid structure that separates cross-cutting concerns from panel UI:

- `designsystem/` — panel-agnostic UI tokens and components (`Buttons`, `Color`,
  `Dimens`, `Theme`).
- `data/` — domain/logic with no Compose dependency (`Model`, `Metadata`,
  `Settings`, `Thumbnails`, `ThumbnailCache`) plus `data/processing/Autostretch`.
- `ui/` — coarse feature packages: `chrome` (`TopBar`, `Toolbar`), `thumbnails`
  (`SortBar`, `FilterBar`, `ThumbnailGrid`), `preview` (`PreviewPane`,
  `AutostretchDialog`), `metadata` (`MetadataPanel`), `showcase`.
- `Main.kt` stays at the root as the app entry + `App()` orchestration.

**Why.** Roughly half the files are not panel-specific (`CompactButton`,
`AppDimens`, `ImageEntry`, `loadThumbnail`, …), so a pure by-panel split would
have forced shared code into an arbitrary panel. The hybrid keeps related panel
UI together while giving shared code honest homes and a clean dependency
direction: `ui.*` → `designsystem` + `data`; panels don't depend on each other
(only `Main` wires them together).

**Notes.** `Main.kt`'s main class is unchanged (`com.imagingutils.MainKt`), so the
run command and `pom.xml` are untouched. The showcase moved to `ui.showcase`, so
it now runs with `mvn exec:java -Dmain.class=com.imagingutils.ui.showcase.ShowcaseKt`.
Compilation and both apps (main + showcase) verified after the move.

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
- **Inline preview pane.** Replaced the thumbnail right‑click context menu with a
  permanent `PreviewPane` (`PreviewPane.kt`) beneath the metadata panel. It shows
  the selected image at a larger size (≈1024 px) with an **Autostretch** on/off
  toggle that re‑decodes the image in place, and an **Enlarge** button that opens
  the existing full‑window pan/zoom `AutostretchDialog`.
  - **Changed** `ThumbnailGrid.kt`: dropped `ContextMenuArea`/`ContextMenuItem`
    and the `onAutostretch` parameter from `ThumbnailGrid`/`ThumbnailCell`.
  - **Changed** `Main.kt`: right column now stacks `MetadataPanel` (top) and
    `PreviewPane` (bottom) split by a `HorizontalDivider`; added
    `previewAutostretch` state; wired **Enlarge** to `autostretchTarget`.
  - *Rationale.* A hidden right‑click action is undiscoverable; a persistent pane
    with explicit buttons surfaces the same capabilities and keeps the heavy
    pan/zoom view one click away rather than always open.
- **Resizable three‑pane layout.** Reorganised the main workspace into three
  side‑by‑side columns: **thumbnails** (left), **preview** (center), and
  **metadata** (right). The preview pane moved out from under the metadata panel
  into the center, taking the remaining (`weight(1f)`) space.
  - **Added** `DraggableVerticalDivider` (in `Main.kt`): a `VerticalDivider` with
    a widened, draggable hit area and an E–W resize cursor. Dragging resizes the
    thumbnails panel; `BoxWithConstraints` clamps its width between
    `thumbnailsPanelMinWidth` and `maxWidth − metadataPanelWidth −
    previewPaneMinWidth`, so the preview never collapses below a usable size.
  - **Added** dimension tokens `thumbnailsPanelDefaultWidth` (480 dp),
    `thumbnailsPanelMinWidth` (220 dp), `previewPaneMinWidth` (280 dp).
  - **Changed** `Main.kt`: added `thumbnailsWidth` state; the left panel now has an
    explicit (draggable) width instead of `weight(1f)`; metadata is a fixed‑width
    right column with a static divider.
  - *Rationale.* A dependency‑free manual divider (using `draggable` +
    `pointerHoverIcon`) avoids pulling in a split‑pane library, matches the
    existing `VerticalDivider` styling, and gives users direct control over how
    much space the browsing grid vs. the preview gets.
- **Sort controls moved into the thumbnails panel.** The Sort key (Name/Date) and
  direction toggle were extracted from the full‑width `TopBar` into a new
  `SortBar` (`SortBar.kt`) that sits at the top of the left thumbnails column,
  above the `FilterBar`.
  - **Added** `SortBar.kt` (with the `SortOption` helper moved over from `TopBar`).
  - **Changed** `TopBar.kt`: dropped the `sortKey`/`ascending`/`onSortKey`/
    `onToggleDirection` params, the Sort UI, and the now‑unused `FontWeight`
    import; it now holds only Open Folder, the folder path, and the theme toggle.
  - **Changed** `Main.kt`: `TopBar` call trimmed; `SortBar` added atop the left
    column (with a `HorizontalDivider` before the `FilterBar`).
  - **Changed** `ShowcaseComponents.kt`: `TopBarDemo` updated to the new signature
    and a `SortBar` demo added to the gallery.
  - *Rationale.* Sorting only affects the thumbnail grid, so placing it directly
    above that grid scopes it visually to what it reorders instead of spanning the
    whole window across the unrelated preview/metadata panes.
- **Zoom / pan in the inline preview.** `PreviewPane` now supports the same
  navigation as the full `AutostretchDialog`, so users rarely need to enlarge:
  - **mouse‑scroll zoom** (0.1×–12×, ×1.1 per notch) over the preview image;
  - **drag‑to‑pan** when the pan toggle is on;
  - header controls: a live zoom **percentage**, **Zoom out** / **Zoom in**
    (×1.25 steps), **Original size** (`FitScreen`, resets scale to 1× and offset),
    and a **Pan** toggle (`PanTool`).
  - Implemented with per‑image `scale`/`offset`/`panEnabled` state (`remember(entry)`
    so switching selection resets the view), a `graphicsLayer` on the `Image`, and
    `clipToBounds` so panned content stays inside the pane. Reuses the shared
    `AppDimens.iconButtonSize`/`iconSize` tokens for consistent control sizing.

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
