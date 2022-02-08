package com.example

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.engine.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.util.concurrent.Executors

fun ktor(port: Int) {
    runBlocking {
        val kek = Executors.newFixedThreadPool(8).asCoroutineDispatcher()
        val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))
//        println("Started echo telnet server at ${server.localAddress}")
        supervisorScope {
            while (true) {
                val socket = server.accept()
                    val channel = Channel<Pair<Int, String>>()
//                    println("Socket accepted: ${socket.remoteAddress}")

                    val input = socket.openReadChannel()
                    val output = socket.openWriteChannel(autoFlush = true)
                    launch(kek) {
                        while (true) {
                            val size = input.readInt()
                            val packet = input.readPacket(size)
                            val line = packet.readText()
                            channel.send(size to line)
                        }
                    }
                    launch(kek) {
                        while (true) {
                            val (size, line) = channel.receive()
                            Computation.exec(line)
//                        println("server got: $line")
                            output.writeInt(size)
                            output.writeStringUtf8(line)
                            output.flush()
                        }

                }.invokeOnCompletion {
                    println("job done: ${it?.stackTraceToString()}")
                }
            }
        }
    }
}
