package pt.isec.amovtp2.geometrygo.data
import com.google.firebase.Timestamp

class Scores(internal val polignSize:Int ,internal val team_id: String, internal val teamName:String, internal val playersPositions : ArrayList<String>,
             internal val avgDistance : String, internal val area : Double, internal val date :Timestamp ) {
}