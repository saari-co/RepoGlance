package co.saari.repoglance.widget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import org.junit.Test

/**
 * JVM assertions on the compact widget's ledger row — no device, no emulator,
 * no UI Automator. Proves the composition emits the label and the value as
 * separate, individually addressable elements.
 */
class LedgerRowUnitTest {

    @Test
    fun ledgerRowEmitsItsLabelAndValue() = runGlanceAppWidgetUnitTest {
        provideComposable { LedgerRow("issues", "128") }

        onNode(hasTestTag(LEDGER_LABEL_TAG)).assertHasTextEqualTo("issues")
        onNode(hasTestTag(LEDGER_VALUE_TAG)).assertHasTextEqualTo("128")
    }

    @Test
    fun anUnknownCountRendersAsAnEmDashRatherThanZero() = runGlanceAppWidgetUnitTest {
        provideComposable { LedgerRow("PRs", "—") }

        onNode(hasTestTag(LEDGER_VALUE_TAG)).assertHasTextEqualTo("—")
        onAllNodes(hasText("0")).assertCountEquals(0)
    }
}
