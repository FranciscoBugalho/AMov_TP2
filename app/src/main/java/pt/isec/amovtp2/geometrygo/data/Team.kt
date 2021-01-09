package pt.isec.amovtp2.geometrygo.data

import android.location.Location
import androidx.lifecycle.ViewModel

class Team(internal var teamName: String): ViewModel() {

    // List of players.
    private var players = ArrayList<Player>()

    // Server starting latitude.
    internal var latitude: Double? = null

    // Server starting longitude.
    internal var longitude: Double? = null

    fun getLeader(): Player {
        return players[0]
    }

    fun addPlayer(id: Int, latitude: Double, longitude: Double) {
        players.add(Player(id, latitude, longitude))
    }

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun getSize(): Int {
        return players.size
    }

    fun getPlayers(): ArrayList<Player> {
        return players
    }

    fun containsPlayerById(id: Int): Boolean {
        players.forEach {
            if (it.id == id)
                return true
        }
        return false
    }

    fun updatePlayerLocation(id: Int, latitude: Double, longitude: Double) {
        players.forEach {
            if (it.id == id) {
                it.latitude = latitude
                it.longitude = longitude
                return
            }
        }
    }

    fun updatePlayerId(id: Int, newId: Int, latitude: Double, longitude: Double) {
        players.forEach {
            if (it.id == id) {
                it.id = newId
                it.latitude = latitude
                it.longitude = longitude
                return
            }
        }
    }

    fun checkPlayersDistance(): Boolean {
        val loc1 = Location("aux1")
        val loc2 = Location("aux2")

        for (i in 0 until (players.size / 2)) {
            for (j in (players.size / 2) until players.size) {
                loc1.latitude = players[i].latitude
                loc1.longitude = players[i].longitude

                loc2.latitude = players[j].latitude
                loc2.longitude = players[j].longitude

                if (loc1.distanceTo(loc2) > DataConstants.MAX_DISTANCE_BETWEEN_PLAYERS)
                    return false
            }
        }
        return true
    }

}