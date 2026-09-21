package co.saari.repoglance.hooks

import android.content.Context
import co.saari.repoglance.data.HttpRequest
import co.saari.repoglance.data.HttpResponse
import co.saari.repoglance.data.HttpTransport
import java.time.Instant

// Debug-only rate-limit fault for the device proof (GrillTrack
// rate-limit-device-proof-026). Armed through ScenarioLaunchActivity
// (--es rateFault LOW|EXHAUSTED|OFF --el rateFaultResetSeconds N), it is
// persisted so it survives process death and expires on its own at its
// reset time. LOW makes the real GitHub call and rewrites only the
// rate-limit headers; EXHAUSTED answers with a synthetic 403 and never
// reaches GitHub. It records, in its own debug prefs, only how many
// responses it served per kind with their last status and time, never a URL,
// request header, or response body. The release flavour returns the
// transport unchanged.
object TransportFault {
    enum class Kind { LOW, EXHAUSTED }

    data class Armed(val kind: Kind, val resetsAt: Instant)

    private const val PREFS_NAME = "repoglance_debug_fault"
    private const val KEY_KIND = "kind"
    private const val KEY_RESETS_AT = "resetsAt"
    internal const val LIMIT = 5000
    internal const val LOW_REMAINING = LIMIT / 20
    internal const val EXHAUSTED_BODY =
        "{\"message\":\"API rate limit exceeded (RepoGlance debug fault)\"}"

    @Volatile
    private var cached: Armed? = null

    @Volatile
    private var loaded = false

    fun arm(context: Context, kind: Kind?, resetSeconds: Long, now: Instant = Instant.now()) {
        val armed = kind?.let { Armed(it, now.plusSeconds(resetSeconds)) }
        val editor = prefs(context).edit()
        if (armed == null) {
            editor.clear()
        } else {
            editor.putString(KEY_KIND, armed.kind.name).putString(KEY_RESETS_AT, armed.resetsAt.toString())
        }
        editor.apply()
        cached = armed
        loaded = true
    }

    fun wrap(context: Context, transport: HttpTransport): HttpTransport {
        val appContext = context.applicationContext
        return HttpTransport { request ->
            val armed = active(load(appContext), Instant.now())
            respond(armed, request, transport).also { response ->
                armed?.let { record(appContext, it.kind, response.statusCode) }
            }
        }
    }

    internal fun active(armed: Armed?, now: Instant): Armed? = armed?.takeIf { now.isBefore(it.resetsAt) }

    internal fun respond(armed: Armed?, request: HttpRequest, delegate: HttpTransport): HttpResponse =
        when (armed?.kind) {
            null -> delegate.execute(request)
            Kind.EXHAUSTED -> HttpResponse(
                statusCode = 403,
                headers = rateHeaders(remaining = 0, resetsAt = armed.resetsAt) +
                    mapOf("Content-Type" to listOf("application/json")),
                body = EXHAUSTED_BODY,
            )
            Kind.LOW -> delegate.execute(request).let { real ->
                HttpResponse(
                    statusCode = real.statusCode,
                    headers = real.headers.filterKeys { !isRateHeader(it) } +
                        rateHeaders(remaining = LOW_REMAINING, resetsAt = armed.resetsAt),
                    body = real.body,
                )
            }
        }

    private fun record(context: Context, kind: Kind, status: Int) {
        val prefs = prefs(context)
        val served = "served.${kind.name}"
        prefs.edit()
            .putInt(served, prefs.getInt(served, 0) + 1)
            .putString("$served.lastStatus", status.toString())
            .putString("$served.lastAt", Instant.now().toString())
            .apply()
    }

    private fun isRateHeader(name: String?): Boolean = name?.startsWith("X-RateLimit-", ignoreCase = true) == true

    private fun rateHeaders(remaining: Int, resetsAt: Instant): Map<String, List<String>> = mapOf(
        "X-RateLimit-Limit" to listOf(LIMIT.toString()),
        "X-RateLimit-Remaining" to listOf(remaining.toString()),
        "X-RateLimit-Used" to listOf((LIMIT - remaining).toString()),
        "X-RateLimit-Reset" to listOf(resetsAt.epochSecond.toString()),
    )

    private fun load(context: Context): Armed? {
        if (loaded) return cached
        val prefs = prefs(context)
        val kind = prefs.getString(KEY_KIND, null)?.let { name -> Kind.entries.firstOrNull { it.name == name } }
        val resetsAt = prefs.getString(KEY_RESETS_AT, null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        cached = if (kind != null && resetsAt != null) Armed(kind, resetsAt) else null
        loaded = true
        return cached
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
