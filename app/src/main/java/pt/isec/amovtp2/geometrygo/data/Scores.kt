package pt.isec.amovtp2.geometrygo.data

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class Scores() : Serializable {
    internal var polignSize:Int = 0
    internal var team_id: String = ""
    internal var teamName:String = ""
    internal var playersPositions : ArrayList<String> = arrayListOf()
    internal var avgDistance : String = ""
    internal var area : Double = 0.0
    internal var date :Timestamp = Timestamp(Date())

    constructor(polignSize: Int, team_id: String, teamName:String, playersPositions : ArrayList<String>, avgDistance : String, area : Double, date :Timestamp ) : this() {
        this.polignSize = polignSize
        this.team_id = team_id
        this.teamName = teamName
        this.playersPositions = playersPositions
        this.avgDistance = avgDistance
        this.area = area
        this.date = date
    }
}