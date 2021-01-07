package pt.isec.amovtp2.geometrygo.data

import java.io.*
import java.net.ServerSocket
import java.net.Socket

class Player(internal var id: Int, internal var latitude: Double, internal var longitude: Double) {

    internal var serverSocket: ServerSocket? = null

    internal var threadCreateTeam: Thread? = null

    internal var socket: Socket? = null
    internal val oS: OutputStream?
        get() = socket?.getOutputStream()
    internal val iS: InputStream?
        get() = socket?.getInputStream()

}