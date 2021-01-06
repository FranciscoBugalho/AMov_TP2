package pt.isec.amovtp2.geometrygo.data

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

    fun getSize(): Int {
        return players.size
    }

    fun getLastPlayer(): Player {
        return players[getSize() - 1]
    }

    fun getPlayers(): ArrayList<Player> {
        return players;
    }


}