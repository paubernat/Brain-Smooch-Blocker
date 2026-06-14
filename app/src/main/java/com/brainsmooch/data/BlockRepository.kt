package com.brainsmooch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "block_state")

class BlockRepository(private val context: Context) {

    private object Keys {
        val IS_ACTIVE = booleanPreferencesKey("is_active")
        val END_TIME = longPreferencesKey("end_time_millis")
        val BLOCKED_DOMAINS = stringPreferencesKey("blocked_domains")
        val BLOCKED_APPS = stringPreferencesKey("blocked_apps")
        val PANIC_PASSWORD_HASH = stringPreferencesKey("panic_password_hash")
    }

    companion object {
        private const val DOMAIN_SEPARATOR = "\n"

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    val blockState: Flow<BlockState> = context.dataStore.data.map { it.toBlockState() }

    suspend fun startBlock(
        domains: List<String>,
        apps: List<String>,
        durationMillis: Long,
        passwordHash: String? = null
    ) {
        val endTime = System.currentTimeMillis() + durationMillis

        // Guardar en SharedPreferences primero (más robusto)
        GuardState.saveBlockState(context, domains, apps, endTime, passwordHash != null)

        // También guardar en DataStore
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ACTIVE] = true
            prefs[Keys.END_TIME] = endTime
            prefs[Keys.BLOCKED_DOMAINS] = domains.joinToString(DOMAIN_SEPARATOR)
            prefs[Keys.BLOCKED_APPS] = apps.joinToString(DOMAIN_SEPARATOR)
            if (passwordHash != null) {
                prefs[Keys.PANIC_PASSWORD_HASH] = passwordHash
            } else {
                prefs.remove(Keys.PANIC_PASSWORD_HASH)
            }
        }
    }

    suspend fun addAppToActiveBlock(packageName: String): List<String> {
        var updated: List<String> = emptyList()
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_APPS].toDomainList()
            updated = if (packageName in current) current else current + packageName
            prefs[Keys.BLOCKED_APPS] = updated.joinToString(DOMAIN_SEPARATOR)
        }
        return updated
    }

    /** Append-only: domains can be added to a running block but never removed. */
    suspend fun addDomainToActiveBlock(domain: String): List<String> {
        var updated: List<String> = emptyList()
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_DOMAINS].toDomainList()
            updated = if (domain in current) current else current + domain
            prefs[Keys.BLOCKED_DOMAINS] = updated.joinToString(DOMAIN_SEPARATOR)
        }
        return updated
    }

    suspend fun endBlock() {
        // Limpiar SharedPreferences
        GuardState.clearBlockState(context)

        // Limpiar DataStore
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ACTIVE] = false
            prefs[Keys.END_TIME] = 0L
            prefs.remove(Keys.PANIC_PASSWORD_HASH)
        }
    }

    suspend fun getBlockStateOnce(): BlockState {
        // Intentar DataStore primero
        val dsState = context.dataStore.data.first().toBlockState()
        if (dsState.isActive) return dsState

        // Fallback a SharedPreferences si DataStore está vacío
        if (GuardState.isBlockActive(context)) {
            return BlockState(
                isActive = true,
                endTimeMillis = GuardState.getEndTimeMillis(context),
                blockedDomains = GuardState.getBlockedDomains(context).toList(),
                blockedApps = GuardState.getBlockedApps(context).toList(),
                hasPanicPassword = GuardState.hasPassword(context)
            )
        }

        return dsState
    }

    suspend fun getPanicPasswordHash(): String? =
        context.dataStore.data.first()[Keys.PANIC_PASSWORD_HASH]

    private fun Preferences.toBlockState() = BlockState(
        isActive = this[Keys.IS_ACTIVE] ?: false,
        endTimeMillis = this[Keys.END_TIME] ?: 0L,
        blockedDomains = this[Keys.BLOCKED_DOMAINS].toDomainList(),
        blockedApps = this[Keys.BLOCKED_APPS].toDomainList(),
        hasPanicPassword = this[Keys.PANIC_PASSWORD_HASH] != null
    )

    private fun String?.toDomainList(): List<String> =
        this?.split(DOMAIN_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
}
