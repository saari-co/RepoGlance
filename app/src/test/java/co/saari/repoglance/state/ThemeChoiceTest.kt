package co.saari.repoglance.state

import android.app.UiModeManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeChoiceTest {

    @Test
    fun theDialogListsLightDarkThenSystemDefault() {
        assertEquals(listOf("Light", "Dark", "System default"), ThemeChoice.entries.map { it.label })
    }

    @Test
    fun eachChoiceMapsToItsPerAppNightMode() {
        assertEquals(UiModeManager.MODE_NIGHT_NO, ThemeChoice.LIGHT.nightMode)
        assertEquals(UiModeManager.MODE_NIGHT_YES, ThemeChoice.DARK.nightMode)
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, ThemeChoice.SYSTEM.nightMode)
    }

    @Test
    fun nothingStoredMeansSystemDefault() {
        assertEquals(ThemeChoice.SYSTEM, ThemePrefs.DEFAULT)
        assertEquals(ThemeChoice.SYSTEM, ThemePrefs.decode(null))
    }

    @Test
    fun anUnreadableValueFallsBackToSystemDefault() {
        for (stored in listOf("", "dark", "NIGHT", "MODE_NIGHT_YES", " LIGHT")) {
            assertEquals("stored=$stored", ThemeChoice.SYSTEM, ThemePrefs.decode(stored))
        }
    }

    @Test
    fun storedNamesReadBackAsTheSameChoice() {
        for (choice in ThemeChoice.entries) {
            assertEquals(choice, ThemePrefs.decode(choice.name))
        }
        assertEquals(listOf("LIGHT", "DARK", "SYSTEM"), ThemeChoice.entries.map { it.name })
    }
}
