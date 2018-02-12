package com.alone.game.netty.codec;

import com.alone.game.netty.event.ReceivedEvent;
import com.alone.game.netty.net.Packet;
import io.netty.channel.group.ChannelGroup;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Basic解码处理
 * @date 2015/6/4 14:55
 */
public class BasicDecoderHandler<W extends Worker<T, ? extends ReceivedEvent<T, P>>, T, P extends Packet> extends AbstractDecoderHandler<W, T, P> {
    public BasicDecoderHandler(ChannelGroup channelGroup, Class<W> worker, Class<P> packet) {
        super(channelGroup, 32765, 0, 2, 0, 0);
        super.setWorkerClass(worker);
        super.setPacketClass(packet);
    }
    public BasicDecoderHandler(ChannelGroup channelGroup, int maxFrameLength,
                               int lengthFieldOffset, int lengthFieldLength,
                               int lengthAdjustment, int initialBytesToStrip, Class<W> worker, Class<P> packet) {
        super(channelGroup, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        super.setWorkerClass(worker);
        super.setPacketClass(packet);
    }
}
