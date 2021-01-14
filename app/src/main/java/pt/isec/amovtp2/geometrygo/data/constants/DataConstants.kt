package pt.isec.amovtp2.geometrygo.data.constants

object DataConstants {

    // Team name max size.
    const val TEAM_NAME_SIZE = 25

    // Minimum number of players to start a game.
    const val MIN_PLAYERS = 3

    // Server port for client connections.
    const val SERVER_DEFAULT_PORT = 9000

    // Max distance between all players in the lobby.
    const val MAX_DISTANCE_BETWEEN_PLAYERS = 100

    // Wait time to ad_cc_btn_send data to the database, in milliseconds (30 seconds).
    const val DELAY_BETWEEN_SENDING_DATA = 30000

    // Max time without sending information to the database, in minutes.
    const val MAX_TIME_WITHOUT_COMMUNICATION = 5

    // Game time, in milliseconds (60 minutes)
    const val GAME_TIME = 3600000

    // Error factor, 2%.
    const val ERROR_FACTOR = 0.02
}