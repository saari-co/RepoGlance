package co.saari.repoglance.state

import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveRefresh
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.model.RateLimitBucket
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateLimitStoreTest {

    private val now = Instant.parse("2026-09-19T21:30:00Z")
    private val reset = now.plusSeconds(1800)

    @Test
    fun unknownObservationsNeverOverwriteAKnownBucket() {
        assertNull(RateLimitStore.stateToRecord(RateLimitSnapshot(RateLimitBucket.UNKNOWN, null, null, null), now))
    }

    @Test
    fun exhaustedWithoutAResetHeaderStillBacksOff() {
        val state = RateLimitStore.stateToRecord(RateLimitSnapshot(RateLimitBucket.EXHAUSTED, 0, 5000, null), now)
        assertEquals(RateLimitState(RateLimitBucket.EXHAUSTED, now.plusSeconds(60)), state)
    }

    @Test
    fun exhaustedLastsUntilTheResetTimeAndThenExpires() {
        val state = RateLimitState(RateLimitBucket.EXHAUSTED, reset)
        assertEquals(RateLimitBucket.EXHAUSTED, RateLimitStore.effectiveBucket(state, now))
        assertEquals(reset, RateLimitStore.exhaustedUntil(state, now))
        assertEquals(RateLimitBucket.UNKNOWN, RateLimitStore.effectiveBucket(state, reset))
        assertNull(RateLimitStore.exhaustedUntil(state, reset))
    }

    @Test
    fun lowExpiresAtTheResetToo() {
        val state = RateLimitState(RateLimitBucket.LOW, reset)
        assertEquals(RateLimitBucket.LOW, RateLimitStore.effectiveBucket(state, now))
        assertEquals(RateLimitBucket.UNKNOWN, RateLimitStore.effectiveBucket(state, reset.plusSeconds(1)))
        assertNull(RateLimitStore.exhaustedUntil(state, now))
    }

    @Test
    fun theLatestKnownBucketAcrossARepositoryRefreshWins() {
        val ok = RateLimitSnapshot(RateLimitBucket.OK, 900, 5000, reset)
        val exhausted = RateLimitSnapshot(RateLimitBucket.EXHAUSTED, 0, 5000, reset)
        val unknown = RateLimitSnapshot(RateLimitBucket.UNKNOWN, null, null, null)
        val results = listOf(
            GitHubApiResult.Success(Unit, now, ok),
            GitHubApiResult.Failure("rate limited", 403, exhausted),
            GitHubApiResult.Failure("offline", null, unknown),
        )
        assertEquals(exhausted, LiveRefresh.latestRateLimit(results))
        assertNull(LiveRefresh.latestRateLimit(listOf(GitHubApiResult.Failure("offline", null, unknown))))
    }
}
