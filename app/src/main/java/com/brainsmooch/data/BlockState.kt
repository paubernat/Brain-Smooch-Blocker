package com.brainsmooch.data

data class BlockState(
    val isActive: Boolean = false,
    val endTimeMillis: Long = 0L,
    val blockedDomains: List<String> = emptyList(),
    val blockedApps: List<String> = emptyList(),
    val hasPanicPassword: Boolean = false
) {
    val remainingMillis: Long
        get() = if (isActive) maxOf(0, endTimeMillis - System.currentTimeMillis()) else 0

    val isExpired: Boolean
        get() = isActive && System.currentTimeMillis() >= endTimeMillis
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean = true
)
