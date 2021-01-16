package pt.isec.amovtp2.geometrygo.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.common.collect.Lists
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Game
import pt.isec.amovtp2.geometrygo.data.Game.game

class EndGameActivity : AppCompatActivity(), OnMapReadyCallback {
    // TextView with the information.
    private lateinit var tvInformation: TextView

    // Button to go to the main menu.
    private lateinit var btnMainMenu: Button

    // Define if it is win or not.
    private var isWin: Boolean = false

    // Map.
    private lateinit var map: GoogleMap

    // Polygon.
    private lateinit var polygon: Polygon

    // Markers list.
    private var markers = Lists.newCopyOnWriteArrayList<Marker>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync(this)

        btnMainMenu = findViewById(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            Intent(this, MainActivity::class.java)
                .also {
                    startActivity(it)
                    finish()
                }
        }

        val textView = findViewById<TextView>(R.id.tvInfoPlayers)

        isWin = intent.getBooleanExtra(ActivityConstants.IS_WIN, false)
        if (isWin) {
            textView.text = getString(R.string.aeg_average_distance_tag) + " " + game.getPlayersAverageDistance() + "m    " + getString(R.string.aeg_area_tag) + " " + game.calculateArea() + "m²"
        } else {
            val loseInformation = intent.getStringExtra(ActivityConstants.LOSE_INFORMATION)

            tvInformation = findViewById(R.id.tvInformation)

            tvInformation.text = getString(R.string.aeg_you_lose) + " " + loseInformation

            textView.text = game.getPlayersDistance()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return

        map = googleMap

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
        addMarkers()
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

            if (isWin)
                polygon.fillColor = -0x657db
        }
    }

    private fun addMarkers() {
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
}