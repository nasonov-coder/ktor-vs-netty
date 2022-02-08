package com.example

import io.ktor.server.netty.*
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.util.CharsetUtil
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.Semaphore


class EchoClient(private val host: String, private val port: Int) {
    val semaphore = Semaphore(0).apply {
    }
    private lateinit var channel: Channel

    private fun start() {
        val group: EventLoopGroup = NioEventLoopGroup(1)
        try {
            val b = Bootstrap()
            b.group(group)
                .channel(NioSocketChannel::class.java)
                .remoteAddress(InetSocketAddress(host, port))
                .handler(object : ChannelInitializer<SocketChannel>() {
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                            .addLast(LengthFieldBasedFrameDecoder(16777218, 0, 4, 0, 4))
                            .addLast(LengthFieldPrepender(4))
                            .addLast(object:ChannelOutboundHandlerAdapter() {
                                override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
//                                    println("msg!! ${(msg as ByteBuf).toString(Charset.defaultCharset())}")
                                    ctx.writeAndFlush(msg).addListener {
//                                        println("client flush: ${it.isSuccess}")
                                    }
                                }
                            })
                            .addLast(EchoClientHandler(semaphore))
                    }
                })
            val f: ChannelFuture = b.connect().sync()
            channel = f.channel()!!
//            f.channel().closeFuture().sync()
        } finally {
//            group.shutdownGracefully().sync()
        }
    }

    fun write(string: String) {
        val ba = Unpooled.copiedBuffer(string, CharsetUtil.UTF_8)
//        println("writing:$string")
        channel.writeAndFlush(ba)
//        println("wrote")
    }

    companion object {
        fun create(host: String, port: Int) =
            EchoClient(host, port).apply {start()}
    }
}

@Sharable
class EchoClientHandler(val semaphore: Semaphore) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks ю!", CharsetUtil.UTF_8))
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        msg as ByteBuf
//        println("client got: ${msg.readCharSequence(msg.readableBytes(), Charset.defaultCharset())}")
        semaphore.release()
    }
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}