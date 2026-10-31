# Permissions

Every Android permission the app requests is listed here with the reason. Adding a permission without an entry here fails the definition of done (CLAUDE.md §11).

All of these come from Google Play services **Nearby Connections**, which lets phones in the same room find and talk to each other over Bluetooth and Wi-Fi with no internet. The list follows Google's [Nearby Connections setup guide](https://developers.google.com/nearby/connections/android/get-started) (checked October 2026), with one correction found on a device (below), and is declared in `net/src/main/AndroidManifest.xml`. Which ones are asked for at runtime on each Android version is decided in `NearbyPermissions.required()`, which has a unit test.

Players are asked only when they choose **Host a game** or **Join a game**. Pass-the-phone play needs no permissions at all, so the game is always playable even if someone says no.

## Install-time permissions (granted automatically)

| Permission | Android versions | Why |
|---|---|---|
| `ACCESS_WIFI_STATE` | all | Nearby checks Wi-Fi state to pick the fastest connection. Google's guide caps it at API 32, but on Android 17 Nearby refused to advertise without it ("Missing permission ... ACCESS_WIFI_STATE" in the log), so it is declared on every version. |
| `CHANGE_WIFI_STATE` | all | Nearby can set up a direct Wi-Fi link between phones. Declared everywhere for the same reason. |
| `BLUETOOTH` | up to 11 (API 30) | Bluetooth connections on older Android. |
| `BLUETOOTH_ADMIN` | up to 11 (API 30) | Bluetooth discovery on older Android. |
| `ACCESS_NETWORK_STATE` | all | Added by the Nearby and Media3 libraries themselves. |
| `WAKE_LOCK` | all | Added by Media3 ExoPlayer (`:audio`), which keeps music playing smoothly. |
| `<app id>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | all | Added by AndroidX Core. It is private to this app and lets it register broadcast receivers that other apps can't reach. |

## Runtime permissions (the player is asked)

| Permission | Android versions | Why |
|---|---|---|
| `ACCESS_COARSE_LOCATION` | up to 12L (API 32) | Before Android 12, scanning for Bluetooth and Wi-Fi devices counts as location access. The game never reads the phone's location. |
| `ACCESS_FINE_LOCATION` | 10 to 12L (API 29–32) | Same reason, for the versions that require fine location for scanning. |
| `BLUETOOTH_ADVERTISE` | 12+ (API 31+) | The host's phone announces its room. |
| `BLUETOOTH_CONNECT` | 12+ (API 31+) | Phones connect to the host. |
| `BLUETOOTH_SCAN` | 12+ (API 31+) | Players' phones look for rooms. Declared with `neverForLocation`: scans are never used to work out where anyone is. |
| `NEARBY_WIFI_DEVICES` | 13+ (API 33+) | Faster Wi-Fi connections between phones. Also `neverForLocation`. |
| `ACCESS_LOCAL_NETWORK` | 17+ (API 37+) | Apps targeting Android 17 need it for Nearby's Wi-Fi LAN connections. It is in the same "Nearby devices" group as the Bluetooth permissions, so players who allowed those are not asked twice. |

## How the app asks

1. Before the system prompt, a short screen explains why in the Vault Keeper's voice: finding the other phones in the room, no internet, no location tracking.
2. If the player says no, the screen says what still works (pass-the-phone) and offers to ask again.
3. If Android won't show the prompt any more ("don't ask again"), the screen offers a button to open the app's settings instead.

On Android 11 and older, Nearby may also need location services switched on to find phones. If it reports that, the lobby says so plainly.
