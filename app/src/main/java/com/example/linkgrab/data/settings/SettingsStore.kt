package com.example.linkgrab.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.linkgrab.domain.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore("linkgrab_settings")
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val quality: Int = 0,
    val kind: MediaKind = MediaKind.VIDEO,
    val audioFormat: ContainerFormat = ContainerFormat.M4A,
    val clipboard: Boolean = false,
    val autoUpdate: Boolean = false,
    val notifications: Boolean = true,
    val onboarded: Boolean = false,
    val lastEngineUpdate: Long = 0,
    val legacyMigrated: Boolean = false,
    val notificationAsked: Boolean = false
)
class SettingsStore(context: Context) {
    private val store = context.settingsDataStore
    val flow = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        AppSettings(
            enumValue(p[stringPreferencesKey("theme")], ThemeMode.SYSTEM), p[intPreferencesKey("quality")] ?: 0,
            enumValue(p[stringPreferencesKey("kind")], MediaKind.VIDEO),
            enumValue(p[stringPreferencesKey("audioFormat")], ContainerFormat.M4A),
            p[booleanPreferencesKey("clipboard")] ?: false, p[booleanPreferencesKey("autoUpdate")] ?: false,
            p[booleanPreferencesKey("notifications")] ?: true, p[booleanPreferencesKey("onboarded")] ?: false,
            p[longPreferencesKey("lastEngineUpdate")] ?: 0, p[booleanPreferencesKey("legacyMigrated")] ?: false,
            p[booleanPreferencesKey("notificationAsked")] ?: false
        )
    }
    suspend fun text(key: String, value: String) { store.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun flag(key: String, value: Boolean) { store.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun quality(value: Int) { store.edit { it[intPreferencesKey("quality")] = value } }
    suspend fun engineUpdated() { store.edit { it[longPreferencesKey("lastEngineUpdate")] = System.currentTimeMillis() } }
    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}
