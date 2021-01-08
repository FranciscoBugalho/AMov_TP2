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
        createTeamName()
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
                        player.id.toString() + " " + player.latitude.toString() + " " + player.longitude.toString()
                    )
                    printStream.flush()
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
    }

    private fun receiveMessagesFromClients(socket: Socket){
        player.socket = socket

        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    val id = newPlayerInfo.split(" ")[0].toInt()

                    Log.i("receiveMessagesFromC...", "recebi mensagem de um jogador")
                    Log.i("receiveMessagesFromC...", "$newPlayerInfo")

                    if(id == -1){ //recebi novo jogador
                        Log.i("receiveMessagesFromC...", "team size + 1: ${team!!.getPlayers().size + 1}")
                        team!!.addPlayer(
                            team!!.getPlayers().size + 1, //atribuição de id
                            newPlayerInfo.split(" ")[1].toDouble(),
                            newPlayerInfo.split(" ")[2].toDouble()
                        )
                        state.postValue(State.NEW_PLAYER)

                        player.oS.run {
                            thread {
                                try {
                                    val printStream = PrintStream(this)
                                    printStream.println(id.toString() + " " + "${newPlayerInfo.split(" ")[1]} ${newPlayerInfo.split(" ")[2]}")
                                    printStream.flush()
                                } catch (_: Exception) {
                                    //stopGame()
                                }
                            }
                        }
                    }
                    else{ //update jogador
                        if (team!!.containsPlayerById(id)) {
                            team!!.updatePlayerLocation(
                                id,
                                newPlayerInfo.split(" ")[1].toDouble(),
                                newPlayerInfo.split(" ")[2].toDouble()
                            )

                            state.postValue(State.UPDATE_VIEW)
                        }
                    }
                }
            }
            catch (_: Exception) {
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

                    Log.i("receiveDataFromPlayers", "${state.value }")

                    val newPlayerInfo = iS.readLine()

                    val id = newPlayerInfo.split(" ")[0].toInt()

                    Log.i("receiveDataFromPlayers", "recebi mensagem de outros jogadores")
                    Log.i("receiveDataFromPlayers", "$newPlayerInfo")

                    if(player.id != -1){
                        if(player.id == 1){ //se é o servidor
                            team!!.addPlayer(
                                team!!.getPlayers().size,
                                newPlayerInfo.split(" ")[1].toDouble(),
                                newPlayerInfo.split(" ")[2].toDouble()
                            )
                            state.postValue(State.NEW_PLAYER)
                        }
                        else{
                            if (team!!.containsPlayerById(-1)) { //se é atribuição de Id
                                team!!.updatePlayerId(-1, player.id, player.latitude, player.longitude)
                                state.postValue(State.UPDATE_VIEW)
                            }
                            else{
                                if (team!!.containsPlayerById(id)) { //se jogador ja existe
                                    team!!.updatePlayerLocation(
                                        id,
                                        newPlayerInfo.split(" ")[1].toDouble(),
                                        newPlayerInfo.split(" ")[2].toDouble()
                                    )
                                    state.postValue(State.UPDATE_VIEW)
                                }
                                else{ //se jogador não existe
                                    team!!.addPlayer(
                                        team!!.getPlayers().size,
                                        newPlayerInfo.split(" ")[1].toDouble(),
                                        newPlayerInfo.split(" ")[2].toDouble()
                                    )
                                    state.postValue(State.NEW_PLAYER)
                                }
                            }
                        }
                    }


                    if (team!!.getSize() >= DataConstants.MIN_PLAYERS)
                        state.postValue(State.READY_TO_PLAY)

                    Log.i("receiveDataFromPlayers", "${state.value }")
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
        Log.i("sendLocationToTeam", "team size: ${team?.getPlayers()?.size}")

        team?.getPlayers()?.forEach {
            if(it.id != player.id){ //nao mandar p mim mesmo
                Log.i("sendLocationToTeam", "oS null?: ${player.oS == null}")
                player.oS?.run {
                    thread {
                        try {
                            Log.i("sendLocationToTeam", "Mandei mensagem para o cliente de id: ${it.id}")
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
        return team!!.getPlayers()[position].id!!
    }

    fun getPlayerLocation(position: Int): String {
        return String.format(
            "%.2f",
            team!!.getPlayers()[position].latitude
        ) + " \t " + String.format("%.2f", team!!.getPlayers()[position].longitude)
    }

}