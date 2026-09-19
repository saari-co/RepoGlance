package co.saari.repoglance.state

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogNamesStoreTest {

    @Test
    fun catalogPushTimesRoundTrip() {
        val times = mapOf("saari-co/RepoGlance" to Instant.parse("2026-09-19T03:50:01Z"), "a/b" to Instant.EPOCH)
        assertEquals(times, CatalogNamesStore.decodePushedAt(CatalogNamesStore.encodePushedAt(times)))
    }

    @Test
    fun missingOrMalformedPushTimesDecodeToNothingRatherThanGuesses() {
        assertEquals(emptyMap<String, Instant>(), CatalogNamesStore.decodePushedAt(null))
        assertEquals(emptyMap<String, Instant>(), CatalogNamesStore.decodePushedAt("not json"))
        assertEquals(mapOf("a/ok" to Instant.EPOCH), CatalogNamesStore.decodePushedAt("""{"a/ok":"1970-01-01T00:00:00Z","a/bad":"yesterday"}"""))
    }
}
