package co.saari.repoglance.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import co.saari.repoglance.MainActivity
import co.saari.repoglance.R
import co.saari.repoglance.state.LatestPushStore
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RepoGlanceTileService : TileService() {
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        val locked = isSecure
        io.execute {
            val text = TileTexts.of(LatestPushStore.load(this), Instant.now(), locked = locked)
            main.post { render(text) }
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openCatalog) else openCatalog()
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    private fun render(text: TileText) {
        val tile = qsTile ?: return
        tile.label = TileTexts.LABEL
        tile.icon = Icon.createWithResource(this, R.drawable.ic_repoglance_mark)
        tile.subtitle = text.subtitle
        tile.contentDescription = text.contentDescription
        tile.state = if (text.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openCatalog() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
