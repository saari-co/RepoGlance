package co.saari.repoglance.hooks

import android.content.Context
import co.saari.repoglance.data.HttpTransport

object TransportFault {
    fun wrap(context: Context, transport: HttpTransport): HttpTransport = transport
}
