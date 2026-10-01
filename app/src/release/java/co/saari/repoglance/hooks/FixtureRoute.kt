package co.saari.repoglance.hooks

import android.content.Intent
import androidx.compose.runtime.Composable

class FixtureRoute {
    val isOpen: Boolean get() = false

    fun onCreate(intent: Intent?) = Unit

    fun onNewIntent(intent: Intent) = Unit

    fun close() = Unit

    @Composable
    fun Content() = Unit
}
