package pt.isec.amovtp2.geometrygo.data

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class Player(private val id: Int, private var latitude: Double, private var longitude: Double) {

    internal var serverSocket: ServerSocket? = null

    internal var threadCreateTeam: Thread? = null

    internal var socket: Socket? = null
    internal val oOS: OutputStream?
        get() = socket?.getOutputStream()
    internal val oIS: InputStream?
        get() = socket?.getInputStream()
}