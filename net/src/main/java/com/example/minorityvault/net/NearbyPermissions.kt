package com.example.minorityvault.net

/**
 * The runtime permissions Nearby Connections needs, per Google's Nearby setup guide (checked October 2026).
 * The manifest in this module declares the same list with matching SDK ranges; docs/permissions.md explains each.
 */
object NearbyPermissions {
    private const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    private const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    private const val BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"
    private const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
    private const val NEARBY_WIFI_DEVICES = "android.permission.NEARBY_WIFI_DEVICES"
    private const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

    private const val ANDROID_10 = 29
    private const val ANDROID_12 = 31
    private const val ANDROID_12L = 32
    private const val ANDROID_13 = 33
    private const val ANDROID_17 = 37

    /** What to ask for on a phone running API level [sdkInt]. */
    fun required(sdkInt: Int): List<String> = buildList {
        // Up to Android 12L, finding Bluetooth and Wi-Fi devices counts as location access.
        if (sdkInt <= ANDROID_12L) add(COARSE_LOCATION)
        if (sdkInt in ANDROID_10..ANDROID_12L) add(FINE_LOCATION)
        if (sdkInt >= ANDROID_12) addAll(listOf(BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, BLUETOOTH_SCAN))
        if (sdkInt >= ANDROID_13) add(NEARBY_WIFI_DEVICES)
        // Apps targeting Android 17 need this for Nearby's Wi-Fi LAN connections.
        if (sdkInt >= ANDROID_17) add(LOCAL_NETWORK)
    }
}
