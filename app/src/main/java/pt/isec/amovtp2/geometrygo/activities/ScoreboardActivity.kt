package pt.isec.amovtp2.geometrygo.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Scores
import pt.isec.amovtp2.geometrygo.data.UtilsFunctions


class ScoreboardActivity : AppCompatActivity() {

    val scores = hashMapOf<Int, ArrayList<Scores>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        getScores()
    }

    fun getScores() {
        val db = Firebase.firestore

        db.collection("Polygons").get().addOnCompleteListener { doc ->

            val list = arrayListOf<String>()
            for (document in doc.result) {
                list.add(document.id)
            }

            for (l in list) {
                db.collection("Polygons").document(l).collection("Scores").get()
                    .addOnCompleteListener { a ->

                        val array = arrayListOf<Scores>()
                        for (score in a.result) {
                            array.add(score.toObject(Scores::class.java))
                        }

                        if (scores.containsKey(array.lastOrNull()!!.polignSize))
                            scores[array.lastOrNull()!!.polignSize]!!.addAll(array)
                        else
                            scores[array.lastOrNull()!!.polignSize] = array

                        updateScores()
                    }
            }

        }
    }

    @SuppressLint("SetTextI18n")
    fun updateScores() {
        val scrollView = findViewById<LinearLayout>(R.id.top5)

        for (key in scores.keys) {
            // Create the LinearLayout
            val linearLayout = LinearLayout(this)
            val param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            param.setMargins(0, 10, 0, 10)
            linearLayout.layoutParams = param
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL

            val tv = TextView(this)
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tv.text = getString(R.string.as_n_vertices) + " " + key.toString()
            tv.setTextColor(Color.BLACK)
            tv.maxLines = 1
            tv.gravity = Gravity.CENTER_HORIZONTAL
            tv.textSize = 20f

            val top5 = scores[key]
            top5!!.sortBy { it.area }

            var qtd = 5
            if (top5.size < qtd)
                qtd = top5.size

            val ll = LinearLayout(this)
            ll.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ll.orientation = LinearLayout.VERTICAL

            linearLayout.addView(tv)

            for (index in 0 until qtd) {
                // Create the LinearLayout
                val insideLinearLayout = LinearLayout(this)
                insideLinearLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                insideLinearLayout.orientation = LinearLayout.HORIZONTAL

                val tvScore = TextView(this)
                tvScore.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tvScore.text =
                    (index + 1).toString() + "º - " + top5[index].teamName + " A:" + String.format("%.2f", top5[index].area) + "m D:" + top5[index].avgDistance + " " + UtilsFunctions.convertDateToStrScores(
                        top5[index].date.toDate()
                    )
                tvScore.setTextColor(Color.BLACK)
                tvScore.maxLines = 3
                tvScore.gravity = Gravity.START
                tvScore.textSize = 20f
                insideLinearLayout.addView(tvScore)
                ll.addView(insideLinearLayout)
            }

            scrollView.addView(linearLayout)
            scrollView.addView(ll)
        }

    }

}
