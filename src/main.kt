import com.google.gson.GsonBuilder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.thread

const val DATAGRAM_SIZE: Int = 32

class Room(val id: Long, val name: String, val playersCapacity: Short, val playersCurrent: Short)

enum class RoomActions(val value: String){
    Create("Create"),
    List("List")
}

class RoomThread: Runnable{
    private val port: Int = 56567
    val rooms: MutableList<Room> = mutableListOf()
    private val roomListLock: ReadWriteLock = ReentrantReadWriteLock()
    private var uniqueID = 0
    private val startTime = System.nanoTime()

    override fun run() {
        val socket = DatagramSocket(port)
        println("[INFO]: Room server started on port $port")
        socket.soTimeout = Int.MAX_VALUE;

        val gson = GsonBuilder().create()

        while (true){
            val datagram = DatagramPacket(ByteArray(DATAGRAM_SIZE), DATAGRAM_SIZE)
            socket.receive(datagram)
            thread {
                val msg = String(datagram.data, 0, datagram.length, StandardCharsets.US_ASCII)
                println("[DEBUG]: Received \"$msg\"")

                when(msg){
                    RoomActions.Create.value -> {
                        var id: Long = -1
                        roomListLock.writeSynchronized {
                            id = getUniqueId()
                            val newRoom = Room(id, "", 4, 0)
                            rooms.add(newRoom)
                        }
                        val responseString = id.toString()
                        val response = DatagramPacket(responseString.toByteArray(StandardCharsets.US_ASCII), responseString.length, datagram.address, datagram.port)
                        socket.send(response)
                        socket.send(response)
                        socket.send(response)
                        socket.send(response)
                        println("[DEBUG]: Replied with $id")
                    }

                    RoomActions.List.value -> {
                        roomListLock.readSynchronized {
                            val json = gson.toJson(rooms)
                            println(json)
                            val response = DatagramPacket(json.toByteArray(StandardCharsets.US_ASCII), json.length, datagram.address, datagram.port)
                            socket.send(response)
                            println("[DEBUG]: Replied with $json")
                            println("[DEBUG]: Length = ${json.length}")
                        }
                    }
                }
            }
        }
    }

    private fun getUniqueId(): Long{
        return startTime + uniqueID++;
    }
}

fun ReadWriteLock.readSynchronized(func: () -> Unit){
    this.readLock().lock()
    func()
    this.readLock().unlock()
}

fun ReadWriteLock.writeSynchronized(func: () -> Unit){
    this.writeLock().lock()
    func()
    this.writeLock().unlock()
}

fun main(){
    RoomThread().run()
}