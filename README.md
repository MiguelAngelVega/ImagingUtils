# ImagingUtils

A cross-platform desktop application for managing photography and
astrophotography files. It browses a folder of images, renders thumbnails, and
displays rich metadata — EXIF/IPTC/XMP for regular photos and RAW, and full FITS
headers for astronomical images.

> Status: early v1 skeleton — folder browsing, thumbnail grid, and metadata panel.

## Features

- **Folder browsing** — pick a directory and list supported image files.
- **Thumbnail grid** — adaptive grid with thumbnails decoded off the UI thread.
- **Metadata panel** — grouped, scrollable key/value view:
  - Photos / RAW: EXIF, IPTC, XMP (via metadata-extractor).
  - FITS: every HDU header card (via nom-tam-fits).
- **FITS preview** — renders the first 2D image plane to a normalized grayscale
  thumbnail.

Supported extensions: `jpg/jpeg/png/tif/tiff/bmp/gif/webp` (photo),
`cr2/cr3/nef/arw/dng/raf/orf/rw2/pef/srw` (RAW), `fits/fit/fts` (FITS),
`xisf` (recognized; preview pending).

## Tech stack

| Concern            | Choice                                            |
| ------------------ | ------------------------------------------------- |
| Language           | Kotlin 2.1.21 (JVM target 21)                     |
| UI framework       | Compose Multiplatform for Desktop 1.8.2 (Material 3) |
| Rendering          | Skiko / Skia (native binaries per OS)             |
| Build tool         | Apache Maven                                       |
| Async              | kotlinx-coroutines (Swing dispatcher)             |

### Imaging libraries

| Library                         | Version | Purpose                                   |
| ------------------------------- | ------- | ----------------------------------------- |
| `nom-tam-fits`                  | 1.22.1  | FITS headers + pixel data                 |
| `metadata-extractor`            | 2.21.0  | EXIF / IPTC / XMP for JPEG, TIFF, PNG, RAW |
| TwelveMonkeys ImageIO (jpeg/tiff/webp/bmp) | 3.14.0 | Extended `ImageIO` format support |

## Prerequisites

- **JDK 21** (tested with Eclipse Temurin 21). Compose Desktop requires JDK 11+.
- **Apache Maven 3.9+** on your `PATH`.

Verify:

```
java -version
mvn -version
```

## Build & run

Compile:

```
mvn clean compile
```

Run the application (opens the desktop window):

```
mvn exec:java
```

Package the compiled classes into a JAR (under `target/`):

```
mvn package
```

> Note: this JAR contains only the app classes — use `mvn exec:java` to run,
> which puts the Compose/imaging dependencies on the classpath automatically.

### macOS note

Running an AWT/Swing-based UI through `exec:java` on macOS may require starting
on the first thread. If the window does not appear, launch with:

```
mvn exec:exec -Dexec.executable=java \
  -Dexec.args="-XstartOnFirstThread -classpath %classpath com.imagingutils.MainKt"
```

## Project structure

```
ImagingUtils/
├── pom.xml                      # Maven build (Kotlin + Compose Desktop)
├── Blueprints.md                # Benchmarking & architecture notes
├── README.md
└── src/main/kotlin/com/imagingutils/
    ├── Main.kt                  # Window, folder picker, split-pane layout
    ├── Ui.kt                    # ThumbnailGrid + MetadataPanel composables
    ├── Model.kt                 # File classification & folder scanning
    ├── Metadata.kt              # EXIF + FITS metadata readers
    └── Thumbnails.kt            # ImageIO / FITS thumbnail rendering
```

## Build notes (Compose Multiplatform on Maven)

Compose Multiplatform is designed around the Gradle plugin, so a few things are
wired manually here for a pure-Maven build:

- **Compose compiler** is enabled via the Kotlin compiler plugin
  `org.jetbrains.kotlin:kotlin-compose-compiler-plugin` (the non-embeddable
  "hosted" variant), passed to `kotlin-maven-plugin` with `-Xplugin`.
- **Desktop artifacts** are declared as explicit `*-desktop` JVM modules
  (`runtime-desktop`, `foundation-desktop`, `ui-desktop`, `material3-desktop`)
  rather than the `compose.desktop.currentOs` aggregator, which pulls in Android
  `.aar` variants under plain Maven.
- **Native Skia** binaries are selected per host OS/arch via Maven profiles that
  set the `skiko-awt-runtime-<platform>` classifier.

### Not yet included

- Native installers (`.msi` / `.dmg` / `.deb`). The Compose Gradle plugin
  provides these out of the box; on Maven they would be added later via
  `jpackage`.
- RAW/XISF thumbnail decoding (currently shown as a labeled placeholder).

## Roadmap

- Embedded-JPEG previews for RAW files; XISF reader.
- Recursive folder scanning and filtering.
- Unit tests for classification, scanning, and metadata readers.
- `jpackage`-based native distributions.
