# OpenDrive source fork

This checkout is the OpenDrive development fork of AARE. The Android app label and Gradle project name are OpenDrive; the original AARE namespace and application ID remain for this initial baseline.

Upstream: https://github.com/prototux/AARE.git (`upstream/main`).

The `opendrive` branch preserves the upstream history and carries OpenDrive changes. Build from the parent folder with `../build.ps1`; that script fetches and merges the latest upstream main before testing and packaging `../build/opendrive-debug.apk`.

See `../README.md` for the build/update workflow and planned OpenAI voice assistant. No OpenAI integration is implemented in this baseline. Original licensing and credits are retained in `LICENSE` and `README.md`.
