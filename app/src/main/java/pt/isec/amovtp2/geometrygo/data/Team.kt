package pt.isec.amovtp2.geometrygo.data

import android.util.Log
import java.io.Serializable
import java.net.ServerSocket

class Team(var teamName: String) {

    // List of players.
    private var players = ArrayList<Player>()

    fun getServerSocket(): ServerSocket? {
        return players[0].serverSocket
    }

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

    fun getLastPlayer(): Player {
        return players[getSize() - 1]
    }

    fun getPlayers(): ArrayList<Player> {
        return players
    }

    fun containsPlayerById(id: Int): Boolean {
        players.forEach {
            if(it.id == id)
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

    fun updatePlayerId(id: Int, newId: Int, latitude: Double, longitude: Double ){
        players.forEach {
            if(it.id == id){
                it.id = newId
                it.latitude = latitude
                it.longitude = longitude
                return
            }

        }
    }

}