package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.common.collect.Lists
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Game.game
import pt.isec.amovtp2.geometrygo.data.GameController
import pt.isec.amovtp2.geometrygo.data.constants.DataConstants

class PlayActivity : AppCompatActivity(), OnMapReadyCallback {

    // Fused Location Provider.
    private lateinit var fLoc: FusedLocationProviderClient

    // End game button.
    private lateinit var btnEndGame: Button

    // Timer TextView.
    private lateinit var tvTimer: TextView

    // Verifies if the location is enabled.
    private var locEnabled = false

    // Represents the latitude.
    private var latitude: Double? = null

    // Represents the longitude.
    private var longitude: Double? = null

    // Map.
    private lateinit var map: GoogleMap

    // Polygon.
    private lateinit var polygon: Polygon

    // Markers list.
    private var markers = Lists.newCopyOnWriteArrayList<Marker>()

    private lateinit var timer: CountDownTimer

    // Location callback to get the latitude and the longitude.
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.locations?.forEach {
                latitude = it.latitude
                longitude = it.longitude

                if (latitude != null && longitude != null) {
                    game.canSaveData(latitude!!, longitude!!)
                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync(this)

        btnEndGame = findViewById(R.id.btnEndGame)
        btnEndGame.setOnClickListener {
            game.confirmEnd()
        }

        tvTimer = findViewById(R.id.tvTimer)

        // Close the network connection.
        game.closeAllConnections()
        game.createDatabase()

        // Stats the timer
        startTimer()

        game.readDataFromDatabase(applicationContext)

        // Initialize the Fused Location Provider.
        fLoc = FusedLocationProviderClient(this)

        game.state.observe(this@PlayActivity) {
            when (game.state.value) {
                GameController.State.UPDATE_VIEW -> updateView(false)
                GameController.State.ADD_BUTTON -> updateView(true)
                GameController.State.END_GAME_WIN -> endGame(true)
                GameController.State.END_GAME_LOSE -> endGame(false)
                GameController.State.END_LOBBY -> closeGame()
            }
        }

        createDialogPolygon()
    }

    private fun closeGame() {
        Intent(this, MainActivity::class.java)
            .also {
                startActivity(it)
                finish()
            }
    }

    private fun endGame(win: Boolean) {
        timer.cancel()
        if (win) {
            Intent(this, EndGameActivity::class.java)
                .putExtra(ActivityConstants.IS_WIN, win)
                .also {
                    startActivity(it)
                    finish()
                }
        } else {
            Intent(this, EndGameActivity::class.java)
                .putExtra(ActivityConstants.IS_WIN, win)
                .putExtra(ActivityConstants.LOSE_INFORMATION, game.getLoseInformation())
                .also {
                    startActivity(it)
                    finish()
                }
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(DataConstants.GAME_TIME.toLong(), 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                if (seconds < 10)
                    tvTimer.text = "$minutes:0$seconds"
                else
                    tvTimer.text = "$minutes:$seconds"
            }

            override fun onFinish() {
                game.endGame(applicationContext)
            }
        }
        timer.start()
    }

    private fun updateView(isButton: Boolean) {
        // Adds the button on screen
        if (isButton && !btnEndGame.isVisible) {
            btnEndGame.isEnabled = true
            btnEndGame.visibility = View.VISIBLE
        } else if (!isButton && btnEndGame.isVisible) { // Removes the button from the screen
            btnEndGame.isEnabled = false
            btnEndGame.visibility = View.GONE
        }

        updateTextView()
        updateMarkers()
        drawPolygon()
    }

    private fun updateTextView() {
        if (!this::map.isInitialized) return

        val textView = findViewById<TextView>(R.id.tvInfoPlayers)
        textView.text = game.getPlayersDistance()
        textView.invalidate()
    }

    private fun updateMarkers() {
        if (!this::map.isInitialized) return

        // Removes the markers from the screen.
        markers.forEach {
            it.remove()
        }
        markers.clear()

        // Draws the new markers.
        for (i in 0 until game.getTeam().getPlayers().size) {
            if (game.getPlayerId() != game.getTeam().getPlayers()[i].id) {
                val mo = MarkerOptions()
                    .position(
                        LatLng(
                            game.getTeam().getPlayers()[i].latitude,
                            game.getTeam().getPlayers()[i].longitude
                        )
                    )
                    .title(
                        getString(R.string.play_activity_player_first_letter) + game.getPlayerId(i)
                                + " - " + game.getPlayerAngle(game.getPlayerId(i)) + "°"
                    )
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                val marker = map.addMarker(mo)
                markers.add(marker)
                marker.showInfoWindow()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return

        map = googleMap

        // Define map settings.
        if (locEnabled)
            map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        val cp = CameraPosition.Builder()
            .target(LatLng(game.getPlayer().latitude, game.getPlayer().longitude)).zoom(17f)
            .bearing(0f).tilt(0f).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cp))

        // Set a market on the player 1 start point.
        val mo =
            MarkerOptions().position(LatLng(game.getTeam().latitude!!, game.getTeam().longitude!!))
                .title(getString(R.string.ap_player_1_start_point))
        val startPoint = map.addMarker(mo)
        startPoint.showInfoWindow()

        drawPolygon()
    }

    private fun drawPolygon() {
        if (!this::map.isInitialized) return

        if (this::polygon.isInitialized)
            polygon.remove()

        val polygonOptions = PolygonOptions()
        // Add players positions.
        for (i in 0 until game.getTeam().getPlayers().size) {
            val position = game.getPlayerPosition(game.getPlayerId(i))
            if (position != null) {
                polygonOptions.add(position)
            }
        }
        polygon = map.addPolygon(polygonOptions)
        polygon.tag = ActivityConstants.POLYGON_TAG

        stylePolygon(polygon)
    }

    private fun stylePolygon(polygon: Polygon) {
        // Get the data object stored with the polygon.
        val type = polygon.tag?.toString() ?: ""

        if (type == ActivityConstants.POLYGON_TAG) {
            polygon.strokeWidth = 8f
            polygon.strokeColor = -0x657db
        }
    }

    override fun onBackPressed() {
        val dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.ap_dialog_back_pressed_title))

            setPositiveButton(getString(R.string.ap_dialog_back_pressed_yes)) { dlg: DialogInterface, _: Int ->
                game.setAsRemoved(game.getPlayerId())
                dlg.dismiss()
                finish()
            }
            setNegativeButton(getString(R.string.ap_dialog_back_pressed_no)) { dlg: DialogInterface, _: Int ->
                dlg.dismiss()
            }
            setCancelable(false)
            create()
        }
        dlg.show()
    }

    override fun onResume() {
        super.onResume()
        startLocation(true)
    }

    override fun onPause() {
        super.onPause()
        if (locEnabled) {
            fLoc.removeLocationUpdates(locationCallback)
            locEnabled = false
        }
    }

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
            interval = 15000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fLoc.requestLocationUpdates(locReq, locationCallback, null)
        locEnabled = true
    }

    private fun createDialogPolygon() {
        val dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.ap_dialog_create_dialog_polygon_title) + " " + getPolygonName())

            setPositiveButton(getString(R.string.ap_dialog_create_polygon_ok)) { dlg: DialogInterface, _: Int ->
                dlg.dismiss()
            }
            setCancelable(true)
            create()
        }
        dlg.show()
    }

    private fun getPolygonName(): String {
        when(game.getTeam().getSize()) {
            3 -> return getString(R.string.ap_polygon_triangle)
            4 -> return getString(R.string.ap_polygon_square)
            5 -> return getString(R.string.ap_polygon_pentagon)
            6 -> return getString(R.string.ap_polygon_hexagon)
            7 -> return getString(R.string.ap_polygon_heptagon)
            8 -> return getString(R.string.ap_polygon_octagon)
            9 -> return getString(R.string.ap_polygon_eneagon)
            10 -> return getString(R.string.ap_polygon_decagon)
        }
        return ""
    }

}