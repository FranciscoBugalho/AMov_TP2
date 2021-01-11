package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Game.game

class PlayActivity : AppCompatActivity(), OnMapReadyCallback {

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

    // Location callback to get the latitude and the longitude.
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.locations?.forEach {
                latitude = it.latitude
                longitude = it.longitude

                if (latitude != null && longitude != null) {
                    if (isServer)
                        game.sendLocationToTeam(latitude!!, longitude!!)
                    else
                        game.sendLocationToServer(latitude!!, longitude!!)

                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Fused Location Provider.
        fLoc = FusedLocationProviderClient(this)

        // Define which view the user will see depending if he started the app on server mode or not.
        isServer = intent.getBooleanExtra(ActivityConstants.IS_SERVER, false)
        if (isServer) {
            setContentView(R.layout.activity_play)
        } else {
            setContentView(R.layout.activity_play_client)
        }

        (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap?) {
        map ?: return

        // Define map settings
        if(locEnabled)
            map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true


        val cp = CameraPosition.Builder().target(LatLng(game.getPlayer().latitude, game.getPlayer().longitude)).zoom(17f)
            .bearing(0f).tilt(0f).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cp))

        val mo = MarkerOptions().position(LatLng(game.getTeam().latitude!!, game.getTeam().longitude!!))
            .title(getString(R.string.ap_player_1_start_point))
        val startPoint = map.addMarker(mo)
        startPoint.showInfoWindow()

        val polygonOptions = PolygonOptions()
        // Add players positions
        for (i in 0 until game.getTeam().getPlayers().size) {
            val position = game.getPlayerPosition(game.getPlayerId(i))
            if (position != null) {
                polygonOptions.add(position)
            }
        }
        // Add again the first player position to close the polygon
        /*val position = game.getPlayerPosition(1)
        if (position != null)
            polygonOptions.add(position)*/

        val polygon = map.addPolygon(polygonOptions)
        polygon.tag = ActivityConstants.POLYGON_TAG

        stylePolygon(polygon)
    }

    private fun stylePolygon(polygon: Polygon) {
        val POLYGON_STROKE_WIDTH_PX = 8
        val COLOR_BLUE_ARGB = -0x657db


        // Get the data object stored with the polygon.
        val type = polygon.tag?.toString() ?: ""
        val strokeColor = COLOR_BLUE_ARGB

        if (type == ActivityConstants.POLYGON_TAG) {
            polygon.strokeWidth = POLYGON_STROKE_WIDTH_PX.toFloat()
            polygon.strokeColor = strokeColor
        }
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
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            //maxWaitTime = 10000
        }
        fLoc.requestLocationUpdates(locReq, locationCallback, null)
        locEnabled = true
    }
}