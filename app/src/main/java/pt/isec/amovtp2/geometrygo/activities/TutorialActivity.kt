package pt.isec.amovtp2.geometrygo.activities

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogNoInternetConnection
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection

@Suppress("DEPRECATION")
class TutorialActivity : AppCompatActivity(), NetworkConnection.ConnectivityReceiverListener {

    /**
     * AlertDialog to display that the device lost internet connection.
     */
    private var alertDialogErrors = AlertDialogNoInternetConnection()

    /**
     * BroadcastReceiver to detect the internet connection.
     */
    private val networkConnection = NetworkConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        // TODO: FAZER DEPOIS

    }

    override fun onResume() {
        super.onResume()
        // Register the service
        registerReceiver(networkConnection, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        NetworkConnection.NetworkConnectivityObject.connectivityReceiverListener = this
    }

    override fun onPause() {
        super.onPause()
        // Unregister the service
        unregisterReceiver(networkConnection)
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        alertDialogErrors.presentDialog(alertDialogErrors, isConnected, supportFragmentManager)
    }
}