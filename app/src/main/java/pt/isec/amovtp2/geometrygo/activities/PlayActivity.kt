package pt.isec.amovtp2.geometrygo.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import pt.isec.amovtp2.geometrygo.*
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.SharedPreferenceUtil
import pt.isec.amovtp2.geometrygo.data.GameController
import pt.isec.amovtp2.geometrygo.data.GameController.State.NOT_ENOUGH_PLAYERS
import java.util.concurrent.TimeUnit

class PlayActivity : AppCompatActivity() {

    /**
     * Game controller.
     */
    private lateinit var game: GameController

    private lateinit var btnStart: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION), 1234)
        }


        startService(Intent(this, MyService::class.java))

        // Define which view the user will see depending if he started the app on server mode or not
        if (intent.getBooleanExtra(IntentConstants.IS_SERVER, false)) {
            setContentView(R.layout.activity_play)

            btnStart = findViewById(R.id.btnStartGame)


            //game.startAsServer(location!!.latitude, location!!.longitude)
            /*game.state.observe(this) {
                when (game.state.value) {
                    NOT_ENOUGH_PLAYERS -> btnStart.isEnabled = false
                }
            }*/
        } else {
            setContentView(R.layout.activity_play_client)
        }
    }

}