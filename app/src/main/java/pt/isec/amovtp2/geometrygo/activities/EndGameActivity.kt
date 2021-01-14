package pt.isec.amovtp2.geometrygo.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amovtp2.geometrygo.R

class EndGameActivity : AppCompatActivity() {
    // TextView with the information.
    private lateinit var tvInformation: TextView

    // Button to go to the main menu.
    private lateinit var btnMainMenu: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        btnMainMenu = findViewById(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            Intent(this, MainActivity::class.java)
                .also {
                    startActivity(it)
                    finish()
                }
        }

        val isWin = intent.getBooleanExtra(ActivityConstants.IS_WIN, false)
        if (isWin) {
        } else {
            val loseInformation = intent.getStringExtra(ActivityConstants.LOSE_INFORMATION)

            tvInformation = findViewById(R.id.tvInformation)

            tvInformation.text = getString(R.string.aeg_you_lose) + " " + loseInformation
        }
    }
}