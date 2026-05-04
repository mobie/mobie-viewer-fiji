# MoBIE Updates — April 2025 to May 2026

Here's a summary of the most significant changes since the last update.

---

## Pixel Value Overlay at Mouse Position
The pixel value overlay (showing the intensity value under the mouse cursor in BDV) has been substantially improved and is now more reliable and informative.

---

## New Blending Mode: AND Blending
A new **AND blending** mode has been implemented, allowing users to combine images in a logical AND fashion — useful for overlaying masks or segmentations.

---

## Thin Plate Spline (TPS) Transformation Support
MoBIE now supports **Thin Plate Spline transformations**. TPS-transformed images can be loaded from collection tables, enabling non-rigid/elastic registration workflows.

---

## Save as OME-Zarr
A new **Save As OME-Zarr** command has been added, making it straightforward to export images directly to the OME-Zarr format from within MoBIE.

---

## Numeric Annotation Display
A new **Numeric Annotation Display** has been implemented, allowing numeric (scalar) per-image values from a table to be displayed as colour overlays — including auto-contrast support.

---

## Auto-Detection of 2D/3D Browsing Mode
MoBIE now **automatically detects** whether to open a dataset in 2D or 3D browsing mode, removing the need to manually configure this in most cases.

---

## Collection Table Improvements
Many improvements to the collection table workflow:
- **Create Collection Table command**: A new GUI command to create collection tables directly from within MoBIE, including file name expression parsing.
- **Excel table reader**: Collection tables can now be loaded from `.xlsx` Excel files.
- **Auto-contrast**: Automatic contrast adjustment when opening collection tables.
- **Grid position support**: Non-integer and pair grid positions are now supported; rows with a `grid_position` column (but no `grid` column) are added to the default grid automatically.
- **Automatic JSON parsing**: JSON files in the collection table folder are parsed automatically.
- **Duplicate source name handling**: Improved handling of duplicate source names.
- **RegionDisplay for all views**: Region displays are now generated for all views in a collection table.
- **Display order fix**: Fixed a bug where display order was incorrect when opening collection tables.
- **Multiple displays bug fix**: Fixed a bug where multiple displays were wrongly created.

---

## Anisotropic Spot Rendering
Spots are now rendered **anisotropically**, correctly accounting for non-isotropic voxel sizes.

---

## Spots Rendering in BigVolumeViewer (BVV)
Spots are now rendered (with shading) in the **BigVolumeViewer** 3D view.

---

## 3D Screenshot Stack
The screenshot functionality has been extended to support **volumetric (stack) screenshots**, capturing a z-series of 2D slices.

---

## MoBIE Menu Restructuring
The MoBIE Fiji plugin menu has been **reorganised** for better discoverability and a more logical item order.

---

## Region Labels Rendered Even with Overlap
Region label overlays now appear even when there is spatial overlap between regions.

---

## Bug Fixes
- Fixed contrast limit bug (#1233)
- Fixed CLI project opening
- Fixed HCS plate view issues
- Fixed linking of segmentations and S3-hosted images
- Fixed BigVolumeViewer classpath issue (#1267)
- Fixed `WindowListener` bug in BigVolumeViewer causing crashes on close (#1238)
- Fixed spot label image creation from `maxLabel` (#1255)
- Fixed colour map logging issue (reported on forum)
- Various further smaller fixes (#975, #1279, #1281, #1285, #1292, #1293, #1294, #1296)
