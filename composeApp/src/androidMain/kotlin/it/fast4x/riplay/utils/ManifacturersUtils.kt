package it.fast4x.riplay.utils

import android.os.Build

fun manufacturersWithAutostart(): Set<String> {
// List of vendors with known restrictions on autostart
    return setOf("xiaomi", "huawei", "oppo", "vivo", "oneplus", "samsung", "asus")
}

fun isManufacturerWithAutostart(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return manufacturer.lowercase() in manufacturersWithAutostart()
}
