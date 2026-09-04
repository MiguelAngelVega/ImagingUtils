# Autostretch (MTF-based display stretch)

Siril's **Autostretch** display mode is a non-linear, statistics-driven
stretch — a *Midtones Transfer Function (MTF)* auto-stretch, the same family
of algorithm as PixInsight's ScreenTransferFunction (STF). It reveals faint
detail that the plain **Linear** display hides.

## Why "Linear" hides the detail

A FITS frame has enormous dynamic range. The interesting signal (nebulosity,
faint galaxy arms) sits just barely above the sky background, while a few stars
are thousands of times brighter. A **linear** map of `[min, max] -> [0, 255]`
squeezes the background and faint signal into the bottom few percent of the
range, so they render as near-black, while bright stars own the rest of the
range. That is why little is visible in Linear.

## What Autostretch does

It picks a black point from **robust statistics**, then applies a non-linear
curve that expands the dark end and compresses the bright end.

1. **Normalize** data to `[0, 1]`.
2. **Robust stats** (resistant to bright-star outliers):
   - `median` of the pixels
   - `MAD` = median absolute deviation, then `MADN = 1.4826 * MAD`
     (scales MAD to be comparable to a standard deviation).
3. **Clipping points** (Siril defaults: shadows clip `C = -2.8`,
   target background `B = 0.25`):
   - `shadows = clamp(median + C * MADN, 0, 1)` -> the black point, a few
     MADs below the median.
   - `highlights = 1.0`.
4. **Midtones balance** `m`, chosen so the background maps to `B = 0.25`:
   - `m = MTF(B, median - shadows)`
5. **Apply the transfer** to every pixel: first rescale into
   `[shadows, highlights]`, then push through the MTF.

### Midtones Transfer Function

```
MTF(m, x) = ((m - 1) * x) / ((2m - 1) * x - m)

with  MTF(m, 0) = 0,  MTF(m, 1) = 1,  MTF(m, m) = 0.5
```

For `m < 0.5` this is a concave, log-like curve: it strongly lifts low values
(faint nebula) toward mid-gray while gently compressing the highlights, so
detail appears without stars blowing out the whole frame.

There is a mirrored branch when `median > 0.5` (inverted / bright-background
images): clip **highlights** instead and use `1 - MTF(...)`.

## Reference pipeline (pseudocode)

```
normalize v -> [0, 1]
median = median(v)
madn   = 1.4826 * median(|v - median|)

shadows = clamp(median - 2.8 * madn, 0, 1)   // C = -2.8
m       = MTF(0.25, median - shadows)        // B = 0.25

for each pixel:
    x   = clamp((v - shadows) / (1 - shadows), 0, 1)
    out = MTF(m, x) * 255
```

## Key properties of Siril's behavior

- **Display-only / non-destructive** — the pixels on disk stay linear; only
  what is shown is stretched. Toggling Linear <-> Autostretch is instant and
  reversible.
- **Color** can run **linked** (one `m` for all channels, preserves color
  balance) or **unlinked** (per-channel, more aggressive, can shift color).
- The defaults `B = 0.25`, `C = -2.8` produce the familiar "reveal the
  nebula" look.

## Relation to this app

The current FITS renderer (`Thumbnails.kt`) implements only the **Linear**
path: it finds `min`/`max` and maps linearly to grayscale. To match Siril,
add an Autostretch mode: after flattening the plane, compute `median` and
`MADN`, derive `shadows` and `m`, then map each pixel through the MTF instead
of the linear `(v - min) / (max - min)`.
