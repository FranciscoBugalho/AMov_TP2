package pt.isec.amovtp2.geometrygo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection.NetworkConnectivityObject.connectivityReceiverListener

@Suppress("DEPRECATION")
class NetworkConnection : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (connectivityReceiverListener != null) {
                connectivityReceiverListener!!.onNetworkConnectionChanged(verifyNetworkState(context))
            }
        }
    }

    private fun verifyNetworkState(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkCapabilities(network).apply {
                if (this?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                        this?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true)
                    return true
            }
        }
        return false
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    object NetworkConnectivityObject {
        var connectivityReceiverListener: ConnectivityReceiverListener? = null
    }
}