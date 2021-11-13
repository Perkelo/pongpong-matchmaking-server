import java.util.concurrent.locks.ReadWriteLock

const val DATAGRAM_SIZE: Int = 32
const val GameServerHost: String = "mc.9amgang.ga"
const val GameServerPort: Int = 56566
const val RoomServerPort: Int = 56567

class Room(val id: Long, val name: String, val playersCapacity: Short, val playersCurrent: Short)

enum class RoomActions(val value: String){
    Create("Create"),
    List("List")
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
    Thread(MatchmakingThread()).start()
    Thread(GameServerThread.instance).start()
}