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
        STARTING_TEAM, NEW_PLAYER, PLAYER_LEFT, READY_TO_PLAY, NOT_ENOUGH_PLAYERS, END_LOBBY, START
    }

    private lateinit var team: Team
    val state = MutableLiveData(State.STARTING_TEAM)

    fun startAsServer(latitude: Double, longitude: Double) {
        createTeamName()
        team.addPlayer(1, latitude, longitude)
        val leader = team.getLeader()

        if (leader.serverSocket == null) {
            thread {
                leader.serverSocket = ServerSocket(DataConstants.SERVER_DEFAULT_PORT)
                leader.serverSocket.apply {
                    try {
                        state.postValue(State.NOT_ENOUGH_PLAYERS)
                        initTeam(leader, leader.serverSocket!!.accept())
                    } catch (_: Exception) {

                    } finally {
                        leader.serverSocket?.close()
                        leader.serverSocket = null
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
        team.addPlayer(1, latitude, longitude)
        if (team.getLastPlayer().socket != null)
            return
        thread {
            try {
                val newSocket = Socket(serverIP, serverPort)
                val player = Player(team.getSize(), latitude, longitude)
                player.socket = newSocket
                sendMyDataToOthers(player)
                initTeam(player, newSocket)
            } catch (_: Exception) {
                Log.i("TAG", "startAsClient: ")
            }
        }
    }

    private fun sendMyDataToOthers(player: Player) {
        player.oOS?.run {
            thread {
                try {
                    val printStream = PrintStream(this)
                    printStream.println(player.latitude.toString() + " " + player.longitude.toString())
                    printStream.flush()
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
    }

    private fun initTeam(player: Player, socket: Socket) {
        player.socket = socket

        try {
            player.threadCreateTeam = thread {
                if (player.oIS == null)
                    return@thread

                val oIS = player.oIS!!.bufferedReader()

                while (state.value != State.START) {
                    val newPlayerInfo = oIS.readLine()

                    team.addPlayer(
                        team.getSize() + 1,
                        newPlayerInfo.split(" ")[0].toDouble(),
                        newPlayerInfo.split(" ")[1].toDouble()
                    )

                    state.postValue(State.NEW_PLAYER)

                    if (team.getSize() >= DataConstants.MIN_PLAYERS)
                        state.postValue(State.READY_TO_PLAY)
                }
            }
        } catch (_: Exception) {
            //deleteLobby()
        } finally {
        }

    }

    private fun deleteLobby() {
        try {
            val leader = team.getLeader()
            state.postValue(State.END_LOBBY)
            leader.socket?.close()
            leader.socket = null
            leader.threadCreateTeam?.interrupt()
            leader.threadCreateTeam = null
        } catch (_: Exception) {
        }
    }

    private fun createTeamName() {
        team = Team("aaa")
    }

    fun getLastPlayerId(): Int {
        return team.getLastPlayer().id
    }

    fun getPlayerLocation(): String {
        return String.format(
            "%.2f",
            team.getLastPlayer().latitude
        ) + " \t " + String.format("%.2f", team.getLastPlayer().longitude)
    }

}