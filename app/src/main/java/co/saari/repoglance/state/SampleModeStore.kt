package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit

object SampleModeStore {
    internal const val PREFS_NAME = "repoglance_sample"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PINS = "pins"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun enter(context: Context) {
        prefs(context).edit { putBoolean(KEY_ACTIVE, true) }
    }

    fun leave(context: Context) {
        prefs(context).edit { clear() }
    }

    fun pins(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PINS, emptySet()).orEmpty().toSet()

    fun togglePin(context: Context, repoFull: String) {
        val current = pins(context)
        val next = if (repoFull in current) current - repoFull else current + repoFull
        prefs(context).edit { putStringSet(KEY_PINS, next) }
    }

    @Composable
    fun rememberPins(context: Context): State<Set<String>> {
        val state = remember { mutableStateOf(pins(context)) }
        DisposableEffect(context) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == KEY_PINS || changedKey == null) state.value = pins(context)
            }
            val p = prefs(context)
            p.registerOnSharedPreferenceChangeListener(listener)
            onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return state
    }
}
