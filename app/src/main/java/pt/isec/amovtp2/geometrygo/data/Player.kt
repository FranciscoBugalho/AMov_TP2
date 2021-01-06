package pt.isec.amovtp2.geometrygo.data

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class Player(internal val id: Int, internal var latitude: Double, internal var longitude: Double) {

    internal var serverSocket: ServerSocket? = null

    internal var threadCreateTeam: Thread? = null

    internal var socket: Socket? = null
    internal val oOS: OutputStream?
        get() = socket?.getOutputStream()
    internal val oIS: InputStream?
        get() = socket?.getInputStream()
}