# Changelog

All notable changes to `niimbot_print` will be documented here.

## 0.1.5-bg

### Added

- Add `onStartPrintRollLabel(code, lines, qrSizeMm)` to print a QR code and up to a few lines of
  text on a single 50 x 30 mm label. Implemented on Android; iOS returns an `UNIMPLEMENTED`
  `FlutterError` for now.

## 0.1.4

### Fixed

- Fixed iOS label printing being aborted when `drawLableText` reports a false-negative result.
- Fixed connected printers being treated as disconnected on iOS by checking both the native connection state and active printer name.
- Fixed successful iOS connections unexpectedly returning users to an empty printer scan screen when printing.

### Changed

- Generate and validate label JSON before starting the physical print job, following the official Niimbot iOS SDK demo flow.
- Continue generating label JSON when an individual SDK draw call reports `NO`, and fail only when no valid label data is generated.
- Added native diagnostic logging for connection status and label drawing parameters.

## 0.1.3

### Fixed

- Fixed false “Enable Bluetooth” warnings on iOS after granting Bluetooth permission or enabling Bluetooth.
- Wait up to three seconds for CoreBluetooth to transition from `unknown` or `off` to `on`.
- Fixed iOS label text rendering failures that caused the “Unable to draw label content” error.
- Preserve label font sizes when long text wraps onto two lines.
- Added explicit errors for missing font resources, font initialization failures, and print timeouts.

### Changed

- Updated iOS font initialization to follow the official Niimbot SDK v4.1.1 implementation.
- Updated drawing-board initialization to use `ZT001.ttf` through `fontArray`.
- Added a 30-second native print timeout.

### Added

- Added official iOS font resources: `FONT.json`, `ZT001.ttf`, and `ZT002.otf`.
- Added tests for delayed Bluetooth initialization and disabled Bluetooth states.

## 0.1.2

- Isolate Android discovery results per scan so stale devices are never returned by a later scan.
- Add horizontal text safe margins so long label text auto-shrinks instead of being clipped.
- Wrap long text at word boundaries into at most two lines before printing.
- Keep the iOS podspec, example deployment target, and CocoaPods lockfile aligned with the package version.

## 0.1.1

- Correct the package homepage metadata.

## 0.1.0

- Include the required native iOS libraries in the published package.
- Refresh the installation, platform setup, and usage documentation.
- Add QR code printing on Android and iOS.
- Upgrade the bundled Android SDK to 4.1.1 and the iOS JCAPI SDK to 3.2.8.
- Include the Android LPAPI dependency required by supported third-party printer paths.
- Correct the Android QR code type and rotation argument order.
- Fix Android connection result fall-through and scan lifecycle handling.
- Fix the iOS scan timeout unit and guard native result callbacks.
- Correct Bluetooth permissions for Android 12 and earlier.
- Add Dart, native, and integration test coverage.
- Update the Android example to AGP 9.1, Gradle 9.3, Java 17, and SDK 37.

## 0.0.6

- Fix the libs that are not found on android

## 0.0.5

- Update the Readme.md

## 0.0.4

- Improve Bluetooth scan and connect stability on Android and iOS.
- Internal cleanup of Gradle/iOS config and permission notes.
- Update the plugins and dependencies on Android.
- Remove Linux, Windows, and macOS from the supported platforms for better plugin compatibility.

## 0.0.3

- Add cross-platform declarations so the package is visible for Android, iOS, Windows, macOS, and Linux targets.¹
- Publish updated README with “Getting Started” steps and example code.
- Expose basic printing API:
    - `onStartScan(...)`
    - `onStartConnect(...)`
    - `onDisconnect()`
    - `onStartPrintText(...)`

## 0.0.2

- Integrate `flutter_blue_plus` for Bluetooth operations and `permission_handler` for runtime permissions.²
- Add simple example app demonstrating scan → connect → print text.
- Basic error handling and result callback plumbing.

## 0.0.1

- Initial publish scaffold for the Flutter plugin.
- Set up plugin structure and minimal platform code.
- Placeholder docs.

---

¹ Package page shows desktop platforms listed in metadata.  
² See dependencies listed on the package page.
