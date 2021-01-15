package pt.isec.amovtp2.geometrygo.data

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import pt.isec.amovtp2.geometrygo.data.constants.DataConstants

class Team(internal var teamName: String) {

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

    fun getPlayerById(id: Int): Player? {
        players.forEach {
            if (it.id == id)
                return it
        }
        return null
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


    fun isLastPlayer(id: Int): Boolean {
        if (id == players[players.size - 1].id)
            return true
        return false
    }

    fun removePlayer(player: Player?): Boolean {
        if (player == null)
            return false
        return players.remove(player)
    }

    fun getPlayerPosition(playerId: Int): LatLng? {
        players.forEach {
            if (it.id == playerId)
                return LatLng(it.latitude, it.longitude)
        }
        return null
    }

    fun closeConnections() {
        players.forEach {
            it.serverSocket?.close()
            it.socket?.close()
            it.threadCreateTeam?.join()
        }
    }

    fun setPlayerLocation(
        id: Int,
        latitude: Double,
        longitude: Double,
        connectionDate: Timestamp
    ): Boolean {
        var diff = connectionDate.toDate().time - Timestamp.now().toDate().time

        diff /= (60 * 1000)

        // If the last connection was more than 5 minutes, set the player to be removed
        if (diff > DataConstants.MAX_TIME_WITHOUT_COMMUNICATION)
            return true

        updatePlayerLocation(id, latitude, longitude)
        return false
    }

    fun removePlayers(playersToRemove: ArrayList<Player>) {
        synchronized(players) {
            players.removeAll(playersToRemove)
        }
    }

    fun getLastPlayer(): Player {
        return players[players.lastIndex]
    }

    fun getNextPlayer(id: Int): Player {
        return players[id]
    }

    fun getBeforePlayer(id: Int): Player {
        return players[id - 2]
    }

}