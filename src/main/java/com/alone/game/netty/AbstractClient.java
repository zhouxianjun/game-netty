package com.alone.game.netty;

import com.alone.game.netty.codec.BasicEncoderHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/15 15:56
 */
@Slf4j
public abstract class AbstractClient {
    protected String ip;
    protected int port;
    @Setter
    protected Boolean reconnect = false;
    protected String name;
    protected Integer readTimeOut = 60;
    protected Integer writerTimeOut = 60;
    protected boolean stateChange = false;
    protected LogLevel logLevel(){return LogLevel.DEBUG;}
    private Bootstrap bootstrap;
    protected Channel channel;
    private ChannelGroup allChannels;

    public AbstractClient(String ip, int port, String name) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        init();
    }

    private void init(){
        EventLoopGroup workerGroup = new NioEventLoopGroup(2,
                DefaultThreadFactory.newThreadFactory(getName() + "[ip=" + ip + ",port=" + port + "]_THREAD_"));

        try {
            bootstrap = new Bootstrap();

            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.MAX_MESSAGES_PER_READ, 4096)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline channelPipeline = ch.pipeline();
                            channelPipeline.addLast(new LoggingHandler(logLevel()));
                            if (stateChange){
                                channelPipeline.addLast("idleStateHandler", new IdleStateHandler(readTimeOut, writerTimeOut, 0));
                            }
                            addLast(channelPipeline);
                            channelPipeline.addLast(getDecoderHandler()).addLast(getEncoderHandler());
                        }
                    });
            log.info("初始化{}连接客户端【IP={},Port={}】", getName(), ip, port);
        } catch (Exception e) {
            log.error("客户端初始化异常", e);
        }
    }

    public synchronized void connect(){
        try {
            if (isAvailable()) {
                return;
            }
            ChannelFuture connect = bootstrap.connect(ip, port).sync();
            connect.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("连接上服务器【IP={},Port={}】", ip, port);
                    channel = future.channel();
                    connected(channel);
                } else {
                    if (reconnect) {
                        future.channel().eventLoop().schedule(() -> {
                            log.info("开始重连服务器【IP={},PORT={}】", ip, port);
                            connect();
                        }, 3, TimeUnit.SECONDS);
                    } else {
                        log.warn("连接服务器【IP={},Port={}】失败!", ip, port);
                    }
                }
            });
        } catch (Exception e) {
            log.error("连接服务器异常", e);
        }

    }

    protected abstract void connected(Channel channel);

    protected String getName(){
        this.name =  this.name == null ? "Client-" + UUID.randomUUID().toString() : this.name;
        return this.name;
    }

    public boolean isAvailable() {
        if(channel == null || !channel.isActive()) {
            return false;
        }
        return true;
    }

    public void writer(Object o){
        channel.writeAndFlush(o);
    }

    public void shutdown() {
        if(channel != null) {
            log.info("关闭与服务器【ip={}，port={}】的连接", ip, port);
            channel.disconnect();
            channel.close();
            channel.eventLoop().shutdownGracefully();
        }
    }

    public ChannelGroup getAllChannels(){
        if (allChannels == null){
            allChannels = new DefaultChannelGroup(new DefaultEventExecutorGroup(1).next());
        }
        return allChannels;
    }
    protected void addLast(ChannelPipeline pipeline) {}
    protected abstract ChannelHandler getDecoderHandler();
    protected ChannelHandler getEncoderHandler(){
        return new BasicEncoderHandler();
    }
}
