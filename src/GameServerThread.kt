import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock

class GameServerThread private constructor() : Runnable{
    companion object {
        val instance = GameServerThread()
    }

    private val connectedPlayers: MutableMap<Pair<InetAddress, Int>, Pair<RoomThread, Long>> = mutableMapOf()
    private val threads: MutableList<RoomThread> = mutableListOf()
    private val threadPool: ExecutorService = Executors.newFixedThreadPool(4)
    private val playersLock = ReentrantReadWriteLock()

    override fun run() {
        val socket = DatagramSocket(GameServerPort)
        println("[INFO]: Game server started on port $GameServerPort")
        socket.soTimeout = Int.MAX_VALUE;
        while (true) {
            val datagram = DatagramPacket(ByteArray(DATAGRAM_SIZE), DATAGRAM_SIZE)
            socket.receive(datagram)

            threadPool.execute {
                var playerPair: Pair<RoomThread, Long>? = null
                playersLock.readSynchronized {
                    playerPair = connectedPlayers[Pair(datagram.address, datagram.port)]
                    playerPair?.first?.datagramQueue?.add(datagram)
                }
                if(playerPair == null){
                    val msg = String(datagram.data, 0, datagram.length, StandardCharsets.US_ASCII).split(' ')
                    if(msg.getOrNull(0) == "Connect") {
                        val roomID = msg.getOrNull(1)?.toLongOrNull()
                        if(roomID != null) {
                            val thread = synchronized(threads) {
                                threads.find {
                                    it.room.id == roomID;
                                }
                            }
                            if(thread != null) {
                                playersLock.writeSynchronized {
                                    connectedPlayers.put(
                                        Pair(datagram.address, datagram.port),
                                        Pair(thread, System.currentTimeMillis())
                                    )
                                }
                            }
                            println("[INFO]: Client ${datagram.address}:${datagram.port} connected to room $roomID")
                            val response = DatagramPacket("Ack".toByteArray(StandardCharsets.US_ASCII), 3, datagram.address, datagram.port)
                            socket.send(response)
                        }
                    }
                }
            }
        }
    }

    fun createRoom(room: Room){
        val thread = RoomThread(room)
        thread.start()
        threads.add(thread)
    }
}

class RoomThread(val room: Room): Thread() {
    val datagramQueue: Queue<DatagramPacket> = ConcurrentLinkedQueue()
    val players: MutableList<GameObject> = mutableListOf()
    override fun run() {
        for (i in 0 until room.playersCapacity){
            players.add(GameObject(i, Vector2.zero, Vector2.zero))
        }
    }
}

data class Vector2(val x: Float, val y: Float){
    companion object{
        val zero = Vector2(0f, 0f)
    }
}
data class GameObject(val id: Int, val position: Vector2, val velocity: Vector2)