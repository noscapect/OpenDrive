# Changelog

## 0.1.0 — 2026-11-10

First public, experimental OpenDrive release, based on AARE 1.4.0 and upstream commit `5d932ae`.

### Added

- Configurable cockpit with navigation, music, EV and favorite tiles.
- OpenAI voice assistant with follow-up context, spoken answers, destination search and music search/playback.
- EV dashboard with reported car values, configured Home Assistant sensors and labeled manual values/estimates.
- Favorite destinations, supported apps and parked web services.
- Home Assistant buttons with explicit action confirmation.
- Opt-in local trip book with categories, retention, deletion and CSV export.
- Temporary authenticated LAN passenger controls for music and driver-confirmed destination proposals.
- Parked charging-break screen with energy information and a session timer.
- Portable build/update scripts, feature guides and a public source repository retaining AARE history and licensing.

### Versioning and distribution

- OpenDrive version `0.1.0`, independently of the upstream AARE version; Android version code `10401` preserves updates from earlier local builds.
- Debug-signed APK using the existing local signing key. The inherited application ID remains `net.prototux.aare`.
- [Release, APK and checksum](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0); release source commit `a507d0a`.
- AARE remains a read-only upstream in the OpenDrive workflow.

### Validation and known limits

- 201 local protocol/app tests passed; APK version, signature and public download checksum verified.
- Phone/car hardware validation remains pending, including the Kia Niro certificate issue.
- Users configure their own optional API/service credentials on the phone; none are bundled.
- ChatGPT account/history access, external agents, direct OBD/Kia connectivity, automatic arrival actions and charging-stop planning are not implemented.

See the [release status](docs/opendrive-status.md) for exact checksums, provenance and validation boundaries.
