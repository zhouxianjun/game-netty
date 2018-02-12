package com.alone.game.netty.codec;

import com.alone.game.netty.event.ReceivedEvent;
import com.alone.game.netty.net.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Basic解码处理
 * @date 2015/6/4 14:55
 */
public class BasicUdpDecoderHandler<W extends Worker<T, ? extends ReceivedEvent<T, P>>, T, P extends Packet> extends AbstractSimpleChannelInboundHandler<W, T, P, DatagramPacket> {
    public BasicUdpDecoderHandler( Class<W> worker, Class<P> packet) {
        super();
        super.setWorkerClass(worker);
        super.setPacketClass(packet);
    }

    @Override
    protected ByteBuf getData(ChannelHandlerContext ctx, DatagramPacket packet) {
        return packet.content();
    }
}
