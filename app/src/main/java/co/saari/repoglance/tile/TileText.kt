package co.saari.repoglance.tile

import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.render.Ages
import co.saari.repoglance.state.LatestPushRecord
import java.time.Duration
import java.time.Instant

data class TileText(val subtitle: String, val active: Boolean, val contentDescription: String)

object TileTexts {
    const val LABEL = "RepoGlance"
    private const val CONNECT = "Open to connect"
    private const val LOCKED = "Unlock to see the latest push"
    private const val REFRESH = "open to refresh"
    private val STALE_AFTER: Duration = Duration.ofHours(24)

    fun of(record: LatestPushRecord?, now: Instant, locked: Boolean = false): TileText {
        if (locked) {
            return TileText(LOCKED, active = record != null, contentDescription = "$LABEL, $LOCKED")
        }
        if (record == null) {
            return TileText(CONNECT, active = false, contentDescription = "$LABEL, $CONNECT")
        }
        val repo = Sanitize.displayText(record.repoFull)
        val stale = Duration.between(record.observedAt, now) > STALE_AFTER
        val subtitle = if (stale) "$repo · $REFRESH" else "$repo · ${Ages.format(record.pushedAt, now)}"
        val description = if (stale) {
            "$LABEL, latest push to $repo, seen over a day ago, open to refresh"
        } else {
            "$LABEL, latest push to $repo ${Ages.updatedLabel(record.pushedAt, now).lowercase()}"
        }
        return TileText(subtitle, active = true, contentDescription = description)
    }
}
