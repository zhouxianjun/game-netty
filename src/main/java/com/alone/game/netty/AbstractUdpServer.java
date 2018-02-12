package com.alone.game.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/16 14:58
 */
@Slf4j
public abstract class AbstractUdpServer {
    private Bootstrap bootstrap;
    @Getter
    private Channel channel;
    protected Integer readTimeOut(){
        return 60;
    }
    protected Integer writerTimeOut(){
        return 60;
    }
    protected int connectTimeOut() {return 10;}
    protected boolean stateChange(){return false;}
    protected LogLevel logLevel(){return LogLevel.DEBUG;}
    public AbstractUdpServer(int boss) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(boss,
                DefaultThreadFactory.newThreadFactory("NETTY_UDP_BOSS_THREAD_"));

        try {
            bootstrap = new Bootstrap();
            final boolean stateChange = this.stateChange();
            bootstrap.group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_RCVBUF, 2048)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(NioDatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (stateChange){
                                pipeline.addLast(new IdleStateHandler(readTimeOut(), writerTimeOut(), connectTimeOut()));
                            }
                            pipeline.addLast(new LoggingHandler(logLevel()));
                            addLast(pipeline);
                            pipeline.addLast(getDecoderHandler());
                            pipeline.addLast(getEncoderHandler());
                        }
                    });
        } catch (Exception e) {
            log.error("创建服务器异常!", e);
        }
    }
    public void start(int port){
        try {
            channel = bootstrap.bind(port).sync().channel();
            log.info("服务器启动成功，开始监听{} 端口...", port);
        } catch (Exception e) {
            log.error("启动服务器异常!", e);
        }
    }

    public void shutdown() {
        channel.close().awaitUninterruptibly();
    }
    protected void addLast(ChannelPipeline pipeline) {

    }
    protected abstract ChannelHandler getDecoderHandler();
    protected ChannelHandler getEncoderHandler(){
        return null;
    }
}
