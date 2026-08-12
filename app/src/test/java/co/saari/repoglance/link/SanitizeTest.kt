package co.saari.repoglance.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// All hostile inputs below are built at runtime by string concatenation
// only — no token-shaped or bearer-shaped string literal appears in this
// file. The redaction bullet character is "•" (BULLET), used three
// times to match Sanitize's "•••" marker.
private const val BULLET3 = "•••"

class SanitizeTest {

    @Test
    fun redactsClassicPersonalAccessTokenPrefixes() {
        for (letter in listOf("p", "o", "u", "s", "r")) {
            val tail = "a".repeat(30)
            val hostile = "gh" + letter + "_" + tail
            val result = Sanitize.displayText(hostile)
            assertTrue("expected redaction for gh${letter}_ prefix", result.contains(BULLET3))
            assertFalse(result.contains(tail))
        }
    }

    @Test
    fun redactsFineGrainedPersonalAccessToken() {
        val tail = "B".repeat(10) + "_" + "c".repeat(15)
        val hostile = "github" + "_pat_" + tail
        val result = Sanitize.displayText(hostile)
        assertTrue(result.contains(BULLET3))
        assertFalse(result.contains(tail))
    }

    @Test
    fun redactsAuthorizationHeaderShape() {
        val tail = "tok" + "en".repeat(10)
        val hostile = "Autho" + "rization: " + tail
        val result = Sanitize.displayText(hostile)
        assertTrue(result.contains(BULLET3))
        assertFalse(result.contains(tail))
    }

    @Test
    fun redactsFullAuthorizationBearerHeaderShape() {
        val tail = "x".repeat(24)
        val hostile = "Autho" + "rization: Bear" + "er " + tail
        val result = Sanitize.displayText(hostile)
        assertEquals(BULLET3, result)
        assertFalse(result.contains(tail))
    }

    @Test
    fun redactsCredentialsAfterNonBearerAuthorizationSchemes() {
        for (scheme in listOf("Basic", "Token")) {
            val tail = "z".repeat(24)
            val hostile = "Autho" + "rization: " + scheme + " " + tail
            val result = Sanitize.displayText(hostile)
            assertEquals(BULLET3, result)
            assertFalse(result.contains(tail))
        }
    }

    @Test
    fun redactsBearerShape() {
        val tail = "x".repeat(20)
        val hostile = "Bear" + "er " + tail
        val result = Sanitize.displayText(hostile)
        assertTrue(result.contains(BULLET3))
        assertFalse(result.contains(tail))
    }

    @Test
    fun stripsControlCharacters() {
        val bell = 7.toChar().toString()
        val hostile = "hello" + bell + "world"
        val result = Sanitize.displayText(hostile)
        assertFalse(result.contains(bell))
    }

    @Test
    fun cleanProsePassesThroughUnchanged() {
        val clean = "Fix flaky retry in sync worker"
        assertEquals(clean, Sanitize.displayText(clean))
        assertTrue(Sanitize.isClean(clean))
    }

    @Test
    fun hostileInputIsNeverClean() {
        val hostile = "ghp_" + "z".repeat(25)
        assertFalse(Sanitize.isClean(hostile))
    }
}
