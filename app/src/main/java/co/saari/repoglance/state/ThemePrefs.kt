package co.saari.repoglance.state

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class ThemeChoice(val label: String, val nightMode: Int) {
    LIGHT("Light", UiModeManager.MODE_NIGHT_NO),
    DARK("Dark", UiModeManager.MODE_NIGHT_YES),
    SYSTEM("System default", UiModeManager.MODE_NIGHT_AUTO),
}

object ThemePrefs {
    internal const val PREFS_NAME = "repoglance_theme"
    private const val KEY_CHOICE = "choice"
    val DEFAULT = ThemeChoice.SYSTEM

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun decode(stored: String?): ThemeChoice =
        ThemeChoice.entries.firstOrNull { it.name == stored } ?: DEFAULT

    fun choice(context: Context): ThemeChoice = decode(prefs(context).getString(KEY_CHOICE, null))

    fun choose(context: Context, choice: ThemeChoice) {
        prefs(context).edit { putString(KEY_CHOICE, choice.name) }
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(choice.nightMode)
    }
}
