package pt.isec.amovtp2.geometrygo.activities

import android.content.IntentFilter
import android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogNoInternetConnection
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection.NetworkConnectivityObject.connectivityReceiverListener

@Suppress("DEPRECATION")
class AboutActivity : AppCompatActivity(), NetworkConnection.ConnectivityReceiverListener {

    // AlertDialog to display that the device lost internet connection.
    private var alertDialogErrors = AlertDialogNoInternetConnection()

    // BroadcastReceiver to detect the internet connection.
    private val networkConnection = NetworkConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Justify the text in the versions 8.0+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            findViewById<TextView>(R.id.tvAbout).justificationMode = JUSTIFICATION_MODE_INTER_WORD
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the service.
        registerReceiver(networkConnection, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        connectivityReceiverListener = this
    }

    override fun onPause() {
        super.onPause()
        // Unregister the service.
        unregisterReceiver(networkConnection)
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        alertDialogErrors.presentDialog(alertDialogErrors, isConnected, supportFragmentManager)
    }
}