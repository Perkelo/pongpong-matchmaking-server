import com.google.gson.GsonBuilder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.thread

public class MatchmakingThread: Runnable{
    val rooms: MutableList<Room> = mutableListOf()
    private val roomListLock: ReadWriteLock = ReentrantReadWriteLock()
    private var uniqueID = 0
    private val startTime = System.nanoTime()

    override fun run() {
        val socket = DatagramSocket(RoomServerPort)
        println("[INFO]: Matchmaking server started on port $RoomServerPort")
        socket.soTimeout = Int.MAX_VALUE;

        val gson = GsonBuilder().create()

        val threadPool = Executors.newFixedThreadPool(2)

        while (true){
            val datagram = DatagramPacket(ByteArray(DATAGRAM_SIZE), DATAGRAM_SIZE)
            socket.receive(datagram)

            threadPool.execute {
                val msg = String(datagram.data, 0, datagram.length, StandardCharsets.US_ASCII)
                println("[DEBUG]: Received \"$msg\"")

                when {
                    msg.startsWith(RoomActions.Create.value) -> {
                        var id: Long = -1
                        val tokens = msg.split(' ')
                        if (tokens.size >= 3) {
                            val name = tokens[1]
                            val tmp = tokens[2].toShortOrNull() ?: 2
                            val playersCapacity = if (tmp > 4) 4 else if (tmp < 2) 2 else tmp

                            roomListLock.writeSynchronized {
                                id = getUniqueId()
                                val newRoom = Room(id, name, playersCapacity, 0)
                                rooms.add(newRoom)
                            }
                            val responseString = "$id $GameServerHost:$GameServerPort"
                            val response = DatagramPacket(
                                responseString.toByteArray(StandardCharsets.US_ASCII),
                                responseString.length,
                                datagram.address,
                                datagram.port
                            )
                            socket.send(response)
                            socket.send(response)
                            socket.send(response)
                            socket.send(response)
                            println("[DEBUG]: Replied with $responseString")
                        }
                    }

                    msg.startsWith(RoomActions.List.value) -> {
                        roomListLock.readSynchronized {
                            val json = gson.toJson(rooms)
                            println(json)
                            val response = DatagramPacket(
                                json.toByteArray(StandardCharsets.US_ASCII),
                                json.length,
                                datagram.address,
                                datagram.port
                            )
                            socket.send(response)
                            println("[DEBUG]: Replied with $json")
                            println("[DEBUG]: Length = ${json.length}")
                        }
                    }

                    else -> Unit //Do nothing if the message is not valid
                }
            }
        }
    }

    private fun getUniqueId(): Long{
        return startTime + uniqueID++;
    }
}