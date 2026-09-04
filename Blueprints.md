# ImagingUtils — Blueprint

A single cross-platform desktop app to help manage Astrophotography and general
Photography files.

## 1. Project goals

- **Platform:** Cross-platform desktop — Windows + macOS + Linux.
- **v1 core focus:** View image **metadata** (EXIF for photos, FITS headers for
  astro) and **thumbnails**.
- **File types:** Both standard photo formats *and* astro formats.
  - Photo: JPEG, PNG, TIFF, RAW (CR2 / NEF / ARW / DNG).
  - Astro: FITS, XISF (later).
- **Author background:** Comfortable in Java / Kotlin.

## 2. What the reference tools are built with (verified)

| Tool | Core language | UI framework | Scripting | Notes |
|---|---|---|---|---|
| **Siril** | C (some C++) | GTK 3/4 | Python | cfitsio, OpenCV, FFTW, GSL. Strict GUI/core split. |
| **PixInsight** | C++ (PCL = ISO C++) | Qt 5 | JavaScript (PJSR / SpiderMonkey) | Native module architecture, heavy parallel numerics. |
| **GraXpert** | Python | customtkinter | Python + ONNX Runtime (AI) | The engine AstroWizard drives for gradient/denoise. |
| **Astro Pixel Processor (APP)** | Java (8 → 17) | Java UI + OpenGL viewer | — | Cross-platform installers w/ bundled JRE; macOS signed & notarized. |
| **AstroWizard** | Not disclosed (inferred runtime-bundled: Electron or PyInstaller/Python) | — | — | Solo dev; Win + macOS (Intel & ARM) + Linux; *orchestrator* over external CLI engines. |

**AstroWizard — confirmed from the official site (astrowizard.lukomatico.com):**

- Free, open beta; Win 10/11, macOS (Apple Silicon + Intel), Linux x86_64.
- Single-file, no-install ("it just runs"). Bundles: Win ≈109 MB, macOS ARM ≈64 MB,
  Linux ≈130 MB. macOS notarized (24 Aug 2026); Windows signed (2 Sep 2026).
- **Architecture = orchestrator:** drives external engines rather than
  reimplementing them — GraXpert (gradient/denoise, Python+ONNX), StarNet2 (star
  removal), RC-Astro CLI (BlurX/NoiseX/StarX). Built-in only for lighter steps
  (stretch, colour balance, curves, narrowband, star colour).
- Input formats: stacked **FITS, TIFF, XISF**. Solo dev (Luke Newbould).
- Framework/language **not disclosed**; ~100 MB self-contained bundles point to a
  runtime-bundled stack (Electron or PyInstaller/Python), not Tauri (~10–20 MB).

**Three takeaways:**

1. Heavy pixel-processing cores are **C/C++** (Siril, PixInsight) — for SIMD, GPU,
   and mature C numeric libraries.
2. A major commercial cross-platform astro app (**APP**) is **pure Java** — proof
   the JVM is fully viable here. It just doesn't advertise the stack.
3. **AstroWizard proves the orchestration pattern**: a thin cross-platform UI can
   shell out to proven CLI engines instead of rebuilding algorithms — a viable
   design for ImagingUtils (manage files + metadata/thumbnails, delegate heavy work).

## 3. "Why don't I hear about Java-based solutions?"

Mostly historical, not technical today:

- **Old Swing/AWT baggage** — non-native look, heavy, slow startup (1990s–2010s).
- **Performance-critical imaging chose C/C++** — direct memory, SIMD, GPU, cfitsio.
- **Science standardized on Python** — astropy / NumPy for scripting.
- **Distribution used to be painful** — bundling a JRE. **Solved now** via
  `jpackage` / `jlink` → native `.msi` / `.dmg` / `.deb` (exactly how APP ships).
- **Nobody markets the stack** — APP *is* Java and popular; the label is invisible.

Net: the "no Java in astro" impression is outdated. Modern JVM (Java 17+,
`jpackage`, Foreign Function & Memory API, Skia rendering) closed most old gaps.

## 4. "Is Kotlin recommended for today's UIs?"

Strong, modern, production-ready — but not the single industry default.

- **Compose Multiplatform (desktop)** is officially production-ready, JetBrains-backed,
  uses Skia hardware-accelerated rendering, ships native installers via `jpackage`,
  and is in production at Wrike and PhonePe (2025).
- **Caveat:** its third-party *component* ecosystem (charts, docking, dialogs) is
  younger/smaller than Qt or the web.
- Industry-wide, the most *common* desktop UI approaches by volume are web-tech
  (Electron/Tauri) and Qt — Kotlin/Compose is rising but a smaller community.

## 5. Stack pros / cons (for this project's v1 scope)

### Kotlin + Compose Multiplatform
- **Pros:** matches skills; single Win/mac/Linux codebase; modern declarative UI;
  GPU Skia; native installers; JVM libs `nom-tam-fits` (FITS) + `metadata-extractor`
  (EXIF incl. RAW) cover v1 directly; native interop via FFM API later.
- **Cons:** younger widget ecosystem; full RAW pixel decode needs a native lib
  (libraw); fewer astro examples than Python; higher memory than Rust/C++.

### Java + JavaFX
- **Pros:** same JVM libs; very mature/stable; huge docs; proven (APP-style); easy.
- **Cons:** UI less "modern" than Compose; slower-moving; FXML/CSS feels dated.

### Python + PySide6 (Qt)
- **Pros:** richest astro ecosystem (`astropy`, `rawpy`, `Pillow`, ONNX AI); Qt is
  the most mature cross-platform UI; huge scientific community; fastest prototyping.
- **Cons:** new language; **hardest packaging/distribution**; lower runtime perf
  unless dropping to C; venv/dependency overhead.

### Tauri (Rust core + web UI)
- **Pros:** tiny fast binaries; excellent packaging/security; web component ecosystem.
- **Cons:** steep Rust curve; less mature astro bindings; two-world UI/logic split.

### Electron (TS + web UI)
- **Pros:** fastest UI dev; unlimited web components; trivial cross-platform.
- **Cons:** heavy memory/disk; not your language; needs native addons; least native.

### C#/.NET + Avalonia
- **Pros:** modern XAML UI; good cross-platform; strong tooling; solid perf.
- **Cons:** new language; smaller astro ecosystem; wrap native libs for FITS/RAW.

## 6. Decision matrix (priority → stack)

- **Kotlin/Java skills + modern UI** → Compose Multiplatform (modern) or JavaFX (safe).
- **Deepest astro/AI libs, OK learning Python** → Python + PySide6.
- **Smallest/fastest native binaries, OK learning Rust** → Tauri.
- **Heavy pixel-processing is paramount** → the pros used C/C++ (Qt) — big solo lift.

## 7. JVM library coverage for v1 (if we go Kotlin/Java)

- **FITS:** `nom-tam-fits` — mature, pure-Java, headers + pixel data.
- **EXIF/metadata:** `metadata-extractor` (Drew Noakes) — reads EXIF from JPEG/TIFF/
  PNG *and* RAW (CR2/NEF/ARW/DNG) without decoding pixels.
- **Thumbnails:** RAW files embed JPEG previews (instant); `TwelveMonkeys ImageIO`
  adds TIFF/extended decoding; Compose/Skia renders it all.
- **XISF:** no ready library, but XML header + binary blob — feasible to add later.
- **Gap:** full RAW pixel decoding needs a native lib (libraw bindings) — *not*
  required for v1 (metadata + embedded previews).

## 8. Status & next steps

- [x] Confirm requirements (cross-platform, metadata+thumbnails, photo+astro).
- [x] Benchmark stacks against reference tools.
- [ ] **Decide the stack** (pending — no decision made yet).
- [ ] Scaffold the chosen project (build tooling, deps, package structure).
- [ ] Implement v1: folder browse → thumbnails → metadata panel (EXIF + FITS).
- [ ] Native installers via chosen packaging path.

### Open question

Optimize the recommendation for:
1. Best fit for Kotlin/Java skills + modern UI (Compose vs JavaFX), or
2. Deepest astro/AI support even if learning Python, or
3. Long-term extensibility toward real image processing (native-interop paths), or
4. A small proof-of-concept in the top 2 options to compare feel.
