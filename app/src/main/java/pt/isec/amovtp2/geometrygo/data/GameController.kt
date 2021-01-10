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

    /**
     * startAsServer
     * Server initialization.
     * Starts the threat that receives messages.
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
     * Client initialization.
     * First communication to server.
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
                connectToServer(player)
                team!!.addPlayer(player)
                receiveDataFromServer()
            } catch (_: Exception) {
                //Log.i("TAG", "startAsClient: ")
            }
        }
    }

    /**
     * receiveMessagesFromClients
     * Server receives the data of a client.
     */
    private fun receiveMessagesFromClients(socket: Socket) {
        val tempPlayer  = Player()
        tempPlayer.socket = socket

        tempPlayer.threadCreateTeam = thread {
            try {
                if (tempPlayer.iS == null)
                    return@thread

                val iS = tempPlayer.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    // TODO: MUDAR ISTO DAQUI
                    if (team!!.getSize() >= 2 && team!!.checkPlayersDistance())
                        state.postValue(State.READY_TO_PLAY)
                    Log.i("receiveMessagesFromCli:", "newPlayerInfo: $newPlayerInfo")

                    if(newPlayerInfo.split(" ")[1] == "!exit"){
                        if(team!!.removePlayer(team!!.getPlayerById(newPlayerInfo.split(" ")[0].toInt()))){
                            Log.i("receiveMessagesFromCli:", "client with the id ${newPlayerInfo.split(" ")[0].toInt()} ghosted")
                            state.postValue(State.UPDATE_VIEW)
                        }
                        else{
                            Log.e("receiveMessagesFromCli:", "there's no client with the id ${newPlayerInfo.split(" ")[0].toInt()}")
                        }
                    }
                    else{
                        handlePlayerData(newPlayerInfo, tempPlayer)
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
     * handlePlayerData
     */
    private fun handlePlayerData(
        newPlayerInfo: String,
        tempPlayer: Player
    ) {
        val teamName = newPlayerInfo.split(" ")[3]
        if (teamName != "")
            team!!.teamName = teamName

        val id = newPlayerInfo.split(" ")[0].toInt()

        if (id == -1) { // Received new player.
            tempPlayer.id = team!!.getPlayers().size + 1
            tempPlayer.latitude = newPlayerInfo.split(" ")[1].toDouble()
            tempPlayer.longitude = newPlayerInfo.split(" ")[2].toDouble()
            team!!.addPlayer(
                tempPlayer
            )

            state.postValue(State.NEW_PLAYER)

            var message = team!!.teamName + " "
            message += if (state.value == State.READY_TO_PLAY || state.value == State.START) {
                "startState"
            } else if (state.value == State.END_LOBBY) {
                "endState"
            } else {
                "standbyState"
            }
            message += " " + "1" + " "
            message += tempPlayer.id.toString() + " " + tempPlayer.latitude + " " + tempPlayer.longitude


            tempPlayer.oS.run {
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
    }


    /**
     * receiveDataFromServer
     * Client receives the data of the team from the server.
     */
    private fun receiveDataFromServer() {
        player.threadCreateTeam = thread {
            try {
                if (player.iS == null)
                    return@thread

                val iS = player.iS!!.bufferedReader()

                while (state.value != State.START) {
                    val newPlayersInfo = iS.readLine()

                    val teamName = newPlayersInfo.split(" ")[0]
                    if (teamName != "")
                        team!!.teamName = teamName

                    //TODO: qq coisa com o status I guess
                    val status = newPlayersInfo.split(" ")[1]
                    val nrPlayers = newPlayersInfo.split(" ")[2].toInt()

                    // TODO: TROCAR ISTO PELO STATUS e chamar setStateAsStart()
                    //if (team!!.getSize() >= 2 && team!!.checkPlayersDistance())
                      //  state.postValue(State.START)

                    handlePlayersInfo(nrPlayers, newPlayersInfo)
                }
            } catch (e: Exception) {
                Log.i("receiveDataFromServer", "e: $e ")
            }
        }
    }

    /**
     * handlePlayersInfo
     * Handles the data of each player received from the server
     */
    private fun handlePlayersInfo(nrPlayers: Int, newPlayersInfo: String) {

        val listIds = arrayListOf<Int>() //ids that came from server

        for (i in 3 until nrPlayers*3 + 3 step 3) {

            val id = newPlayersInfo.split(" ")[i].toInt()
            listIds.add(id)

            if (id != -1) {
                if (id == 1) { // If it's server.
                    if (!team!!.containsPlayerById(1)) {
                        team!!.addPlayer(
                            team!!.getPlayers().size,
                            newPlayersInfo.split(" ")[1 + i].toDouble(),
                            newPlayersInfo.split(" ")[2 + i].toDouble()
                        )
                        // Sort the players array.
                        team!!.getPlayers().sortBy { it.id }

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
                            // Sort the players array.
                            team!!.getPlayers().sortBy { it.id }

                            state.postValue(State.NEW_PLAYER)
                        }
                    }
                }
            } else
                state.postValue(State.UPDATE_VIEW)
        }

        removeClientsThatLeftLobby(listIds)
    }

    /**
     * removeClientsThatLeftLobby
     * Removes clients not received from server from the lobby
     */
    private fun removeClientsThatLeftLobby(listIds: ArrayList<Int>) {
        var listClientsToRemove = arrayListOf<Int>() //ids that are in the player's storage data but were not received from the server
        var flagDelete = true

        team!!.getPlayers().forEach{
            listIds.forEach(){ iterator ->
                if(it.id == iterator)
                    flagDelete = false
            }

            if(flagDelete){
                listClientsToRemove.add(it.id)
            }
            else {
                flagDelete = true
            }
        }

        listClientsToRemove.forEach{
            team!!.removePlayer(team!!.getPlayerById(it))
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
     * Server sends the data from each player to the others.
     */
    fun sendLocationToTeam(latitude: Double, longitude: Double) {
        if (team == null) return

        player.latitude = latitude
        player.longitude = longitude

        val message = createMessageForClients()

        synchronized(team!!) {
            team?.getPlayers()?.forEach {
                if(it.id != player.id)
                    it.oS?.run {
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
                //state.postValue(State.UPDATE_VIEW)
            }
        }
    }

    private fun createMessageForClients(): String {
        var message = team!!.teamName + " "
        // Standby, Start, End
        message += if (state.value == State.READY_TO_PLAY || state.value == State.START) {
            "startState"
        } else if (state.value == State.END_LOBBY) {
            "endState"
        } else {
            "standbyState"
        }
        message += " " + team?.getPlayers()!!.size.toString() + " "
        team?.getPlayers()?.forEach { iterator ->
            if(team!!.isLastPlayer(iterator.id))
                message += iterator.id.toString() + " " + "${iterator.latitude}" + " " + "${iterator.longitude}"
            else
                message += iterator.id.toString() + " " + "${iterator.latitude}" + " " + "${iterator.longitude}" + " "
        }

        return message
    }

    private fun connectToServer(player: Player) {
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

    /**
     * sendLocationToServer
     * Client send it's data to the server.
     */
    fun sendLocationToServer(latitude: Double, longitude: Double) {
        if (team == null) return

        player.latitude = latitude
        player.longitude = longitude

        synchronized(team!!) {
            player.oS.run{
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
        state.postValue(State.UPDATE_VIEW)
    }

    /**
     * createTeam
     * Initializes team with a name.
     */
    fun createTeam(teamName: String) {
        team = Team(teamName)
    }

    /**
     * createEmptyTeamName
     * Initializes team without a name.
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
            team!!.latitude = team!!.getPlayerById(1)!!.latitude
            team!!.longitude = team!!.getPlayerById(1)!!.longitude
        }

        state.postValue(State.START)
    }

    fun serverExitLobby(){
        state.postValue(State.END_LOBBY)
        Log.i("serverExitLobby", "state: ${state.value} ")
    }

    fun clientExitLobby(){


        synchronized(team!!) {
            player.oS.run{
                thread {
                    try {
                        val printStream = PrintStream(this)
                        Log.i("sendLocationToServer", "${player.id} !exit")
                        printStream.println(player.id.toString() + " " + "!exit")
                        printStream.flush()
                    } catch (_: Exception) {
                        //stopGame()
                    }
                }
            }
        }
        state.postValue(State.PLAYER_LEFT)
    }

    fun playerExists(): Boolean {
        return this::player.isInitialized
    }

}