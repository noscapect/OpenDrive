# OpenDrive source fork

This checkout is the OpenDrive development fork of AARE. The Android app label and Gradle project name are OpenDrive; the original AARE namespace and application ID remain for this initial baseline.

Upstream: https://github.com/prototux/AARE.git (`upstream/main`).

The `opendrive` branch preserves the upstream history and carries OpenDrive changes. Build from the parent folder with `../build.ps1`; that script fetches and merges the latest upstream main before testing and packaging `../build/opendrive-debug.apk`.

OpenDrive's own assistant lives in `app/src/main/java/net/opendrive/assistant`, connected to the upstream runtime and phone settings through small integration points. It provides OpenAI speech input, follow-up conversation, spoken answers, navigation destination search and music search/playback. Setup and test limits are documented in `docs/opendrive-assistant.md`; future feature ideas are tracked in `docs/opendrive-roadmap.md`.

The AARE remote is fetch-only in our workflow: its push URL is `DISABLED`, enforced by `../update-upstream.ps1`. No OpenDrive changes are published to AARE. Original licensing and credits are retained in `LICENSE` and `README.md`.
