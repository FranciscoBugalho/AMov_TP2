package pt.isec.amovtp2.geometrygo.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amovtp2.geometrygo.R

class PlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define which view the user will see depending if he started the app on server mode or not
        if (intent.getBooleanExtra(IntentConstants.IS_SERVER, false))
            setContentView(R.layout.activity_play)
        else
            setContentView(R.layout.activity_play_client)
    }
}