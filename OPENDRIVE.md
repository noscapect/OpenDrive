# OpenDrive source fork

This checkout is the OpenDrive development fork of AARE. The first published release is [OpenDrive 0.1.0](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0). The Android app label and Gradle project name are OpenDrive; the inherited namespace and application ID remain `net.prototux.aare`.

Upstream: https://github.com/prototux/AARE.git (`upstream/main`).

Public repository: https://github.com/noscapect/OpenDrive (`origin/main`; the local workspace branch is `opendrive`). See [release status](docs/opendrive-status.md), [changelog](CHANGELOG.md) and [roadmap](docs/opendrive-roadmap.md).

The development branch preserves the upstream history and carries OpenDrive changes. Build from this repository with `./tools/opendrive/build.ps1`; that script fetches and merges the latest upstream main before testing and packaging `build/opendrive/opendrive-debug.apk`. Our existing local workspace also has a parent `build.ps1` wrapper that writes to `../build`.

OpenDrive's own version lives in `opendrive-version.properties`, separately from AARE. Add `-Release` for the exact version name; normal builds append a development suffix. Both use local debug signing, and neither automatically publishes a GitHub release. The release tag identifies the APK source; later documentation commits do not change the published APK.

OpenDrive's own assistant lives in `app/src/main/java/net/opendrive/assistant`, connected to the upstream runtime and phone settings through small integration points. It provides OpenAI speech input, follow-up conversation, spoken answers, navigation destination search and music search/playback. Setup and test limits are documented in `docs/opendrive-assistant.md`; future feature ideas are tracked in `docs/opendrive-roadmap.md`.

The cockpit, EV data view, favorites, Home Assistant client, local trip book, temporary passenger server and charging break screen live in `app/src/main/java/net/opendrive/features`. Setup and validation limits are in `docs/opendrive-features.md`. The phone has a dedicated OpenDrive settings category; the car launcher exposes the new feature screens.

The AARE remote is fetch-only in our workflow: its push URL is `DISABLED`, enforced by `tools/opendrive/update-upstream.ps1`. No OpenDrive changes are published to AARE. Original licensing and credits are retained in `LICENSE` and `README.AARE.md`.
