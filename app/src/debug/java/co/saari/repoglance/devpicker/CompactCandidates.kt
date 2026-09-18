package co.saari.repoglance.devpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.SnapshotRendering
import java.time.Instant

/**
 * GrillTrack round `compact-widget-content-round-2`, active slot
 * `compact-composition`.
 *
 * Round 1 was rejected: all five candidates shared one three-line column and
 * varied only wording, so they read as the same widget. Round 2 varies
 * composition — hierarchy, alignment, and grouping — and shrinks type so three
 * lines fit at the 120x64dp floor.
 *
 * Debug source set only: never enters a release build.
 */

private val REPO = 10.sp
private val BODY = 11.sp
private val MICRO = 8.sp
private val HERO = 30.sp

@Composable
private fun Shell(content: @Composable () -> Unit) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        content()
    }
}

@Composable
private fun RepoLine(s: RepoSnapshot, size: androidx.compose.ui.unit.TextUnit = REPO) = Text(
    s.repo.name,
    maxLines = 1,
    style = TextStyle(
        color = GlanceTheme.colors.onSurfaceVariant,
        fontSize = size,
    ),
)

private fun n(v: Int?, s: RepoSnapshot) = SnapshotRendering.countText(v, s.valueBasis)

/** A — hero number. One number owns the widget; everything else is subordinate. */
@Composable
fun CandidateA(s: RepoSnapshot) = Shell {
    RepoLine(s)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            n(s.prsAwaitingMyReview, s),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = HERO,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            "to\nreview",
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = MICRO),
        )
    }
}

/** B — split columns. Two equal count blocks side by side, each number over its label. */
@Composable
fun CandidateB(s: RepoSnapshot) = Shell {
    RepoLine(s)
    Spacer(modifier = GlanceModifier.height(2.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                n(s.openIssues, s),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                "issues",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = MICRO),
            )
        }
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                n(s.openPrs, s),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                "PRs",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = MICRO),
            )
        }
    }
}

/** C — right-aligned ledger. Labels left, numbers hard right, so digits form a column. */
@Composable
fun CandidateC(s: RepoSnapshot) = Shell {
    RepoLine(s)
    LedgerRow("issues", n(s.openIssues, s))
    LedgerRow("PRs", n(s.openPrs, s))
    LedgerRow("to review", n(s.prsAwaitingMyReview, s))
}

@Composable
private fun LedgerRow(label: String, value: String) = Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        label,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = MICRO),
        modifier = GlanceModifier.defaultWeight(),
    )
    Text(
        value,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = BODY,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/** D — Badge row. Each number is a filled pill, read as a row of tokens. */
@Composable
fun CandidateD(s: RepoSnapshot, now: Instant) = Shell {
    RepoLine(s)
    Spacer(modifier = GlanceModifier.height(3.dp))
    Row {
        Badge("I", n(s.openIssues, s))
        Spacer(modifier = GlanceModifier.width(4.dp))
        Badge("P", n(s.openPrs, s))
        Spacer(modifier = GlanceModifier.width(4.dp))
        Badge("R", n(s.prsAwaitingMyReview, s))
    }
    Spacer(modifier = GlanceModifier.height(3.dp))
    Text(
        Ages.updatedLabel(s.observedAt, now),
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = MICRO),
    )
}

@Composable
private fun Badge(prefix: String, value: String) = Box(
    contentAlignment = Alignment.Center,
    modifier = GlanceModifier
        .background(GlanceTheme.colors.secondaryContainer)
        .cornerRadius(6.dp)
        .padding(horizontal = 5.dp, vertical = 2.dp),
) {
    Text(
        "$prefix $value",
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSecondaryContainer,
            fontSize = MICRO,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/** E — sentence. Numbers inline in prose, repo name last as the quiet attribution. */
@Composable
fun CandidateE(s: RepoSnapshot) = Shell {
    Text(
        "${n(s.prsAwaitingMyReview, s)} need review",
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Text(
        "${n(s.openIssues, s)} issues open",
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = BODY),
    )
    Text(
        "${n(s.openPrs, s)} PRs open",
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = BODY),
    )
    RepoLine(s, MICRO)
}
