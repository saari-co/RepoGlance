package co.saari.repoglance.auth

import co.saari.repoglance.data.HttpRequest
import co.saari.repoglance.data.HttpResponse
import co.saari.repoglance.data.HttpTransport
import java.io.IOException
import java.net.URLDecoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubDeviceFlowClientTest {
    private val now = Instant.parse("2026-08-12T17:00:00Z")
    private val clock = MutableClock(now)

    @Test
    fun beginUsesOnlyPublicClientIdAndParsesGitHubVerificationData() {
        val transport = RecordingTransport(
            HttpResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = """{
                  "device_code":"device-value",
                  "user_code":"ABCD-EFGH",
                  "verification_uri":"https://github.com/login/device",
                  "expires_in":900,
                  "interval":5
                }""",
            ),
        )

        val authorization = GitHubDeviceFlowClient(config(), transport, clock).begin()

        assertEquals("ABCD-EFGH", authorization.userCode)
        assertEquals("https://github.com/login/device", authorization.verificationUri)
        assertEquals(now.plusSeconds(900), authorization.expiresAt)
        assertEquals(5L, authorization.intervalSeconds)
        assertFalse(authorization.toString().contains("device-value"))
        assertFalse(authorization.toString().contains("ABCD-EFGH"))
        assertEquals(GitHubDeviceFlowClient.DEVICE_CODE_ENDPOINT, transport.singleRequest().url)
        assertEquals(mapOf("client_id" to "public-client-id"), transport.singleRequest().formBody())
    }

    @Test
    fun pollModelsPendingSlowDownAndExpiringToken() {
        val transport = RecordingTransport(
            response("""{"error":"authorization_pending"}"""),
            response("""{"error":"slow_down","interval":12}"""),
            response(
                """{
                  "access_token":"access-value",
                  "expires_in":28800,
                  "refresh_token":"refresh-value",
                  "refresh_token_expires_in":15897600,
                  "token_type":"bearer"
                }""",
            ),
        )
        val client = GitHubDeviceFlowClient(config(), transport, clock)

        assertEquals(DevicePollResult.Pending, client.poll("device-value"))
        assertEquals(DevicePollResult.SlowDown(12L), client.poll("device-value"))
        val authorized = client.poll("device-value") as DevicePollResult.Authorized

        assertEquals("access-value", authorized.token.accessToken)
        assertEquals("refresh-value", authorized.token.refreshToken)
        assertEquals(now.plusSeconds(28800), authorized.token.accessTokenExpiresAt)
        assertEquals(now.plusSeconds(15897600), authorized.token.refreshTokenExpiresAt)
        transport.requests.forEach { request ->
            assertEquals(
                mapOf(
                    "client_id" to "public-client-id",
                    "device_code" to "device-value",
                    "grant_type" to GitHubDeviceFlowClient.DEVICE_GRANT_TYPE,
                ),
                request.formBody(),
            )
        }
    }

    @Test
    fun pollMapsOnlyTransportIoToASanitizedTransientFailure() {
        val client = GitHubDeviceFlowClient(config(), HttpTransport { throw IOException("sensitive detail") }, clock)

        val failure = assertThrows(TransientDevicePollException::class.java) {
            client.poll("device-value")
        }

        assertNull(failure.message)
        assertNull(failure.cause)
    }

    @Test
    fun refreshRotatesDeviceFlowTokenWithNoConfidentialParameter() {
        val transport = RecordingTransport(
            response(
                """{
                  "access_token":"new-access",
                  "expires_in":28800,
                  "refresh_token":"new-refresh",
                  "refresh_token_expires_in":15897600,
                  "token_type":"bearer"
                }""",
            ),
        )

        val token = GitHubDeviceFlowClient(config(), transport, clock).refresh("old-refresh")

        assertEquals("new-access", token.accessToken)
        assertEquals("new-refresh", token.refreshToken)
        assertEquals(
            mapOf(
                "client_id" to "public-client-id",
                "grant_type" to "refresh_token",
                "refresh_token" to "old-refresh",
            ),
            transport.singleRequest().formBody(),
        )
    }

    @Test
    fun deviceFlowErrorsUseTrustedMessagesOnly() {
        val transport = RecordingTransport(
            response(
                """{
                  "error":"device_flow_disabled",
                  "error_description":"untrusted server detail"
                }""",
            ),
        )

        val failure = assertThrows(GitHubAuthException::class.java) {
            GitHubDeviceFlowClient(config(), transport, clock).poll("device-value")
        }

        assertTrue(failure.needsNewSignIn)
        assertTrue(failure.message.orEmpty().contains("Device Flow"))
        assertFalse(failure.message.orEmpty().contains("untrusted"))
    }

    @Test
    fun beginRejectsVerificationDestinationsOutsideExactGitHubDevicePage() {
        val transport = RecordingTransport(
            response(
                """{
                  "device_code":"device-value",
                  "user_code":"ABCD-EFGH",
                  "verification_uri":"https://example.com/login/device",
                  "expires_in":900,
                  "interval":5
                }""",
            ),
        )

        assertThrows(GitHubAuthException::class.java) {
            GitHubDeviceFlowClient(config(), transport, clock).begin()
        }
    }

    @Test
    fun pollerHonorsPendingAndServerSlowDownIntervals() = runBlocking {
        val gateway = QueueGateway(
            DevicePollResult.Pending,
            DevicePollResult.SlowDown(12L),
            DevicePollResult.Authorized(nonExpiringToken("accepted")),
        )
        val waits = mutableListOf<Long>()
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis ->
            waits += millis
            clock.advanceMillis(millis)
        }

        val token = poller.awaitToken(authorization)

        assertEquals("accepted", token.accessToken)
        assertEquals(listOf(5_000L, 5_000L, 12_000L), waits)
        assertEquals(3, gateway.pollCount)
    }

    @Test
    fun pollerRetriesTransientTransportFailureAtTheRequiredInterval() = runBlocking {
        val gateway = QueueGateway(
            TransientDevicePollException(),
            DevicePollResult.Authorized(nonExpiringToken("accepted")),
        )
        val waits = mutableListOf<Long>()
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis ->
            waits += millis
            clock.advanceMillis(millis)
        }

        val token = poller.awaitToken(authorization)

        assertEquals("accepted", token.accessToken)
        assertEquals(listOf(5_000L, 5_000L), waits)
        assertEquals(2, gateway.pollCount)
    }

    @Test
    fun pollerStopsRetryingTransientTransportFailureAtLocalExpiry() = runBlocking {
        val gateway = AlwaysTransientGateway()
        val waits = mutableListOf<Long>()
        val authorization = authorization(expiresAt = now.plusSeconds(10), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis ->
            waits += millis
            clock.advanceMillis(millis)
        }

        val failure = assertThrows(GitHubAuthException::class.java) {
            runBlocking { poller.awaitToken(authorization) }
        }

        assertTrue(failure.needsNewSignIn)
        assertEquals(listOf(5_000L, 5_000L), waits)
        assertEquals(1, gateway.pollCount)
    }

    @Test
    fun resumeWakeCannotPollBeforeGitHubsMinimumInterval() = runBlocking {
        val gateway = QueueGateway(DevicePollResult.Authorized(nonExpiringToken("accepted")))
        val waits = mutableListOf<Long>()
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis ->
            waits += millis
            if (waits.size > 1) clock.advanceMillis(millis)
        }

        val token = poller.awaitToken(authorization)

        assertEquals("accepted", token.accessToken)
        assertEquals(listOf(5_000L, 5_000L), waits)
        assertEquals(1, gateway.pollCount)
    }

    @Test
    fun resumeWakeCannotBypassASlowDownDeadline() = runBlocking {
        val gateway = QueueGateway(
            DevicePollResult.SlowDown(null),
            DevicePollResult.Authorized(nonExpiringToken("accepted")),
        )
        val waits = mutableListOf<Long>()
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis ->
            waits += millis
            if (waits.size != 2) clock.advanceMillis(millis)
        }

        val token = poller.awaitToken(authorization)

        assertEquals("accepted", token.accessToken)
        assertEquals(listOf(5_000L, 10_000L, 10_000L), waits)
        assertEquals(2, gateway.pollCount)
    }

    @Test
    fun activityResumeSignalWakesAParkedPollWait() = runBlocking {
        val signal = DeviceFlowPollWakeSignal()
        val waitCompleted = async {
            signal.await(60_000L)
            true
        }
        yield()

        signal.wake()

        assertTrue(withTimeout(1_000L) { waitCompleted.await() })
    }

    @Test
    fun resumeAfterTheDeadlineReachesTheAuthorizedResultImmediately() = runBlocking {
        val signal = DeviceFlowPollWakeSignal()
        val gateway = QueueGateway(DevicePollResult.Authorized(nonExpiringToken("accepted")))
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock, signal::await)
        val token = async { poller.awaitToken(authorization) }
        yield()

        clock.advanceMillis(5_000L)
        signal.wake()

        assertEquals("accepted", withTimeout(1_000L) { token.await() }.accessToken)
        assertEquals(1, gateway.pollCount)
    }

    @Test
    fun pollerStopsAtLocalExpiryWithoutOneLateRequest() = runBlocking {
        val gateway = QueueGateway(DevicePollResult.Pending)
        val authorization = authorization(expiresAt = now.plusSeconds(5), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { millis -> clock.advanceMillis(millis) }

        val failure = assertThrows(GitHubAuthException::class.java) {
            runBlocking { poller.awaitToken(authorization) }
        }

        assertTrue(failure.needsNewSignIn)
        assertEquals(0, gateway.pollCount)
    }

    @Test
    fun pollerPropagatesCancellationBeforePolling() {
        val gateway = QueueGateway(DevicePollResult.Pending)
        val authorization = authorization(expiresAt = now.plusSeconds(60), intervalSeconds = 5)
        val poller = GitHubDeviceFlowPoller(gateway, clock) { throw CancellationException("cancelled") }

        assertThrows(CancellationException::class.java) {
            runBlocking { poller.awaitToken(authorization) }
        }
        assertEquals(0, gateway.pollCount)
    }

    @Test
    fun latePollResultCannotCommitAfterCancellation() {
        val gate = AuthorizationCommitGate()
        val generation = gate.nextGeneration()
        var persisted: String? = null

        gate.invalidate { persisted = null }
        val committed = gate.commit(generation) { persisted = "late-token" }

        assertFalse(committed)
        assertNull(persisted)
    }

    @Test
    fun cancellationClearsACommitAlreadyInProgress() {
        val gate = AuthorizationCommitGate()
        val generation = gate.nextGeneration()
        val commitEntered = CountDownLatch(1)
        val releaseCommit = CountDownLatch(1)
        var persisted: String? = null
        val commitThread = Thread {
            gate.commit(generation) {
                commitEntered.countDown()
                releaseCommit.await()
                persisted = "accepted-token"
            }
        }
        val cancelThread = Thread {
            check(commitEntered.await(5, TimeUnit.SECONDS))
            gate.invalidate { persisted = null }
        }

        commitThread.start()
        cancelThread.start()
        assertTrue(commitEntered.await(5, TimeUnit.SECONDS))
        releaseCommit.countDown()
        commitThread.join(5_000L)
        cancelThread.join(5_000L)

        assertFalse(commitThread.isAlive)
        assertFalse(cancelThread.isAlive)
        assertNull(persisted)
    }

    private fun authorization(expiresAt: Instant, intervalSeconds: Long) = GitHubDeviceAuthorization(
        deviceCode = "device-value",
        userCode = "ABCD-EFGH",
        verificationUri = "https://github.com/login/device",
        expiresAt = expiresAt,
        intervalSeconds = intervalSeconds,
    )

    private fun config() = GitHubAuthConfig(clientId = "public-client-id")

    private fun response(body: String) = HttpResponse(statusCode = 200, headers = emptyMap(), body = body)

    private fun nonExpiringToken(value: String) = GitHubUserToken(value, null, null, null, "bearer")

    private class RecordingTransport(vararg responses: HttpResponse) : HttpTransport {
        private val queued = ArrayDeque(responses.toList())
        val requests = mutableListOf<HttpRequest>()

        override fun execute(request: HttpRequest): HttpResponse {
            requests += request
            return queued.removeFirst()
        }

        fun singleRequest(): HttpRequest = requests.single()
    }

    private class QueueGateway(vararg results: Any) : GitHubDeviceAuthorizationGateway {
        private val queued = ArrayDeque(results.toList())
        var pollCount = 0

        override fun poll(deviceCode: String): DevicePollResult {
            pollCount += 1
            return when (val result = queued.removeFirst()) {
                is TransientDevicePollException -> throw result
                is DevicePollResult -> result
                else -> error("Unsupported queued result")
            }
        }
    }

    private class AlwaysTransientGateway : GitHubDeviceAuthorizationGateway {
        var pollCount = 0

        override fun poll(deviceCode: String): DevicePollResult {
            pollCount += 1
            throw TransientDevicePollException()
        }
    }

    private class MutableClock(private var current: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
        override fun instant(): Instant = current
        fun advanceMillis(millis: Long) {
            current = current.plusMillis(millis)
        }
    }

    private fun HttpRequest.formBody(): Map<String, String> = body
        ?.toString(Charsets.UTF_8)
        .orEmpty()
        .split('&')
        .filter(String::isNotBlank)
        .associate { pair ->
            val (name, value) = pair.split('=', limit = 2)
            URLDecoder.decode(name, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
        }
}
