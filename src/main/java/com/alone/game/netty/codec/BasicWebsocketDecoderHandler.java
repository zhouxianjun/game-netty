package com.alone.game.netty.codec;

import com.alone.game.netty.event.ReceivedEvent;
import com.alone.game.netty.net.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Basic解码处理
 * @date 2015/6/4 14:55
 */
public class BasicWebsocketDecoderHandler<W extends Worker<T, ? extends ReceivedEvent<T, P>>, T, P extends Packet> extends AbstractSimpleChannelInboundHandler<W, T, P, ByteBufHolder> {
    private final WebSocketClientHandshaker handShaker;
    private ChannelPromise handshakeFuture;
    public BasicWebsocketDecoderHandler(Class<W> worker, Class<P> packet, WebSocketClientHandshaker handShaker) {
        super();
        this.handShaker = handShaker;
        super.setWorkerClass(worker);
        super.setPacketClass(packet);
    }

    public BasicWebsocketDecoderHandler(Class<W> worker, Class<P> packet) {
        super();
        this.handShaker = null;
        super.setWorkerClass(worker);
        super.setPacketClass(packet);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        if (handShaker != null) handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (handShaker != null) {
            handShaker.handshake(ctx.channel());
        } else {
            super.channelActive(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (handShaker != null && !handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected ByteBuf getData(ChannelHandlerContext ctx, ByteBufHolder packet) {
        return packet.content();
    }

    @Override
    protected boolean beforeRead(ChannelHandlerContext ctx, ByteBufHolder webSocketFrame) {
        if (handShaker != null && !handShaker.isHandshakeComplete()) {
            Channel ch = ctx.channel();
            try {
                handShaker.finishHandshake(ch, (FullHttpResponse) webSocketFrame);
                super.channelActive(ctx);
                handshakeFuture.setSuccess();
            } catch (Exception e) {
                System.out.println("WebSocket Client failed to connect");
                handshakeFuture.setFailure(e);
            }
            return false;
        }
        return true;
    }
}
