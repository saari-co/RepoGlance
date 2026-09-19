package co.saari.repoglance.refresh

import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPlanTest {

    private val now = Instant.parse("2026-09-19T21:30:00Z")

    private fun names(refs: List<RepoRef>) = refs.map { it.full }

    @Test
    fun neverRefreshedPinsGoFirstThenOldestObservation() {
        val observed = mapOf("a/old" to now.minusSeconds(7200), "a/new" to now.minusSeconds(60))
        val ordered = RefreshPlan.order(listOf("a/new", "a/never", "a/old", "not a repo")) { observed[it.full] }
        assertEquals(listOf("a/never", "a/old", "a/new"), names(ordered))
    }

    @Test
    fun aRunStopsAfterTwentyRepositories() {
        val pins = (1..25).map { RepoRef("o", "r$it") }
        val done = RefreshPlan.execute(pins, emptySet(), RateLimitBucket.OK) { RefreshStep(RateLimitBucket.OK, stop = false) }
        assertEquals(RefreshPlan.MAX_PER_RUN, done.size)
        assertEquals(pins.take(20), done)
    }

    @Test
    fun lowBudgetRefreshesOnlyRepositoriesWithAWidget() {
        val pins = listOf(RepoRef("o", "a"), RepoRef("o", "b"), RepoRef("o", "c"))
        val done = RefreshPlan.execute(pins, setOf("o/c"), RateLimitBucket.LOW) { RefreshStep(RateLimitBucket.LOW, stop = false) }
        assertEquals(listOf("o/c"), names(done))
    }

    @Test
    fun budgetTurningLowMidRunSkipsTheRemainingWidgetlessPins() {
        val pins = listOf(RepoRef("o", "a"), RepoRef("o", "b"), RepoRef("o", "c"))
        val done = RefreshPlan.execute(pins, setOf("o/c"), RateLimitBucket.OK) { RefreshStep(RateLimitBucket.LOW, stop = false) }
        assertEquals(listOf("o/a", "o/c"), names(done))
    }

    @Test
    fun exhaustedBudgetStopsTheRunAndStartsNoRequest() {
        val pins = listOf(RepoRef("o", "a"), RepoRef("o", "b"))
        var calls = 0
        val none = RefreshPlan.execute(pins, setOf("o/a"), RateLimitBucket.EXHAUSTED) {
            calls += 1
            RefreshStep(RateLimitBucket.OK, stop = false)
        }
        assertEquals(emptyList<RepoRef>(), none)
        assertEquals(0, calls)
        val first = RefreshPlan.execute(pins, emptySet(), RateLimitBucket.OK) { RefreshStep(RateLimitBucket.EXHAUSTED, stop = true) }
        assertEquals(listOf("o/a"), names(first))
    }

    @Test
    fun anUnknownBucketKeepsTheLastKnownOne() {
        val pins = listOf(RepoRef("o", "a"), RepoRef("o", "b"), RepoRef("o", "c"))
        val steps = ArrayDeque(listOf(RefreshStep(RateLimitBucket.LOW, false), RefreshStep(RateLimitBucket.UNKNOWN, false)))
        val done = RefreshPlan.execute(pins, setOf("o/a", "o/c"), RateLimitBucket.OK) { steps.removeFirstOrNull() ?: RefreshStep(RateLimitBucket.UNKNOWN, false) }
        assertEquals(listOf("o/a", "o/c"), names(done))
    }
}
