package co.saari.repoglance

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionDispatcherCloseTest {
    @Test
    fun aRedrawSuspendedOnTheSessionDispatcherIsLostWhenOnClearedClosesIt() {
        val run = clearSessionThenCloseMidRedraw { redraw -> redraw() }
        assertEquals("the redraw never resumes after the close", listOf("stores cleared", "redraw started"), run.steps)
        assertTrue("the rejected resume cancelled the session clear", run.cancelled)
    }

    @Test
    fun aRedrawOnIoFinishesWhenOnClearedClosesTheSessionDispatcher() {
        val run = clearSessionThenCloseMidRedraw { redraw -> withContext(Dispatchers.IO) { redraw() } }
        assertEquals(
            listOf("stores cleared", "redraw started", "widget state written", "widgets updated"),
            run.steps,
        )
        assertTrue("the closed dispatcher still rejects the final return, after the redraw", run.cancelled)
    }

    private class Run(val steps: List<String>, val cancelled: Boolean)

    private fun clearSessionThenCloseMidRedraw(redrawOn: suspend (suspend () -> Unit) -> Unit): Run {
        val sessionDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val viewModelScope = CoroutineScope(SupervisorJob())
        val steps = CopyOnWriteArrayList<String>()
        val redrawStarted = CountDownLatch(1)
        val glanceWrite = CompletableDeferred<Unit>()
        // Stands in for WidgetRefresh.updateAll: Glance suspends in updateAppWidgetState, then again in update.
        val redraw: suspend () -> Unit = {
            steps += "redraw started"
            redrawStarted.countDown()
            glanceWrite.await()
            steps += "widget state written"
            yield()
            steps += "widgets updated"
        }
        val clear = viewModelScope.launch(sessionDispatcher + NonCancellable) {
            steps += "stores cleared"
            redrawOn(redraw)
        }
        assertTrue(redrawStarted.await(5, TimeUnit.SECONDS))

        viewModelScope.cancel()
        sessionDispatcher.close()
        assertTrue(
            "the session thread is idle, so the redraw is parked inside its suspension",
            (sessionDispatcher.executor as ExecutorService).awaitTermination(5, TimeUnit.SECONDS),
        )
        glanceWrite.complete(Unit)
        runBlocking { withTimeout(5_000) { clear.join() } }
        return Run(steps.toList(), clear.isCancelled)
    }
}
