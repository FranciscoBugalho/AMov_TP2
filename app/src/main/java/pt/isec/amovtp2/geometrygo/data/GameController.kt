package pt.isec.amovtp2.geometrygo.data

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class GameController : ViewModel() {
    enum class State {
        STARTING_TEAM, NEW_PLAYER, PLAYER_LEFT, READY_TO_PLAY, NOT_ENOUGH_PLAYERS, END_LOBBY, START, UPDATE_VIEW
    }

    private lateinit var player: Player
    private var team: Team? = null
    val state = MutableLiveData(State.STARTING_TEAM)

    fun startAsServer(latitude: Double, longitude: Double) {
        player = Player(1, latitude, longitude)
        team!!.addPlayer(player)

        if (player.serverSocket == null) {
            thread {
                player.serverSocket = ServerSocket(DataConstants.SERVER_DEFAULT_PORT)
                player.serverSocket.apply {
                    try {
                        state.postValue(State.NOT_ENOUGH_PLAYERS)
                        receiveMessagesFromClients(player.serverSocket!!.accept())
                    } catch (_: Exception) {

                    } finally {
                        /*leader.serverSocket?.close()
                        leader.serverSocket = null*/
                    }
                }
            }
        }
    }

    fun startAsClient(
        serverIP: String,
        serverPort: Int = DataConstants.SERVER_DEFAULT_PORT,
        latitude: Double,
        longitude: Double
    ) {
        createEmptyTeamName()
        player = Player(-1, latitude, longitude)
        thread {
            try {
                val newSocket = Socket(serverIP, serverPort)
                player.socket = newSocket
                sendMyDataToOthers(player)
                team!!.addPlayer(player)
                receiveDataFromPlayers()
            } catch (_: Exception) {
                Log.i("TAG", "startAsClient: ")
            }
        }
    }

    private fun sendMyDataToOthers(player: Player) {
        player.oS?.run {
            thread {
                try {
                    val printStream = PrintStream(this)
                    printStream.println(
                        player.id.toString() + " " + player.latitude.toString() + " " + player.longitude.toString() + " " + team!!.teamName
                    )
                    printStream.flush()
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
    }

    private fun receiveMessagesFromClients(socket: Socket) {
        player.socket = socket

        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    val teamName = newPlayerInfo.split(" ")[3]
                    if (teamName != "")
                        team!!.teamName = teamName

                    val id = newPlayerInfo.split(" ")[0].toInt()

                    if (id == -1) { // Received new player.
                        team!!.addPlayer(
                            team!!.getPlayers().size + 1, // Set id.
                            newPlayerInfo.split(" ")[1].toDouble(),
                            newPlayerInfo.split(" ")[2].toDouble()
                        )

                        state.postValue(State.NEW_PLAYER)

                        player.oS.run {
                            thread {
                                try {
                                    val printStream = PrintStream(this)
                                    printStream.println(
                                        "$id " + "${
                                            newPlayerInfo.split(" ")[1]
                                        } ${newPlayerInfo.split(" ")[2]} ${team!!.teamName}"
                                    )
                                    printStream.flush()
                                } catch (_: Exception) {
                                    //stopGame()
                                }
                            }
                        }
                    } else { // Update player information.
                        if (team!!.containsPlayerById(id)) {
                            team!!.updatePlayerLocation(
                                id,
                                newPlayerInfo.split(" ")[1].toDouble(),
                                newPlayerInfo.split(" ")[2].toDouble()
                            )
                            state.postValue(State.UPDATE_VIEW)
                        }
                    }

                    // Sort the players array.
                    team!!.getPlayers().sortBy { it.id }

                    if (team!!.getSize() >= 2 && team!!.checkPlayersDistance())
                            state.postValue(State.READY_TO_PLAY)
                }
            } catch (_: Exception) {
                //deleteLobby()
            }
        }
    }

    private fun receiveDataFromPlayers() {
        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    val teamName = newPlayerInfo.split(" ")[3]
                    if (teamName != "")
                        team!!.teamName = teamName

                    val id = newPlayerInfo.split(" ")[0].toInt()

                    if (id != -1) {
                        if (id == 1) { // If it's server.
                            if (!team!!.containsPlayerById(1)) {
                                team!!.addPlayer(
                                    team!!.getPlayers().size,
                                    newPlayerInfo.split(" ")[1].toDouble(),
                                    newPlayerInfo.split(" ")[2].toDouble()
                                )
                                state.postValue(State.NEW_PLAYER)
                            } else {
                                team!!.updatePlayerLocation(
                                    id,
                                    newPlayerInfo.split(" ")[1].toDouble(),
                                    newPlayerInfo.split(" ")[2].toDouble()
                                )
                                state.postValue(State.UPDATE_VIEW)
                            }
                        } else {
                            if (team!!.containsPlayerById(-1)) { // Set player id.
                                team!!.updatePlayerId(-1, id, player.latitude, player.longitude)
                                state.postValue(State.UPDATE_VIEW)
                            } else {
                                if (team!!.containsPlayerById(id)) { // Player already exists, update.
                                    team!!.updatePlayerLocation(
                                        id,
                                        newPlayerInfo.split(" ")[1].toDouble(),
                                        newPlayerInfo.split(" ")[2].toDouble()
                                    )
                                    state.postValue(State.UPDATE_VIEW)
                                } else { // Player does not exists.
                                    team!!.addPlayer(
                                        team!!.getPlayers().size,
                                        newPlayerInfo.split(" ")[1].toDouble(),
                                        newPlayerInfo.split(" ")[2].toDouble()
                                    )
                                    state.postValue(State.NEW_PLAYER)
                                }
                            }
                        }
                    } else
                        state.postValue(State.UPDATE_VIEW)

                    // Sort the players array.
                    team!!.getPlayers().sortBy { it.id }

                }
            } catch (_: Exception) {
                //deleteLobby()
            }
        }
    }

    private fun deleteLobby() {
        try {
            val leader = team!!.getLeader()
            state.postValue(State.END_LOBBY)
            leader.socket?.close()
            leader.socket = null
            leader.threadCreateTeam?.interrupt()
            leader.threadCreateTeam = null
        } catch (_: Exception) {
        }
    }

    fun sendLocationToTeam(latitude: Double, longitude: Double) {
        if (team == null) return

        synchronized(team!!) {
            team?.getPlayers()?.forEach {
                if (it.id != player.id) { // Don't send to myself.
                    player.oS?.run {
                        thread {
                            try {
                                val printStream = PrintStream(this)
                                printStream.println(player.id.toString() + " " + "$latitude $longitude ${team!!.teamName}")
                                printStream.flush()

                                printStream.println(it.id.toString() + " " + "${it.latitude} ${it.longitude} ${team!!.teamName}")
                                printStream.flush()
                            } catch (_: Exception) {
                                //stopGame()
                            }
                        }
                    }
                } else
                    state.postValue(State.UPDATE_VIEW)
            }
        }
    }

    fun createTeam(teamName: String) {
        team = Team(teamName)
    }

    fun getTeamName(): String {
        return team!!.teamName
    }

    private fun createEmptyTeamName() {
        team = Team("")
    }

    fun getTeam(): Team {
        return team!!
    }

    fun teamExists(): Team? {
        return team
    }

    fun getPlayerId(): Int {
        return player.id
    }

    fun getPlayerId(position: Int): Int {
        return team!!.getPlayers()[position].id
    }

    fun getPlayerLocation(position: Int): String {
        return String.format(
            "%.2f",
            team!!.getPlayers()[position].latitude
        ) + " \t " + String.format("%.2f", team!!.getPlayers()[position].longitude)
    }

}