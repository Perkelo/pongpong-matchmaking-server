import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

const val DEBUG = true
const val port = 7878

enum class MessageType(var prefix: String){
    ServerConnect("SC"),
    ServerDisconnect("SD"),
    GetServers("GS"),
    InvalidMessage("")
}

private val servers: MutableList<Pair<String, Int>> = listOf<Pair<String, Int>>().toMutableList()

fun main(){
    val serverSocket = ServerSocket(port)
    println("[INFO] Server started on port $port")
    while(true){
       val clientSocket = serverSocket.accept()
       thread {
           val msg = BufferedReader(InputStreamReader(clientSocket.getInputStream())).readLine()
           println("Received: $msg")

           val msgType: MessageType = when{
               msg.startsWith(MessageType.ServerConnect.prefix) -> MessageType.ServerConnect
               msg.startsWith(MessageType.ServerDisconnect.prefix) -> MessageType.ServerDisconnect
               msg.startsWith(MessageType.GetServers.prefix) -> MessageType.GetServers
               else -> MessageType.InvalidMessage
           }

           when(msgType) {
               MessageType.ServerConnect, MessageType.ServerDisconnect -> {
                   val ipAddress = msg.substring(msgType.prefix.length, msg.indexOf("P"))
                   val port = msg.substring(msg.indexOf("P") + 1).toInt()

                   if (DEBUG) {
                       println("Type: $msgType")
                       println("Address: $ipAddress")
                       println("Port: $port")
                   }

                   when(msgType){
                       MessageType.ServerConnect -> {
                           servers.add(Pair(ipAddress, port))
                       }

                       MessageType.ServerDisconnect -> {
                           servers.removeIf {
                               it.first == ipAddress && it.second == port
                           }
                       }

                       else -> Unit
                   }

                   with(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))) {
                       write("0")
                       flush()
                   }
               }

               MessageType.GetServers -> {
                   val response = JSON.toJSON(servers) {
                       "{\"IP\":\"${it.first}\",\"port\":\"${it.second}\"}"
                   }
                   if (DEBUG) {
                       println("Type: $msgType")
                       println("Response: $response")
                   }
                   with(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))) {
                       write(response)
                       flush()
                   }
               }

               else -> Unit
           }
           clientSocket.close()
           println()
       }
   }
}