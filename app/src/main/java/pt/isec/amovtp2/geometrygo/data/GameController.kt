package pt.isec.amovtp2.geometrygo.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.constants.*
import java.io.PrintStream
import java.lang.Math.toDegrees
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs

class GameController : ViewModel() {
    enum class State {
        STARTING_TEAM, NEW_PLAYER, UPDATE_VIEW, NOT_ENOUGH_PLAYERS, PLAYER_LEFT, READY_TO_PLAY,
        END_LOBBY, START, END_GAME_WIN, END_GAME_LOSE, ADD_BUTTON
    }

    // Actual player.
    private lateinit var player: Player

    // Team will all the players.
    private var team: Team? = null

    // Actual game state.
    val state = MutableLiveData(State.STARTING_TEAM)

    // Checks if the game already started or not.
    private var gameStarted = false

    // Start game date and time.
    private lateinit var startDateTime: Date

    // String to display what why the player lose the game.
    private lateinit var loseInformation: String

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
                try {
                    player.serverSocket = ServerSocket(DataConstants.SERVER_DEFAULT_PORT)
                    while (player.serverSocket != null) {
                        player.serverSocket.apply {
                            state.postValue(State.NOT_ENOUGH_PLAYERS)
                            receiveMessagesFromClients(player.serverSocket!!.accept())
                        }
                    }
                }catch (_: Exception) {
                    return@thread
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
        longitude: Double,
    ) {
        createEmptyTeamName()
        player = Player(-1, latitude, longitude)
        try {
            thread {
                val newSocket = Socket(serverIP, serverPort)
                player.socket = newSocket
                connectToServer(player)
                team!!.addPlayer(player)
                receiveDataFromServer()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * receiveMessagesFromClients
     * Server receives the data of a client.
     */
    private fun receiveMessagesFromClients(socket: Socket) {
        val tempPlayer = Player()
        tempPlayer.socket = socket

        tempPlayer.threadCreateTeam = thread {
            try {
                if (tempPlayer.iS == null)
                    return@thread

                val iS = tempPlayer.iS!!.bufferedReader()

                while (state.value != State.START) {

                    val newPlayerInfo = iS.readLine()

                    if (!gameStarted)
                        checkIfGameCanStart()

                    if (newPlayerInfo.split(" ")[1] == MessagesStatusConstants.EXIT_MESSAGE) {
                        if (team!!.removePlayer(team!!.getPlayerById(newPlayerInfo.split(" ")[0].toInt()))) {
                            state.postValue(State.UPDATE_VIEW)
                        } else {
                            Log.e(
                                ErrorConstants.RECEIVE_MESSAGE_FROM_CLIENT_TAG,
                                "There's no client with the id ${newPlayerInfo.split(" ")[0].toInt()}"
                            )
                        }
                    } else {
                        handlePlayerData(newPlayerInfo, tempPlayer)
                    }

                    // Sort the players array.
                    team!!.getPlayers().sortBy { it.id }
                }
            } catch (_: Exception) {
                //deleteLobby()
                return@thread
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
            tempPlayer.id = team!!.getPlayers()[team!!.getSize() - 1].id + 1
            tempPlayer.latitude = newPlayerInfo.split(" ")[1].toDouble()
            tempPlayer.longitude = newPlayerInfo.split(" ")[2].toDouble()

            team!!.addPlayer(tempPlayer)

            state.postValue(State.NEW_PLAYER)

            var message = team!!.teamName + " "
            message += if (state.value == State.READY_TO_PLAY || state.value == State.START) {
                MessagesStatusConstants.START_STATE
            } else if (state.value == State.END_LOBBY) {
                MessagesStatusConstants.END_STATE
            } else {
                MessagesStatusConstants.STANDBY_STATE
            }
            message += " " + "1" + " "
            message += tempPlayer.id.toString() + " " + tempPlayer.latitude + " " + tempPlayer.longitude

            tempPlayer.oS.run {
                try {
                    thread {
                        val printStream = PrintStream(this)
                        printStream.println(message)
                        printStream.flush()
                    }
                } catch (_: Exception) {
                    //stopGame()
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

                    when {
                        newPlayersInfo.split(" ")[1] == MessagesStatusConstants.END_STATE -> {
                            state.postValue(State.END_LOBBY)
                        }
                        newPlayersInfo.split(" ")[1] == MessagesStatusConstants.START_STATE -> {
                            startDateTime =
                                UtilsFunctions.convertToDate(newPlayersInfo.split(" ")[2])

                            setStateAsStart()
                        }
                        else -> {
                            val teamName = newPlayersInfo.split(" ")[0]
                            if (teamName != "")
                                team!!.teamName = teamName

                            val nrPlayers = newPlayersInfo.split(" ")[2].toInt()

                            handlePlayersInfo(nrPlayers, newPlayersInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(ErrorConstants.RECEIVE_DATA_FROM_SERVER_TAG, "e: $e")
            }
        }
    }

    /**
     * handlePlayersInfo
     * Handles the data of each player received from the server.
     */
    private fun handlePlayersInfo(nrPlayers: Int, newPlayersInfo: String) {
        // Ids that came from server
        val listIds = arrayListOf<Int>()

        for (i in 3 until nrPlayers * 3 + 3 step 3) {

            val id = newPlayersInfo.split(" ")[i].toInt()
            listIds.add(id)

            if (id != -1) {
                if (id == 1) { // If it's server.
                    if (!team!!.containsPlayerById(1)) {
                        team!!.addPlayer(
                            id,
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
                                id,
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
     * Removes clients not received from server from the lobby.
     */
    private fun removeClientsThatLeftLobby(listIds: ArrayList<Int>) {
        // Ids that are in the player's storage data but were not received from the server.
        val listClientsToRemove =
            arrayListOf<Int>()
        var flagDelete = true

        team!!.getPlayers().forEach {
            listIds.forEach { iterator ->
                if (it.id == iterator)
                    flagDelete = false
            }

            if (flagDelete) {
                listClientsToRemove.add(it.id)
            } else {
                flagDelete = true
            }
        }

        synchronized(team!!) {
            listClientsToRemove.forEach {
                if (team!!.removePlayer(team!!.getPlayerById(it))) {
                    state.postValue(State.UPDATE_VIEW)
                } else {
                    Log.e(ErrorConstants.REMOVE_CLIENTS_TAG, "There's no client with the id $it")
                }
            }
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
                if (it.id != player.id)
                    it.oS?.run {
                        try {
                            thread {
                                val printStream = PrintStream(this)
                                printStream.println(message)
                                printStream.flush()
                            }
                        } catch (_: Exception) {
                        //stopGame()
                        }
                    }
                //state.postValue(State.UPDATE_VIEW)
            }
        }
    }

    private fun createMessageForClients(): String {
        var message = team!!.teamName + " "
        message += if (state.value == State.READY_TO_PLAY || state.value == State.START) {
            MessagesStatusConstants.START_STATE
        } else if (state.value == State.END_LOBBY) {
            MessagesStatusConstants.END_STATE
        } else {
            MessagesStatusConstants.STANDBY_STATE
        }
        message += " " + team?.getPlayers()!!.size.toString() + " "
        team?.getPlayers()?.forEach { iterator ->
            if (iterator.id == -1) {
                if (team!!.isLastPlayer(iterator.id))
                    message += " "
            } else {
                message += if (team!!.isLastPlayer(iterator.id))
                    iterator.id.toString() + " " + "${iterator.latitude}" + " " + "${iterator.longitude}"
                else
                    iterator.id.toString() + " " + "${iterator.latitude}" + " " + "${iterator.longitude}" + " "
            }
        }

        return message
    }

    private fun connectToServer(player: Player) {
        player.oS?.run {
            try {
                thread {
                    val printStream = PrintStream(this)
                    printStream.println(
                        player.id.toString() + " " + player.latitude.toString() + " "
                                + player.longitude.toString() + " " + team!!.teamName
                    )
                    printStream.flush()
                }
            } catch (_: Exception) {
                //stopGame()
            }
        }
    }

    /**
     * sendLocationToServer
     * Client ad_cc_btn_send it's data to the server.
     */
    fun sendLocationToServer(latitude: Double, longitude: Double) {
        if (team == null) return

        player.latitude = latitude
        player.longitude = longitude

        synchronized(team!!) {
            player.oS?.run {
                try {
                    thread {
                        val printStream = PrintStream(this)
                        printStream.println(
                            player.id.toString() + " "
                                    + "$latitude $longitude ${team!!.teamName}"
                        )
                        printStream.flush()
                    }
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
        state.postValue(State.UPDATE_VIEW)
    }

    fun serverChangeStatus(status: String) {
        var message: String? = null
        if (status == MessagesStatusConstants.END_STATE) {
            state.postValue(State.END_LOBBY)
            message = team!!.teamName + " " + status + " 0"
        } else if (status == MessagesStatusConstants.START_STATE) {
            state.postValue(State.START)
            message =
                team!!.teamName + " " + status + " " + UtilsFunctions.convertDateToStr(startDateTime)
        }

        if (message == null) return

        synchronized(team!!) {
            team?.getPlayers()?.forEach {
                if (it.id != player.id)
                    it.oS?.run {
                        try {
                            thread {
                                val printStream = PrintStream(this)
                                printStream.println(message)
                                printStream.flush()
                            }
                        } catch (_: Exception) {
                            //stopGame()
                        }
                    }
            }
        }
    }

    fun clientExitLobby() {
        synchronized(team!!) {
            player.oS.run {
                try {
                    thread {
                        val printStream = PrintStream(this)
                        printStream.println(
                            player.id.toString()
                                    + " " + MessagesStatusConstants.EXIT_MESSAGE
                        )
                        printStream.flush()
                    }
                } catch (_: Exception) {
                    //stopGame()
                }
            }
        }
        state.postValue(State.PLAYER_LEFT)
    }

    private fun checkIfGameCanStart() {
        if (team!!.getSize() >= DataConstants.MIN_PLAYERS && team!!.checkPlayersDistance())
            state.postValue(State.READY_TO_PLAY)
    }

    /**
     * setStateAsStart
     */
    private fun setStateAsStart() {
        if (player.id == 1) {
            team!!.latitude = player.latitude
            team!!.longitude = player.longitude
            startDateTime = Date()
        } else {
            team!!.latitude = team!!.getPlayerById(1)!!.latitude
            team!!.longitude = team!!.getPlayerById(1)!!.longitude
        }

        state.postValue(State.START)
        gameStarted = true
    }

    fun startGame() {
        setStateAsStart()
        serverChangeStatus(MessagesStatusConstants.START_STATE)
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

    fun playerExists(): Boolean {
        return this::player.isInitialized
    }

    fun getPlayerPosition(playerId: Int): LatLng? {
        return team!!.getPlayerPosition(playerId)
    }

    fun closeAllConnections() {
        player.serverSocket?.close()
        player.socket?.close()

        team!!.closeConnections()
    }

    fun createDatabase() {
        val db = Firebase.firestore

        team!!.getPlayers().forEach {
            val playerData = hashMapOf(
                FirebaseConstants.LATITUDE_FIELD to it.latitude,
                FirebaseConstants.LONGITUDE_FIELD to it.longitude,
                FirebaseConstants.LAST_CONNECTION_FIELD to Timestamp(Date()),
                FirebaseConstants.CONFIRMED_END to false
            )

            db.collection(FirebaseConstants.PLAYER_LOCATION_COLLECTION)
                .document(team!!.teamName)
                .collection(
                    FirebaseConstants.PLAYERS_COLLECTION_PATH + " " + UtilsFunctions.convertDateToStr(
                        startDateTime
                    )
                )
                .document(it.id.toString()).set(playerData)
        }
        state.postValue(State.UPDATE_VIEW)
    }

    fun canSaveData(latitude: Double, longitude: Double) {
        // Update all data.
        if (player.latitude != latitude || player.longitude != longitude) {
            player.latitude = latitude
            player.longitude = longitude

            saveDataInDatabase()
        } else // Update only the time for the last connection.
            updateTimeInDatabase()
    }

    private fun updateTimeInDatabase() {
        val db = Firebase.firestore

        val v = db.collection(FirebaseConstants.PLAYER_LOCATION_COLLECTION)
            .document(team!!.teamName)
            .collection(
                FirebaseConstants.PLAYERS_COLLECTION_PATH + " " + UtilsFunctions.convertDateToStr(
                    startDateTime
                )
            )
            .document(player.id.toString())

        // Saves the last connection in the database.
        db.runTransaction { transition ->
            transition.update(v, FirebaseConstants.LAST_CONNECTION_FIELD, Timestamp(Date()))
            null
        }
    }

    private fun saveDataInDatabase() {
        val db = Firebase.firestore

        val v = db.collection(FirebaseConstants.PLAYER_LOCATION_COLLECTION)
            .document(team!!.teamName)
            .collection(
                FirebaseConstants.PLAYERS_COLLECTION_PATH + " " + UtilsFunctions.convertDateToStr(
                    startDateTime
                )
            )
            .document(player.id.toString())

        // Saves the data in the database.
        db.runTransaction { transition ->
            transition.update(v, FirebaseConstants.LATITUDE_FIELD, player.latitude)
            transition.update(v, FirebaseConstants.LONGITUDE_FIELD, player.longitude)
            transition.update(v, FirebaseConstants.LAST_CONNECTION_FIELD, Timestamp(Date()))
            transition.update(v, FirebaseConstants.CONFIRMED_END, player.endConfirmation)
            null
        }
    }

    fun readDataFromDatabase(context: Context) {
        state.postValue(State.UPDATE_VIEW)
        var end: Int
        GlobalScope.launch {
            val db = Firebase.firestore
            while (state.value != State.END_GAME_WIN || state.value != State.END_GAME_LOSE) {
                end = 1
                val toRemove = arrayListOf<Player>()

                team!!.getPlayers().forEach {
                    db.collection(FirebaseConstants.PLAYER_LOCATION_COLLECTION)
                        .document(team!!.teamName)
                        .collection(
                            FirebaseConstants.PLAYERS_COLLECTION_PATH + " " + UtilsFunctions.convertDateToStr(
                                startDateTime
                            )
                        )
                        .document(it.id.toString())
                        .addSnapshotListener { doc, e ->
                            if (e != null)
                                return@addSnapshotListener

                            // Gets all the information from the firebase.
                            if (doc != null && doc.exists()) {
                                val latitude = doc.getDouble(FirebaseConstants.LATITUDE_FIELD)
                                val longitude = doc.getDouble(FirebaseConstants.LONGITUDE_FIELD)
                                val connectionDate =
                                    doc.getTimestamp(FirebaseConstants.LAST_CONNECTION_FIELD)
                                val endConfirmation =
                                    doc.getBoolean(FirebaseConstants.CONFIRMED_END)

                                if (endConfirmation == true)
                                    end += 1
                                else
                                    end = 1

                                if (latitude != null && longitude != null && connectionDate != null) {
                                    val playerToRemove = team!!.setPlayerLocation(
                                        it.id,
                                        latitude,
                                        longitude,
                                        connectionDate,
                                    )

                                    if (player.id == it.id) {
                                        player.latitude = latitude
                                        player.longitude = longitude
                                        player.endConfirmation = endConfirmation ?: false
                                    }

                                    if (playerToRemove != null)
                                        toRemove.add(playerToRemove)

                                }
                            }
                        }
                }

                synchronized(team!!.getPlayers()) {
                    // Remove players if needed.
                    if (toRemove.isNotEmpty()) {
                        team!!.removePlayers(toRemove)
                    }
                    // TODO: SE FOR EU TERMINAR O JOGO
                }

                when {
                    // If the team has less than 3 members.
                    team!!.getSize() < DataConstants.MIN_PLAYERS -> {
                        loseInformation = context.getString(R.string.not_enough_players_to_finish_the_game_information)
                        state.postValue(State.END_GAME_LOSE)
                        return@launch
                    }
                    // If all the players confirm the intention to end the game.
                    end == team!!.getSize() -> {
                        state.postValue(State.END_GAME_WIN)
                        return@launch
                    }
                    // If the polygon angles are equal.
                    isPolygonRegular() -> state.postValue(State.ADD_BUTTON)
                    else -> state.postValue(State.UPDATE_VIEW)
                }
                end = 1

                delay(DataConstants.DELAY_BETWEEN_SENDING_DATA.toLong())
            }
        }

        // TODO TRATAR DO FIM DO JOGO
    }

    private fun isPolygonRegular(): Boolean {
        // Every angle: (n−2) × 180° / n
        val angle = (team!!.getSize() - 2) * 180 / team!!.getSize()
        val error = angle.toDouble() * DataConstants.ERROR_FACTOR
        var count = 0

        team!!.getPlayers().forEach {
            when (it.id) {
                // First player.
                1 -> {
                    val thisAngle = toDegrees(
                        UtilsFunctions.calculateAngle(it, team!!.getNextPlayer(1)) -
                                UtilsFunctions.calculateAngle(it, team!!.getLastPlayer())
                    )

                    val finalAngle = UtilsFunctions.convertToFirstQuadrant(abs(thisAngle))

                    Log.i(
                        "TAG",
                        "isPolygonRegular: first player: $finalAngle ${angle - error} $angle"
                    )

                    if (finalAngle > (angle - error) && finalAngle < (angle + error)) count += 1
                    else return false
                }
                // Last player.
                team!!.getLastPlayer().id -> {
                    val thisAngle = toDegrees(
                        UtilsFunctions.calculateAngle(it, team!!.getLeader()) -
                                UtilsFunctions.calculateAngle(it, team!!.getBeforePlayer(it.id))
                    )

                    val finalAngle = UtilsFunctions.convertToFirstQuadrant(abs(thisAngle))

                    Log.i(
                        "TAG",
                        "isPolygonRegular: last player: $finalAngle ${angle - error} $angle"
                    )

                    if (finalAngle > (angle - error) && finalAngle < (angle + error)) count += 1
                    else return false
                }
                // Others.
                else -> {
                    val thisAngle = toDegrees(
                        UtilsFunctions.calculateAngle(it, team!!.getNextPlayer(it.id)) -
                                UtilsFunctions.calculateAngle(it, team!!.getBeforePlayer(it.id))
                    )

                    val finalAngle = UtilsFunctions.convertToFirstQuadrant(abs(thisAngle))

                    Log.i("TAG", "isPolygonRegular: ${it.id} $finalAngle ${angle - error} $angle")

                    if (finalAngle > (angle - error) && finalAngle < (angle + error)) count += 1
                    else return false
                }
            }
        }
        return count == team!!.getSize()
    }

    fun confirmEnd() {
        val db = Firebase.firestore

        val v = db.collection(FirebaseConstants.PLAYER_LOCATION_COLLECTION)
            .document(team!!.teamName)
            .collection(
                FirebaseConstants.PLAYERS_COLLECTION_PATH + " " + UtilsFunctions.convertDateToStr(
                    startDateTime
                )
            )
            .document(player.id.toString())

        // Set end confirmation in the database.
        db.runTransaction { transition ->
            transition.update(v, FirebaseConstants.CONFIRMED_END, true)
            null
        }
    }

    fun endGame(context: Context) {
        loseInformation = context.getString(R.string.time_expired_information)
        state.postValue(State.END_GAME_LOSE)
    }

    fun getLoseInformation(): String {
        return loseInformation
    }
}