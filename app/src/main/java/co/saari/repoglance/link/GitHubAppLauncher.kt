package co.saari.repoglance.link

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Explicit GitHub-app routing for navigator detail. Adjacent launch is a
 * best-effort Android request, not a guarantee: the system decides whether
 * the current posture and task state can form a split. Browser fallback is
 * intentionally not chosen in this slice. */
object GitHubAppLauncher {
    const val PACKAGE_NAME = "com.github.android"

    fun open(context: Context, url: String, adjacent: Boolean): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(PACKAGE_NAME)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(flags(adjacent))
        }
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        return true
    }

    internal fun flags(adjacent: Boolean): Int = Intent.FLAG_ACTIVITY_NEW_TASK or
        if (adjacent) Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT else 0
}
