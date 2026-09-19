package co.saari.repoglance.link

import android.content.Context
import android.content.Intent
import android.net.Uri

object GitHubAppLauncher {
    const val PACKAGE_NAME = "com.github.android"

    fun intent(url: String, adjacent: Boolean): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(PACKAGE_NAME)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(flags(adjacent))
        }

    fun open(context: Context, url: String, adjacent: Boolean): Boolean {
        val intent = intent(url, adjacent)
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        return true
    }

    internal fun flags(adjacent: Boolean): Int = Intent.FLAG_ACTIVITY_NEW_TASK or
        if (adjacent) Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT else 0
}
