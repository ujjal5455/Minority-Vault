# Minority Vault

An offline, local-multiplayer Android party game of negotiation and betrayal for 4–8 players. Only the minority wins.

Read [CLAUDE.md](CLAUDE.md) for the rules, architecture and conventions. [prompt.md](prompt.md) is the phase-by-phase build plan.

## Requirements

- JDK 17 or newer (21 recommended). Android Studio's bundled JDK works.
- Android SDK with platform 37 (Android 17).
- A phone running Android 8.0 (API 26) or newer.

If you build from a terminal, point Gradle at a JDK and the SDK:

- `JAVA_HOME` set to a JDK, for example Android Studio's `jbr` folder.
- `local.properties` in the repo root containing `sdk.dir=<path to your Android SDK>` (Android Studio creates it for you; it is git-ignored). On Windows use forward slashes, e.g. `sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk`.

## Build and check

```bash
./gradlew assembleDebug              # debug APK at app/build/outputs/apk/debug/
./gradlew :engine:test               # engine unit tests
./gradlew test                       # all unit tests
./gradlew ktlintCheck detekt         # style and static analysis
./gradlew ktlintFormat               # auto-fix style issues
```

CI runs `./gradlew ktlintCheck detekt test assembleDebug` on every push and pull request.

## Install on a phone

Enable USB debugging on the phone, connect it, then:

```bash
./gradlew installDebug
```

Or copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it.

## Modules

| Module | Purpose |
|---|---|
| `:app` | Activity, navigation, Hilt setup, wiring |
| `:engine` | Pure Kotlin game rules (no Android) |
| `:protocol` | Pure Kotlin network messages and views |
| `:net` | Nearby Connections transport |
| `:ai` | Vault Keeper and bots |
| `:data` | Room and DataStore |
| `:designsystem` | Theme and reusable components |
| `:audio` | Sound, music and haptics |
| `:feature:*` | Screens: lobby, round, results, history, settings, tutorial |

The allowed dependency direction is enforced at build time: a forbidden project dependency fails the build.
