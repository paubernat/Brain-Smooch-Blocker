package com.brainsmooch.data

import android.content.Context

/**
 * Lightweight, synchronously-readable mirror of "is a block currently active".
 *
 * The DataStore in [BlockRepository] is the source of truth for the UI, but the
 * AccessibilityService needs to know the block state instantly on every window
 * change without launching coroutines. This SharedPreferences flag is written
 * whenever a block starts or ends.
 */
object GuardState {
    private const val PREFS = "guard_state"
    private const val KEY_ACTIVE = "block_active"
    private const val KEY_HARDCORE = "hardcore_mode"
    private const val KEY_BLOCKED_APPS = "blocked_apps"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBlockActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, active).apply()
    }

    fun isBlockActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVE, false)

    fun setHardcore(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HARDCORE, enabled).apply()
    }

    fun isHardcore(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HARDCORE, false)

    fun setBlockedApps(context: Context, apps: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, apps).apply()
    }

    fun getBlockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

    fun clearBlockedApps(context: Context) {
        prefs(context).edit().remove(KEY_BLOCKED_APPS).apply()
    }
}
