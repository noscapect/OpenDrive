# OpenDrive

An experimental Android Auto phone-side app built on [AARE](https://github.com/prototux/AARE), with a configurable cockpit and an optional OpenAI voice assistant.

OpenDrive preserves AARE's source history and takes upstream updates into its own development branch. OpenDrive changes are published here, never pushed to AARE. See [AARE's original README](README.AARE.md) for the upstream project, credits, protocol setup and compatibility notes.

## Download

The first release is **OpenDrive 0.1.0**: [download the prebuilt APK](https://github.com/noscapect/OpenDrive/releases/download/v0.1.0/opendrive-0.1.0.apk) or see the [release notes](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0).

This initial APK is debug-signed with the same local key as our earlier builds. It uses Android version code 10401 so it can update those builds despite OpenDrive starting its own version sequence at 0.1.0. Hardware validation is still pending.

See [SHA256SUMS](https://github.com/noscapect/OpenDrive/releases/download/v0.1.0/SHA256SUMS), the [release status and validation record](docs/opendrive-status.md), and the [changelog](CHANGELOG.md).

## Features

- Configurable cockpit with navigation, music, EV and favorite tiles.
- Optional OpenAI speech assistant with follow-up context, destination search and music search/playback.
- EV dashboard using reported car data or configured Home Assistant sensors. Missing readings remain unknown; manual values and estimates are labeled.
- Favorite destinations, supported apps and parked web services.
- Home Assistant buttons with confirmation before an action is sent.
- Opt-in local trip book with categories, retention, deletion and CSV export.
- Temporary, authenticated LAN passenger controls for music and destination proposals. The driver confirms destinations.
- Parked charging-break screen with energy information and a session timer.

These are first implementations. The latest local validation passed **201 protocol/app tests**, and APK signing verification passed. Phone UI, actual OpenAI/Home Assistant calls, audio, LAN pairing and Kia Niro compatibility have not yet been validated together on hardware. This is not a production or certified automotive release.

## Build

Install a JDK 17-21 (21 recommended) and the Android SDK, including platform 37 and Android SDK Build Tools. Set `JAVA_HOME` and `ANDROID_HOME`, or open the project in Android Studio.

From a clean checkout, using PowerShell:

```powershell
./tools/opendrive/build.ps1
```

The script configures a read-only AARE `upstream` remote if needed, fetches the latest `upstream/main`, merges it, runs protocol/app tests, builds the debug APK and verifies its signature. Commit local changes before updating; a fetch failure or merge conflict stops the build.

Outputs:

```text
build/opendrive/opendrive-debug.apk
build/opendrive/build-info.json
build/opendrive/apk-verification.txt
build/opendrive/tests/
```

`build-info.json` records the exact OpenDrive and AARE commits, APK checksum and test results. Use `-OutputDirectory <path>` for a different output location. `-SkipUpdate` deliberately skips fetching; it requires an already configured upstream reference and is recorded in the build metadata.

OpenDrive's own version is stored in `opendrive-version.properties`, independently of AARE. Add `-Release` to build the exact version (`0.1.0`); normal builds append `-dev.<commit>`. This flag controls version labeling and still builds a debug-signed APK with the local key; it does not supply a production signing key or publish anything automatically.

For a direct Gradle build (does not update upstream automatically):

```sh
./gradlew :aap:test :app:testDebugUnitTest :app:assembleDebug
```

On Windows use `gradlew.bat`. The APK is then under `app/build/outputs/apk/debug/`. CI also runs desktop head-unit tests and uploads a debug APK artifact. CI uses its own temporary debug signing key: its APK cannot update an installation signed with a different local or official AARE key. Signing keys are never committed here.

## Configure on the phone

The **OpenDrive** settings category contains Cockpit, EV, Favorites, Home, Trips and Passenger settings. Configure the optional voice assistant separately under Voice assistant. Users enter their own credentials on the phone; no API keys or account tokens ship in the source or APK.

- [Cockpit, EV, Home Assistant, trips, passenger controls and charging break](docs/opendrive-features.md)
- [OpenDrive voice assistant](docs/opendrive-assistant.md)
- [Feature status and roadmap](docs/opendrive-roadmap.md)
- [AARE certificates and head-unit compatibility](docs/certificates.md)

The assistant currently uses the OpenAI API with a user-provided API key. ChatGPT account sign-in, existing ChatGPT conversations and external Buddy/Hermes/Codex agents are not integrated. The EV view does not include a direct OBD or Kia account driver. Home Assistant requires a configured HTTPS endpoint and token.

## Repository structure

```text
app/                  Android application and OpenDrive feature code
  src/main/java/net/opendrive/assistant/
  src/main/java/net/opendrive/features/
aap/                  Upstream Android Auto protocol implementation
headunit/             Upstream desktop head unit
xposed/               Upstream optional integration
tools/opendrive/      Portable build and upstream update scripts
docs/                 Feature guides, upstream research and roadmap
```

The initial app label is OpenDrive, while the inherited Android application ID remains `net.prototux.aare`. It therefore replaces a compatible, equally signed local AARE/OpenDrive installation rather than installing alongside it. A separate package identity is future work.

Releases are currently published manually; version 0.1.0 is available above. The inherited AARE release workflow is preserved as documentation in `docs/aare-release-workflow.yml`, not executed in this repository. Automatic release publication and production signing have not been configured.

## License and attribution

GPL-3.0-or-later; see [LICENSE](LICENSE). This project is based on [prototux/AARE](https://github.com/prototux/AARE); original authorship, license notices and history are retained. Protobuf definitions originate from aasdk; see `aap/src/main/proto/NOTICE.md`. Other bundled components retain their respective licenses.

OpenDrive is an independent project and is not affiliated with Google, Kia, OpenAI or the AARE maintainers. Android Auto and ChatGPT are trademarks of their respective owners.
