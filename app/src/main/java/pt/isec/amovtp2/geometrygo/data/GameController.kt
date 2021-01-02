package pt.isec.amovtp2.geometrygo.data

import androidx.lifecycle.MutableLiveData
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class GameController {
    enum class State {
        STARTING_TEAM, NEW_PLAYER, PLAYER_LEFT, READY_TO_PLAY, NOT_ENOUGH_PLAYERS, END_LOBBY
    }

    private lateinit var team: Team
    val state = MutableLiveData(State.STARTING_TEAM)

    fun startAsServer(latitude: Double, longitude: Double) {
        createTeamName()
        team.addPlayer(1, latitude, longitude)
        val leader = team.getLeader()

        if (leader.serverSocket == null) {
            thread {
                leader.serverSocket = ServerSocket(9000)
                leader.serverSocket.apply {
                    try {
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

    private fun initTeam(leader: Player, socket: Socket) {
        leader.socket = socket

        try {
            leader.threadCreateTeam = thread {
                if (team.getServerSocket() == null)
                    return@thread

                val oIS = leader.oIS!!.bufferedReader()

                while (state.value != State.READY_TO_PLAY) {
                    val newPlayerInfo = oIS.readLine()

                    team.addPlayer(team.getSize(),
                            newPlayerInfo.split(" ")[0].toDouble(),
                            newPlayerInfo.split(" ")[1].toDouble()
                    )
                }
            }
        } catch (_: Exception) {

        } finally {
            deleteLobby()
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
        } catch (_: Exception) { }
    }

    private fun createTeamName() {
        team = Team("aaa")
    }

}