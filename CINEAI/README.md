# CINEAI

CINEAI is a local-first Android computational photography/video enhancement app. It imports existing media, keeps originals in app-private storage, analyzes photos locally, applies a real scene-aware tonal/color pipeline, and exports final copies through MediaStore.

## Build

- Android Studio Rabbit 1 / Quail 4 or newer compatible IDE
- JDK 17
- Android SDK 36
- Gradle 9.6

Open the project in Android Studio and sync. The included wrapper properties target Gradle 9.6.

## Real processing

### Photos
`PhotoProcessor` performs real decode → EXIF orientation → scene analysis → exposure/tonal curve/highlight-shadow recovery → temperature/tint → vibrance/saturation → vignette → high-quality resize → JPEG encode. The default target is 3840 px on the long side while preserving aspect ratio.

### Video
`VideoProcessor` uses Media3 Transformer and hardware-backed MediaCodec where supported. It applies real GPU effects (brightness, contrast, RGB balance, HSL and optional blur/sharpness) and exports asynchronously while retaining the source audio track.

### AI/ML
Bundled ML Kit face detection is used by `SceneAnalyzer` to protect portrait/group processing choices. No cloud API is required. A future ONNX/TFLite super-resolution model can be inserted behind `ImageUpscaler` without changing UI/repository architecture.

## Privacy
Imported originals are copied to `filesDir/media/originals`. They are never overwritten by enhancement. Only successful final exports are written to the public Gallery under `Pictures/CINEAI` or `Movies/CINEAI`.

## Important limitations
Software cannot recreate the physical sensor, lens optics, depth of field, rolling-shutter behavior, or genuine sensor dynamic range of a DSLR/mirrorless camera. CINEAI aims for a professional-looking tonal/color/detail response using real processing. Conventional upscaling does not create genuine captured detail.

## Verification status

The repository was statically reviewed in this environment for source structure, dependency/API alignment against the current Android documentation, private-storage/export invariants, and absence of mock-processing code. A full Android Gradle build could not be executed here because this runtime does not contain an Android SDK/Gradle dependency cache and has no outbound package-download access. The supplied Gradle wrapper is configured for Gradle 9.6 so Android Studio can resolve the build normally.
