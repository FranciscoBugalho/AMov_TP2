package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.GameController
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogJoinLobby

// 192.168.1.70
class PlayActivity : AppCompatActivity() {
    // Game controller.
    private lateinit var game: GameController

    // Verifies if the lobby already started or not.
    private var lobbyStarted = false

    // Start button.
    private lateinit var btnStart: Button

    // Fused Location Provider.
    private lateinit var fLoc: FusedLocationProviderClient

    // Verifies if the location is enabled.
    private var locEnabled = false

    // Represents the latitude.
    private var latitude: Double? = null

    // Represents the longitude.
    private var longitude: Double? = null

    // Helps to check if the user started as server or no.
    private var isServer: Boolean = false

    // Dialog to user insert the server ip address.
    private lateinit var dialog: AlertDialogJoinLobby

    // Location callback to get the latitude and the longitude.
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.locations?.forEach {
                latitude = it.latitude
                longitude = it.longitude

                if (!lobbyStarted) {
                    if (isServer)
                        game.startAsServer(latitude!!, longitude!!)
                    else {
                        dialog = AlertDialogJoinLobby(game, latitude!!, longitude!!)
                        dialog.presentDialog(supportFragmentManager)
                    }
                    lobbyStarted = true
                }
                else{
                    if(latitude != null && longitude != null)
                        game.sendLocationToTeam(latitude!!, longitude!!)
                    else
                        Log.e("PlayActivity", "Location is null.")
                }
            }
        }
    }

    /**
     * onCreate
     * 1.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Fused Location Provider.
        fLoc = FusedLocationProviderClient(this)

        game = ViewModelProvider(this).get(GameController::class.java)
        // Define which view the user will see depending if he started the app on server mode or not.
        isServer = intent.getBooleanExtra(IntentConstants.IS_SERVER, false)
        if (isServer) {
            setContentView(R.layout.activity_play)

            btnStart = findViewById(R.id.btnStartGame)

            // Get server ip address.
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val strIPAddress = String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                (ip shr 8) and 0xff,
                (ip shr 16) and 0xff,
                (ip shr 24) and 0xff
            )

            // Display server ip on the screen.
            findViewById<TextView>(R.id.tvIpAddress).text = strIPAddress

        } else {
            setContentView(R.layout.activity_play_client)
        }

        game.state.observe(this@PlayActivity) {
            when (game.state.value) {
                GameController.State.NOT_ENOUGH_PLAYERS -> btnStart.isEnabled =
                    false
                GameController.State.NEW_PLAYER -> updateView()
            }
        }
    }

    /**
     * updateView
     * 1.
     */
    private fun updateView() {
        val linearLayout = findViewById<LinearLayout>(R.id.llPlayers)

        // Creates a LinearLayout to add two TextViews.
        val newPlayerLayout = LinearLayout(this)
        var param = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        param.setMargins(0, 0, 0, 10)
        newPlayerLayout.layoutParams = param
        newPlayerLayout.orientation = LinearLayout.HORIZONTAL

        // Creates the TextView for the player name.
        val tvNewPlayerId = TextView(this)
        param = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        param.weight = 0.50f
        tvNewPlayerId.layoutParams = param
        (getString(R.string.player_tag) + " " + game.getLastPlayerId()).also {
            tvNewPlayerId.text = it
        }
        tvNewPlayerId.setTextColor(Color.BLACK)
        tvNewPlayerId.textSize = 18f
        tvNewPlayerId.gravity = Gravity.START
        tvNewPlayerId.maxLines = 1

        // Creates the TextView to the player location.
        val tvNewPlayerLocation = TextView(this)
        param = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        param.weight = 0.50f
        tvNewPlayerLocation.layoutParams = param
        tvNewPlayerLocation.text = game.getPlayerLocation()
        tvNewPlayerLocation.setTextColor(Color.BLACK)
        tvNewPlayerLocation.textSize = 18f
        tvNewPlayerLocation.gravity = Gravity.END
        tvNewPlayerLocation.maxLines = 1

        // Add the both TextViews to the inner LinearLayout.
        newPlayerLayout.addView(tvNewPlayerId)
        newPlayerLayout.addView(tvNewPlayerLocation)

        // Add the inside LinearLayout to the main LinearLayout.
        linearLayout.addView(newPlayerLayout)

        // To redraw the LinearLayout on the screen.
        linearLayout.invalidate()
    }

    /**
     * updateView
     * 1. calls "startLocation" method
     */
    override fun onResume() {
        super.onResume()
        startLocation(true)
    }

    /**
     * onPause
     * 1. if location service is enabled, disables it
     */
    override fun onPause() {
        super.onPause()
        if (locEnabled) {
            fLoc.removeLocationUpdates(locationCallback)
            locEnabled = false
        }
    }

    /**
     * onRequestPermissionsResult
     * 1.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 25) {
            startLocation(false)
        }
    }

    /**
     * startLocation
     * 1.
     */
    private fun startLocation(askPerm: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Ask the permissions to use location.
            if (askPerm)
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ), 25
                )
            else
                finish()

            return
        }

        val locReq = LocationRequest().apply {
            interval = 5000
            //fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            //maxWaitTime = 10000
        }
        fLoc.requestLocationUpdates(locReq, locationCallback, null)
        locEnabled = true
    }

}