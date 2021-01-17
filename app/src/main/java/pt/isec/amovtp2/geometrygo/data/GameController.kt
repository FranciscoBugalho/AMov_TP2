package pt.isec.amovtp2.geometrygo.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil.computeArea
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
import kotlin.collections.HashMap
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
                } catch (_: Exception) {
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
                            player.id = -1
                        }
                        newPlayersInfo.split(" ")[1] == MessagesStatusConstants.START_STATE -> {
                            startDateTime =
                                UtilsFunctions.convertToDate(newPlayersInfo.split(" ")[2])

                            team!!.identifier = newPlayersInfo.split(" ")[3]

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
            team!!.getPlayers().clear()
            team!!.addPlayer(player)
            player.serverSocket?.close()
            player.serverSocket = null
            player.threadCreateTeam?.interrupt()
            player.threadCreateTeam = null
            player.socket?.close()
            player.socket = null
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
                        }
                    }
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
                team!!.teamName + " " + status + " " + UtilsFunctions.convertDateToStr(startDateTime) + " " + team!!.identifier
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

        if (status == MessagesStatusConstants.END_STATE)
            deleteLobby()
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
                    player.id = -1
                    team!!.getPlayers().clear()
                    team!!.addPlayer(player)
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
            team!!.generateTeamIdentifier(startDateTime)
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
                FirebaseConstants.CONFIRMED_END to false,
                FirebaseConstants.IS_REMOVED to false,
            )

            db.collection(team!!.identifier).document(it.id.toString()).set(playerData)

        }
        state.postValue(State.UPDATE_VIEW)
    }

    fun canSaveData(latitude: Double, longitude: Double) {
        // Update all data.
        if (player.latitude != latitude || player.longitude != longitude) {
            player.latitude = latitude
            player.longitude = longitude

            saveDataInDatabase()
        }
    }

    private fun saveDataInDatabase() {
        val db = Firebase.firestore

        val v = db.collection(team!!.identifier).document(player.id.toString())

        // Saves the data in the database.
        db.runTransaction { transition ->
            transition.update(v, FirebaseConstants.LATITUDE_FIELD, player.latitude)
            transition.update(v, FirebaseConstants.LONGITUDE_FIELD, player.longitude)
            transition.update(v, FirebaseConstants.LAST_CONNECTION_FIELD, Timestamp(Date()))
            transition.update(v, FirebaseConstants.CONFIRMED_END, player.endConfirmation)
            transition.update(v, FirebaseConstants.IS_REMOVED, false)
            null
        }
    }

    fun readDataFromDatabase(context: Context) {
        state.postValue(State.UPDATE_VIEW)

        val db = Firebase.firestore
        var end: Int
        val toRemove = arrayListOf<Player>()

        GlobalScope.launch {
            team!!.getPlayers().forEach {
                db.collection(team!!.identifier).document(it.id.toString())
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
                            val isRemoved =
                                doc.getBoolean(FirebaseConstants.IS_REMOVED)

                            if (latitude != null && longitude != null && connectionDate != null && isRemoved != null) {
                                team!!.getPlayers().forEach { iterator ->
                                // If the player weren't removed.
                                    if (!isRemoved) {
                                        if (iterator.id == it.id) {
                                            iterator.latitude = latitude
                                            iterator.longitude = longitude
                                            iterator.endConfirmation = endConfirmation ?: false

                                            iterator.toRemove = team!!.setPlayerLocation(
                                                iterator.id,
                                                latitude,
                                                longitude,
                                                connectionDate,
                                            )

                                            if (iterator.toRemove)
                                                setAsRemoved(iterator.id)
                                        }
                                    } else {
                                        team!!.getPlayerById(it.id)?.toRemove = true
                                    }
                                }
                            }
                        }
                    }
            }

            while (state.value != State.END_GAME_WIN || state.value != State.END_GAME_LOSE) {
                end = 0

                team!!.getPlayers().forEach {
                    // Check the end confirmation.
                    if (it.endConfirmation)
                        end += 1
                    else
                        end = 1

                    if (it.toRemove)
                        toRemove.add(it)
                }

                synchronized(team!!.getPlayers()) {
                    // If it's the actual player changes the screen.
                    toRemove.forEach {
                        if (it.id == player.id)
                            state.postValue(State.END_LOBBY)
                    }
                    // Remove players if needed.
                    if (toRemove.isNotEmpty()) {
                        team!!.removePlayers(toRemove)
                    }
                }

                when {
                    // If the team has less than 3 members.
                    team!!.getSize() < DataConstants.MIN_PLAYERS -> {
                        loseInformation =
                            context.getString(R.string.not_enough_players_to_finish_the_game_information)
                        state.postValue(State.END_GAME_LOSE)
                    }
                    // If all the players confirm the intention to end the game.
                    end == team!!.getSize() -> {
                        state.postValue(State.END_GAME_WIN)
                    }
                    // If the polygon angles are equal.
                    isWin() -> state.postValue(State.ADD_BUTTON)
                    else -> state.postValue(State.UPDATE_VIEW)
                }

                delay(DataConstants.DELAY_BETWEEN_SENDING_DATA.toLong())
            }
        }
    }

    fun setAsRemoved(id: Int) {
        val db = Firebase.firestore

        val v = db.collection(team!!.identifier).document(id.toString())

        // Saves the data in the database.
        db.runTransaction { transition ->
            transition.update(v, FirebaseConstants.IS_REMOVED, true)
        }
    }

    private fun isWin(): Boolean {
        return isPolygonRegular() && polygonContainsPoint()
    }

    private fun polygonContainsPoint(): Boolean {
        val points = arrayListOf<LatLng>()
        for (i in 0 until team!!.getPlayers().size) {
            val position = team!!.getPlayerPosition(getPlayerId(i))
            if (position != null) {
                points.add(position)
            }
        }

        return PolyUtil.containsLocation(
            LatLng(team!!.latitude!!, team!!.longitude!!),
            points,
            true
        )
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
                        UtilsFunctions.calculateAngle(it, team!!.getFirst()) -
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

        val v = db.collection(team!!.identifier).document(player.id.toString())

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

    fun getPlayerAngle(playerId: Int): String {
        val actualPlayer = team!!.getPlayerById(playerId)
        when (playerId) {
            // First player.
            1 -> {
                val thisAngle = toDegrees(
                    UtilsFunctions.calculateAngle(actualPlayer!!, team!!.getNextPlayer(1)) -
                            UtilsFunctions.calculateAngle(actualPlayer, team!!.getLastPlayer())
                )

                return String.format("%.1f", UtilsFunctions.convertToFirstQuadrant(abs(thisAngle)))
            }
            // Last player.
            team!!.getLastPlayer().id -> {
                val thisAngle = toDegrees(
                    UtilsFunctions.calculateAngle(actualPlayer!!, team!!.getFirst()) -
                            UtilsFunctions.calculateAngle(
                                actualPlayer,
                                team!!.getBeforePlayer(actualPlayer.id)
                            )
                )

                return String.format("%.1f", UtilsFunctions.convertToFirstQuadrant(abs(thisAngle)))
            }
            // Others.
            else -> {
                val thisAngle = toDegrees(
                    UtilsFunctions.calculateAngle(
                        actualPlayer!!,
                        team!!.getNextPlayer(actualPlayer.id)
                    ) -
                            UtilsFunctions.calculateAngle(
                                actualPlayer,
                                team!!.getBeforePlayer(actualPlayer.id)
                            )
                )

                return String.format("%.1f", UtilsFunctions.convertToFirstQuadrant(abs(thisAngle)))
            }
        }
    }

    fun getPlayersDistance(): String {
        return team!!.getPlayersDistance()
    }

    fun calculateArea(): String {
        return String.format("%.2f", computeArea(team!!.getAllPlayersPosition()))
    }

    fun getPlayersAverageDistance(): String {
        return team!!.getPlayersAverageDistance()
    }
    /**
            identificador da equipa,
            nome da equipa,
            coordenadas finais de cada elemento,
            comprimento médio da aresta,
            área do polígono e
            data e hora em que foi alcançado
     */
    fun saveScores() {
        val polygnName = "POLYGN_" + team?.getSize()
        val db = Firebase.firestore

        val finalArea = computeArea(team!!.getAllPlayersPosition())

        val playersCoord = arrayListOf<String>()
        for (i in 0 until team!!.getPlayers().size) {
            playersCoord.add(team!!.getPlayers()[i].latitude.toString() +" "+team!!.getPlayers()[i].longitude.toString())
        }
        val score = Scores(team!!.getSize(),team!!.identifier,team!!.teamName, playersCoord ,getPlayersAverageDistance(),finalArea,Timestamp(Date()))


        db.collection("Polygns").document(polygnName).set(score, SetOptions.merge())

    }
}