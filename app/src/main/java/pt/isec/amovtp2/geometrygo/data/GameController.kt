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

    private var team: Team? = null
    val state = MutableLiveData(State.STARTING_TEAM)

    fun startAsServer(latitude: Double, longitude: Double) {
        team!!.addPlayer(1, latitude, longitude)
        val leader = team!!.getLeader()

        if (leader.serverSocket == null) {
            thread {
                leader.serverSocket = ServerSocket(DataConstants.SERVER_DEFAULT_PORT)
                leader.serverSocket.apply {
                    try {
                        state.postValue(State.NOT_ENOUGH_PLAYERS)
                        initTeam(leader, leader.serverSocket!!.accept())
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
        createTeamName()
        val player = Player(2, latitude, longitude)
        thread {
            try {
                val newSocket = Socket(serverIP, serverPort)
                player.socket = newSocket
                sendMyDataToOthers(player)
                team!!.addPlayer(player)
                initTeam(player, newSocket)
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
                        team!!.getSize()
                            .toString() + " " + player.latitude.toString() + " " + player.longitude.toString()
                    )
                    printStream.flush()
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
    }

    private fun initTeam(player: Player, socket: Socket) {
        player.socket = socket

        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val oIS = player.iS!!.bufferedReader()

                while (state.value != State.START) {
                    val newPlayerInfo = oIS.readLine()

                    val id = newPlayerInfo.split(" ")[0].toInt()

                    if (team!!.containsPlayerById(id)) {
                        team!!.updatePlayerLocation(
                            id,
                            newPlayerInfo.split(" ")[1].toDouble(),
                            newPlayerInfo.split(" ")[2].toDouble()
                        )

                        state.postValue(State.UPDATE_VIEW)
                    } else {
                        team!!.addPlayer(
                            id,
                            newPlayerInfo.split(" ")[1].toDouble(),
                            newPlayerInfo.split(" ")[2].toDouble()
                        )
                        state.postValue(State.NEW_PLAYER)
                    }

                    if (team!!.getSize() >= DataConstants.MIN_PLAYERS)
                        state.postValue(State.READY_TO_PLAY)
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
        team?.getPlayers()?.forEach {
            it.oS?.run {
                thread {
                    try {
                        val printStream = PrintStream(this)
                        printStream.println(it.id.toString() + " " + "$latitude $longitude")
                        printStream.flush()
                    } catch (_: Exception) {
                        //stopGame()
                    }
                }
            }
        }
    }

    fun createTeam(teamName: String) {
        team = Team(teamName)
    }

    fun getTeamName(): String {
        return team!!.teamName
    }

    private fun createTeamName() {
        team = Team("aaa")
    }

    fun getTeam(): Team {
        return team!!
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