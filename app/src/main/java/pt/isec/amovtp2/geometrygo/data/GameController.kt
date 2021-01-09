package pt.isec.amovtp2.geometrygo.data

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

    /**
     * startAsServer
     * Server initialization
     * Starts the threat that receives messages
     */
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

    /**
     * startAsClient
     * Client initialization
     * First communication to server
     */
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
                sendLocationToServer(player.latitude, player.longitude)
                team!!.addPlayer(player)
                receiveDataFromServer()
            } catch (_: Exception) {
                //Log.i("TAG", "startAsClient: ")
            }
        }
    }

    /**
     * receiveMessagesFromClients
     * Server receives the data of a client
     */
    private fun receiveMessagesFromClients(socket: Socket) {
        player.socket = socket

        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    if (team!!.getSize() >= 2 && team!!.checkPlayersDistance())
                        state.postValue(State.READY_TO_PLAY)

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
                }
            } catch (_: Exception) {
                //deleteLobby()
            }
        }
    }

    /**
     * receiveDataFromServer
     * Client receives the data of the team from the server
     */
    private fun receiveDataFromServer() {
        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayersInfo = iS.readLine()

                    if (team!!.getSize() >= 2 && team!!.checkPlayersDistance())
                        state.postValue(State.START)

                    val teamName = newPlayersInfo.split(" ")[0]
                    if (teamName != "")
                        team!!.teamName = teamName

                    //TODO: qq coisa com o status I guess
                    val status = newPlayersInfo.split(" ")[1]
                    val nrPlayers = newPlayersInfo.split(" ")[2].toInt()

                    handlePlayersInfo(nrPlayers, newPlayersInfo)

                    // Sort the players array.
                    team!!.getPlayers().sortBy { it.id }

                }
            } catch (_: Exception) {
                //deleteLobby()
            }
        }
    }

    /**
     * handlePlayersInfo
     * Handles the data of each player received from the server
     */
    private fun handlePlayersInfo(nrPlayers: Int, newPlayersInfo: String) {
        for (i in 3..nrPlayers + 3) {
            val id = newPlayersInfo.split(" ")[i].toInt()

            if (id != -1) {
                if (id == 1) { // If it's server.
                    if (!team!!.containsPlayerById(1)) {
                        team!!.addPlayer(
                            team!!.getPlayers().size,
                            newPlayersInfo.split(" ")[1 + i].toDouble(),
                            newPlayersInfo.split(" ")[2 + i].toDouble()
                        )
                        state.postValue(State.NEW_PLAYER)
                    } else {
                        team!!.updatePlayerLocation(
                            id,
                            newPlayersInfo.split(" ")[1 + i].toDouble(),
                            newPlayersInfo.split(" ")[2 + i].toDouble()
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
                                newPlayersInfo.split(" ")[1 + i].toDouble(),
                                newPlayersInfo.split(" ")[2 + i].toDouble()
                            )
                            state.postValue(State.UPDATE_VIEW)
                        } else { // Player does not exists.
                            team!!.addPlayer(
                                team!!.getPlayers().size,
                                newPlayersInfo.split(" ")[1 + i].toDouble(),
                                newPlayersInfo.split(" ")[2 + i].toDouble()
                            )
                            state.postValue(State.NEW_PLAYER)
                        }
                    }
                }
            } else
                state.postValue(State.UPDATE_VIEW)
        }
    }

    /**
     * deleteLobby
     */
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

    /**
     * sendLocationToTeam
     * Server sends the data from each player to the others
     */
    fun sendLocationToTeam(latitude: Double, longitude: Double) {
        if (team == null) return
        // Standby, Start, End

        var message = createMessageForClients()

        synchronized(team!!) {
            team?.getPlayers()?.forEach { it ->
                if (it.id != player.id) { // Don't send to myself.
                    player.oS?.run {
                        thread {
                            try {
                                val printStream = PrintStream(this)
                                printStream.println(message)
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

    private fun createMessageForClients(): String {
        var message = "${team!!.teamName}" + " "
        if (state.value == State.READY_TO_PLAY || state.value == State.START) {
            message += "startState"
        } else if (state.value == State.END_LOBBY) {
            message += "endState"
        } else {
            message += "standbyState"
        }
        message += " " + team?.getPlayers()!!.size.toString()
        team?.getPlayers()?.forEach { iterator ->
            message += iterator.id.toString() + " " + "${iterator.latitude}" + " " + "${iterator.longitude}"
        }
        return message
    }

    /**
     * sendLocationToServer
     * Client send it's data to the server
     */
    fun sendLocationToServer(latitude: Double, longitude: Double) {
        if (team == null) return

        synchronized(team!!) {
            team?.getPlayerById(1)?.oS.run{
                thread {
                    try {
                        val printStream = PrintStream(this)
                        printStream.println(player.id.toString() + " " + "$latitude $longitude ${team!!.teamName}")
                        printStream.flush()
                    } catch (_: Exception) {
                        //stopGame()
                    }
                }
            }
        }
    }

    /**
     * createTeam
     * Initializes team with a name
     */
    fun createTeam(teamName: String) {
        team = Team(teamName)
    }

    /**
     * createEmptyTeamName
     * Initializes team without a name
     */
    private fun createEmptyTeamName() {
        team = Team("")
    }


    /**
     * getTeamName
     */
    fun getTeamName(): String {
        return team!!.teamName
    }

    /**
     * getTeam
     */
    fun getTeam(): Team {
        return team!!
    }

    /**
     * teamExists
     */
    fun teamExists(): Team? {
        return team
    }

    /**
     * getPlayer
     */
    fun getPlayer(): Player {
        return player
    }

    /**
     * getPlayerId
     */
    fun getPlayerId(): Int {
        return player.id
    }

    /**
     * getPlayerId
     */
    fun getPlayerId(position: Int): Int {
        return team!!.getPlayers()[position].id
    }

    /**
     * getPlayerLocation
     */
    fun getPlayerLocation(position: Int): String {
        return String.format(
            "%.2f",
            team!!.getPlayers()[position].latitude
        ) + " \t " + String.format("%.2f", team!!.getPlayers()[position].longitude)
    }

    /**
     * setStateAsStart
     */
    fun setStateAsStart() {
        if (player.id == 1) {
            team!!.latitude = player.latitude
            team!!.longitude = player.longitude
        } else {
            team!!.latitude = team!!.getPlayers()[0].latitude
            team!!.longitude = team!!.getPlayers()[0].longitude
        }

        state.postValue(State.START)
    }

}