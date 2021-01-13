package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Game.game
import pt.isec.amovtp2.geometrygo.data.GameController
import pt.isec.amovtp2.geometrygo.data.constants.MessagesStatusConstants
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogChooseContact
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogCreateLobby
import pt.isec.amovtp2.geometrygo.fragments.AlertDialogJoinLobby

// 192.168.1.70
class LobbyActivity : AppCompatActivity() {

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

    // Dialog to user insert the team name.
    private lateinit var dialogCreate: AlertDialogCreateLobby

    // Dialog to user insert the server ip address.
    private lateinit var dialogJoin: AlertDialogJoinLobby

    // Location callback to get the latitude and the longitude.
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.locations?.forEach {
                latitude = it.latitude
                longitude = it.longitude

                if (latitude != null && longitude != null) {
                    if (!lobbyStarted) {
                        lobbyStarted = true
                        if (isServer) {
                            dialogCreate.setLatitude(latitude!!)
                            dialogCreate.setLongitude(longitude!!)
                        } else {
                            dialogJoin.setLatitude(latitude!!)
                            dialogJoin.setLongitude(longitude!!)
                        }
                    } else {
                        if(!game.playerExists() || game.getPlayer().serverSocket != null)
                            game.sendLocationToTeam(latitude!!, longitude!!)
                        else{
                            game.sendLocationToServer(latitude!!, longitude!!)
                        }
                    }
                }
            }
        }
    }

    /**
     * onCreate
     * 1.
     */
    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        // Initialize the Fused Location Provider.
        fLoc = FusedLocationProviderClient(this)

        game = ViewModelProvider(this).get(GameController::class.java)

        // Define which view the user will see depending if he started the app on server mode or not.
        isServer = intent.getBooleanExtra(ActivityConstants.IS_SERVER, false)
        if (isServer) {
            setContentView(R.layout.activity_lobby)

            btnStart = findViewById(R.id.btnStartGame)
            btnStart.isEnabled = false
            btnStart.setOnClickListener {
                game.startGame()
            }

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

            // Create and display the dialog to define the team name.
            dialogCreate = AlertDialogCreateLobby(
                game,
                latitude,
                longitude,
                findViewById(R.id.tvTeamName)
            )

            dialogCreate.presentDialog(supportFragmentManager)

        } else {
            setContentView(R.layout.activity_lobby_client)

            // Create and display the dialog to insert the server ip address.
            dialogJoin = AlertDialogJoinLobby(game, latitude, longitude)
            dialogJoin.presentDialog(supportFragmentManager)
        }

        game.state.observe(this@LobbyActivity) {
            when (game.state.value) {
                GameController.State.STARTING_TEAM -> updateView()
                GameController.State.NEW_PLAYER -> updateView()
                GameController.State.UPDATE_VIEW -> updateView()
                GameController.State.NOT_ENOUGH_PLAYERS -> updateButton(false)
                GameController.State.PLAYER_LEFT -> finish()
                GameController.State.END_LOBBY -> finish()
                GameController.State.READY_TO_PLAY -> updateButton(true)
                GameController.State.START -> play()
                else -> updateView()
            }
        }
    }

    private fun play() {
        // Will change the activity and start the game.
        if (isServer) {
            Intent(this, PlayActivity::class.java)
                .putExtra(ActivityConstants.IS_SERVER, true)
                .also {
                    startActivity(it)
                }
        } else {
            Intent(this, PlayActivity::class.java)
                .putExtra(ActivityConstants.IS_SERVER, false)
                .also {
                    startActivity(it)
                }
        }
    }

    override fun onBackPressed() {
        val dlg = AlertDialog.Builder(this).run {
            if (isServer)
                setTitle(getString(R.string.ad_ql_close_lobby))
            else
                setTitle(getString(R.string.ad_ql_quit_lobby))

            setPositiveButton(getString(R.string.ad_ql_btn_yes)) { dlg: DialogInterface, _: Int ->
                if (isServer) {
                    game.serverChangeStatus(MessagesStatusConstants.END_STATE)
                } else {
                    game.clientExitLobby()
                }
                dlg.dismiss()
            }
            setNegativeButton(getString(R.string.ad_ql_btn_no)) { dlg: DialogInterface, _: Int ->
                dlg.dismiss()
            }
            setCancelable(false)
            create()
        }
        dlg.show()
    }

    /**
     * updateView
     * 1.
     */
    private fun updateView() {
        if (game.teamExists() == null)
            return

        val linearLayout = findViewById<LinearLayout>(R.id.llPlayers)

        linearLayout.removeAllViews()
        linearLayout.invalidate()

        // Set team name.
        val tvTitle = findViewById<TextView>(R.id.tvTeamName)
        tvTitle.text = game.getTeamName()
        tvTitle.invalidate()

        // Updates the LinearLayout with the players.
        for (i in 0 until game.getTeam().getPlayers().size) {
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

            // Gets player id.
            val playerId = game.getPlayerId(i)

            if (playerId == -1 || playerId == game.getPlayerId()) {
                tvNewPlayerId.text = getString(R.string.you_player_tag)

                tvNewPlayerId.setTypeface(null, Typeface.BOLD)
            } else {
                (getString(R.string.player_tag) + " " + playerId).also {
                    tvNewPlayerId.text = it
                }
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
            tvNewPlayerLocation.text = game.getPlayerLocation(i)
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
    }

    private fun updateButton(buttonEnabled: Boolean) {
        btnStart.isEnabled = buttonEnabled

        if (buttonEnabled)
            btnStart.background = ContextCompat.getDrawable(this, R.drawable.menu_buttons)
        else
            btnStart.background = ContextCompat.getDrawable(this, R.drawable.menu_buttons_disabled)
    }

    /**
     * updateView
     * 1. Calls "startLocation" method
     */
    override fun onResume() {
        super.onResume()
        startLocation(true)
    }

    /**
     * onPause
     * 1. If location service is enabled, disables it
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
        if (requestCode == ActivityConstants.REQUEST_CODE_LOCATION) {
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
                    ), ActivityConstants.REQUEST_CODE_LOCATION
                )
            else
                finish()

            return
        }

        val locReq = LocationRequest().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            //maxWaitTime = 10000
        }
        fLoc.requestLocationUpdates(locReq, locationCallback, null)
        locEnabled = true
    }

    fun sendMessage(view: View) {
        val dialogSendSMS = AlertDialogChooseContact(findViewById<TextView>(R.id.tvIpAddress).text.toString())
        dialogSendSMS.presentDialog(supportFragmentManager)
    }

}