package pt.isec.amovtp2.geometrygo.data

import androidx.lifecycle.ViewModel
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class Player() : ViewModel() {
    internal var id : Int = 0
    internal var latitude : Double = 0.0
    internal var longitude : Double = 0.0

    constructor(id: Int, latitude: Double, longitude: Double) : this(){
         this.id = id
         this.latitude = latitude
         this.longitude = longitude
    }

    internal var serverSocket: ServerSocket? = null

    internal var threadCreateTeam: Thread? = null

    internal var socket: Socket? = null
    internal val oS: OutputStream?
        get() = socket?.getOutputStream()
    internal val iS: InputStream?
        get() = socket?.getInputStream()

}