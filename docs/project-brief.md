# FishBoard Project Brief

## One-Sentence Definition

FishBoard is a rapid fish scanning system that uses an Android app, a fixed fish board/camera setup, and computer vision or AI to automatically capture fish images, generate fish IDs, identify species, measure length, and save reviewable records.

## Core Direction

FishBoard is not a normal photo app or a slow manual data-entry form. The product should be designed around field sampling speed:

```text
Fish enters the board area
-> App detects the fish
-> App captures the best image/frame
-> App creates a fish ID
-> AI predicts species
-> App measures length
-> App saves photo + metadata
-> App returns to Ready for the next fish
```

Manual input should be a fallback for review and correction, not the primary workflow.

## Design Principles

1. Rapid scan first.
2. Manual forms are secondary.
3. AI species ID and length measurement are core goals.
4. Every record must link back to an image.
5. Store raw data so mistakes can be reviewed.
6. Local storage first, export later.
7. Build the fake pipeline before real camera or AI.
8. Design the data model now so future AI can plug in.

## Core Value

FishBoard should create reliable fisheries sampling records quickly and repeatedly. The value is not just taking fish photos; the value is turning each fish into a traceable data record with minimal manual effort.

Each final fish record should include at least:

```text
fish ID
timestamp
photo path
species prediction
species confidence
length in mm
length confidence
review status
notes
```

Future fields may include:

```text
corrected species
corrected length
model version
calibration profile ID
session ID
location
weight
```

## Session and ID Strategy

FishBoard should support many users, devices, locations, and offline field sessions without creating confusing duplicate IDs.

Use two layers of identifiers:

```text
internal ID -> UUID for database/server use
display fish ID -> human-readable ID for field use, CSV, labels, and support
```

Session IDs should group a field run:

```text
YYYYMMDD-LOCATION-DEVICE-S###
```

Example:

```text
20260701-PIER01-D03-S001
```

Fish IDs should add a fish sequence within the session:

```text
YYYYMMDD-LOCATION-DEVICE-S###-F######
```

Example:

```text
20260701-PIER01-D03-S001-F000001
```

This gives field users an ID they can read, while still making it much less likely that records collide when many fishing people or devices collect data offline.

## Main App Areas

The app should be organized around these main screens:

```text
Home
Rapid Scan
Review Records
Record Detail
Calibration
Export Data
Settings
```

The home screen should stay simple:

```text
Start Rapid Scan
Review Records
Calibration
Export Data
Settings
```

## Rapid Scan State Machine

Rapid scan should behave like a state machine:

```text
Idle
-> Ready
-> Detecting
-> Fish Detected
-> Stabilizing
-> Capturing
-> Processing
-> Saving
-> Saved
-> Ready
```

This is important so the app can avoid duplicate records, blurry captures, partial fish captures, and confusing old-frame processing.

## Hardware Assumptions

Initial design assumptions:

```text
Phone or tablet is fixed above the board
Fish board position is fixed
Lighting is stable
Background has strong contrast
Calibration marker or known ruler exists
System should work local-first and ideally offline
```

The physical board may include:

```text
fixed background board
ruler or calibration marker
fish placement zone
phone/tablet mount
fixed LED lighting
optional guide channel or slide
```

## Computer Vision and AI Tasks

The AI/CV work should be split into smaller tasks:

```text
fish presence detection
fish bounding box
fish segmentation or contour detection
length measurement
species classification
confidence scoring
```

The first implementation can use placeholders, then replace each fake part with a real implementation over time.

## Length Measurement Roadmap

1. Manual length input.
2. Manual head/tail point selection on a saved image.
3. Semi-automatic contour or longest-axis estimate.
4. Fully automatic rapid-scan length measurement.

Core formula:

```text
lengthMm = pixelDistance * mmPerPixel
```

## Species Identification Roadmap

1. Placeholder species prediction.
2. Manual species entry and correction.
3. Simple classifier integration.
4. Production model trained or tuned from FishBoard image data.

Species predictions should eventually preserve:

```text
predicted species
confidence
model version
possibly top-3 predictions
manual correction status
```

## Data Storage

Use separate storage for photos and metadata:

```text
photos -> phone file storage
metadata -> Room database
photo filename and relative path -> stored in database
```

Do not store full image bytes directly inside the database.

Each fish image should have a stable filename derived from the display fish ID:

```text
20260701-PIER01-D03-S001-F000001.jpg
```

Inside exports, photos should be organized by session:

```text
images/
|-- 20260701-PIER01-D03-S001/
    |-- 20260701-PIER01-D03-S001-F000001.jpg
    |-- 20260701-PIER01-D03-S001-F000002.jpg
```

The CSV should not contain raw image bytes. It should contain the image filename and relative image path so each row can point back to the matching exported image.

Current local storage direction:

```text
sessions -> Room table: scan_sessions
records -> Room table: fish_records
exported status -> fish_records.exportedAtMillis
photos -> file storage, linked by filename / relative path
```

The app should treat Room as the local source of truth for sessions, fish records, review corrections, and export state.

## Export Shape

Exports should support CSV and images, eventually as a ZIP package:

```text
FishBoardExport_YYYY_MM_DD.zip
|-- records.csv
|-- missing_images.csv
|-- images/
    |-- SESSION_ID/
        |-- SESSION_ID-F000001.jpg
        |-- SESSION_ID-F000002.jpg
```

When real captured images are available, export should copy them into the matching `images/SESSION_ID/` folder. If a record points to an image that is not available, export should still succeed and list that record in `missing_images.csv` so missing photos are visible instead of silently ignored.

Example CSV fields:

```csv
fishId,timestamp,species,speciesConfidence,lengthMm,lengthConfidence,reviewed,photoFilename,notes
F000001,2026-07-01 14:30:12,Bluefish,0.87,234,0.91,false,F000001.jpg,
```

## Development Roadmap

The current project should move in this order:

1. App skeleton: Home, Rapid Scan, Records, Calibration, Export.
2. Fake rapid scan flow with simulated fish records.
3. Records review UI.
4. Local database with Room.
5. CameraX manual capture.
6. Calibration.
7. Manual or semi-automatic length measurement.
8. Fish detection.
9. Automatic capture rapid scan.
10. AI species model.
11. Automatic length measurement.
12. Export and possible FAST Platform integration.

## Immediate Next Step

Build the fake rapid scan prototype:

```text
Start Rapid Scan
-> Simulate Fish Scan
-> Auto-generate fish ID
-> Fake species / fake length
-> Save record
-> Ready for next fish
-> Review Records
```

This keeps the app aligned with the final system from the beginning, even before real camera, calibration, length measurement, or AI are available.

## Reference Inspiration

FishBoard is conceptually inspired by the MER Consultants FAST Platform:

https://merconsultants.org/fast-platform/

Important reference ideas from FAST:

```text
automated fisheries data collection
simple app for low-training field use
automated image capture
automated length, weight, and species ID
local app storage before upload
quality control and review workflow
custom hardware/software for field conditions
```
