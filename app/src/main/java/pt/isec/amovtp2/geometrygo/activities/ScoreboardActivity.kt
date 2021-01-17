package pt.isec.amovtp2.geometrygo.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.Scores
import pt.isec.amovtp2.geometrygo.data.UtilsFunctions

class ScoreboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)
        updateScores()
    }

    fun getScores(): HashMap<Int, ArrayList<Scores>> {
        val scores = hashMapOf<Int,ArrayList<Scores>>()
        val db = Firebase.firestore
        db.collection("Polygns").get().addOnSuccessListener { polignType ->
            val array = arrayListOf<Scores>()
            for (polign in polignType){
                array.add(polign.toObject(Scores::class.java))
            }
            if(scores.containsKey(array.lastOrNull()!!.polignSize))
                scores[array.lastOrNull()!!.polignSize]!!.addAll(array)
            else
                scores.put(array.lastOrNull()!!.polignSize, array )
        }
        return scores
    }

    fun updateScores(){
        val scrollView = findViewById<LinearLayout>(R.id.top5)
        val scores = getScores()

        for (key in scores.keys){
            // Create the LinearLayout
            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            linearLayout.orientation = LinearLayout.HORIZONTAL

            val tv = TextView(this)
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tv.text = key.toString()
            tv.setTextColor(Color.BLACK)
            tv.maxLines = 1
            tv.gravity = Gravity.CENTER_VERTICAL
            linearLayout.addView(tv)

            val top5 = scores[key]
            top5!!.sortBy { it.area }

            var qtd = 5
            if(top5.size<qtd)
                qtd = top5.size

            val ll = LinearLayout(this)
            ll.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ll.orientation = LinearLayout.VERTICAL

            for ( index in 0 until qtd) {
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
                tvScore.text = top5[index].teamName+" "+top5[index].area +"m "+ top5[index].avgDistance +" "+ UtilsFunctions.convertDateToStr(top5[index].date.toDate())
                tvScore.setTextColor(Color.BLACK)
                tvScore.maxLines = 3
                tvScore.gravity = Gravity.START
                insideLinearLayout.addView(tvScore)
                ll.addView(insideLinearLayout)
            }
            scrollView.addView(linearLayout)
            scrollView.addView(ll)
        }

    }

}
