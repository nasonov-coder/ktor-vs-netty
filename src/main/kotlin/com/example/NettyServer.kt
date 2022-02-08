package com.example

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.util.CharsetUtil
import java.net.InetSocketAddress

fun netty(port: Int) {
    EchoServer(port).start()
}
@Sharable
class EchoServerInboundHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val `in` = msg as ByteBuf
        val str = `in`.toString(CharsetUtil.UTF_8)
        Computation.exec(str)
//        println("Server received: $str")

        ctx.writeAndFlush(Unpooled.copiedBuffer(str, CharsetUtil.UTF_8)).addListener {
//            println("server flushed: ${it.isSuccess}")
        }

    }

//    override fun channelReadComplete(ctx: ChannelHandlerContext) {
//        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
//    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
class EchoServerOutboundHandler: ChannelOutboundHandlerAdapter() {
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
//        println("server outbound")
        ctx.writeAndFlush(msg as ByteBuf)
    }
}
class EchoServer(private val port: Int) {
    @Throws(Exception::class)
    fun start() {
        val group: EventLoopGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(group)
                .channel(NioServerSocketChannel::class.java)
                .localAddress(InetSocketAddress(port))
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socketChannel: SocketChannel) {
                        socketChannel.pipeline()
                            .addLast(LengthFieldBasedFrameDecoder(16777218, 0, 4, 0, 4))
                            .addLast(LengthFieldPrepender(4))
                            .addLast(EchoServerInboundHandler())
                            .addFirst(EchoServerOutboundHandler())
                    }
                })
            val f = b.bind().sync()
            f.channel().closeFuture().sync()
        } finally {
            group.shutdownGracefully().sync()
        }
    }
}