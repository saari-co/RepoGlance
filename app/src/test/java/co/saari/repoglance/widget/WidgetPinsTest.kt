package co.saari.repoglance.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPinsTest {
    @Test
    fun configurationListPutsPinsFirstThenTheCatalogOrderAndKeepsUnknownPins() {
        val names = listOf("a/newest", "b/second", "c/third")
        val list = WidgetPins.configurationList(names, setOf("c/third", "z/pinned-elsewhere")).map { it.full }
        assertEquals(listOf("z/pinned-elsewhere", "c/third", "a/newest", "b/second"), list)
    }

    @Test
    fun configurationListDropsMalformedNames() {
        assertEquals(emptyList<String>(), WidgetPins.configurationList(listOf("not-a-repo"), emptySet()).map { it.full })
    }

    @Test
    fun releasedRepositoriesAreThoseNoOtherWidgetStillUses() {
        assertEquals(setOf("a/b"), WidgetPins.releasedRepositories(listOf("a/b", "c/d"), listOf("c/d")))
        assertEquals(emptySet<String>(), WidgetPins.releasedRepositories(emptyList(), listOf("c/d")))
    }
}
