package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogNoInternetConnection
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection
import pt.isec.amovtp2.geometrygo.receivers.NetworkConnection.NetworkConnectivityObject.connectivityReceiverListener

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), NetworkConnection.ConnectivityReceiverListener {

    // AlertDialog to display that the device lost internet connection.
    private var alertDialogErrors = AlertDialogNoInternetConnection()

    // BroadcastReceiver to detect the internet connection.
    private val networkConnection = NetworkConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCreateLobby).setOnClickListener {
            Intent(this, LobbyActivity::class.java)
                .putExtra(ActivityConstants.IS_SERVER, true)
                .also {
                    startActivity(it)
                }
        }

        findViewById<Button>(R.id.btnJoinLobby).setOnClickListener {
            Intent(this, LobbyActivity::class.java)
                .putExtra(ActivityConstants.IS_SERVER, false)
                .also {
                    startActivity(it)
                }
        }

        findViewById<Button>(R.id.btnScoreboard).setOnClickListener {
            Intent(this, ScoreboardActivity::class.java).also {
                startActivity(it)
            }
        }

        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            Intent(this, TutorialActivity::class.java).also {
                startActivity(it)
            }
        }

        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            Intent(this, AboutActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        askPermissionsForMessages(true)

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
        updateButtons(isConnected)
    }

    private fun updateButtons(isConnected: Boolean) {
        val btnCreateLobby = findViewById<Button>(R.id.btnCreateLobby)
        val btnJoinLobby = findViewById<Button>(R.id.btnJoinLobby)
        val btnScoreboard = findViewById<Button>(R.id.btnScoreboard)

        // Those buttons only work if exist internet connection.
        if (isConnected) {
            btnCreateLobby.isEnabled = true
            btnCreateLobby.background = ContextCompat.getDrawable(this, R.drawable.menu_buttons)
            btnJoinLobby.isEnabled = true
            btnJoinLobby.background = ContextCompat.getDrawable(this, R.drawable.menu_buttons)
            btnScoreboard.isEnabled = true
            btnScoreboard.background = ContextCompat.getDrawable(this, R.drawable.menu_buttons)
        } else {
            btnCreateLobby.isEnabled = false
            btnCreateLobby.background =
                ContextCompat.getDrawable(this, R.drawable.menu_buttons_disabled)
            btnJoinLobby.isEnabled = false
            btnJoinLobby.background =
                ContextCompat.getDrawable(this, R.drawable.menu_buttons_disabled)
            btnScoreboard.isEnabled = false
            btnScoreboard.background =
                ContextCompat.getDrawable(this, R.drawable.menu_buttons_disabled)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ActivityConstants.REQUEST_CODE_MESSAGES) {
            askPermissionsForMessages(false)
        }
    }

    private fun askPermissionsForMessages(askPerm: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Ask the permissions to send and receive messages.
            if (askPerm)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS),
                    ActivityConstants.REQUEST_CODE_MESSAGES
                )
            else
                finish()

            return
        }
    }
}