package com.alone.game.netty.event;

import com.alone.game.netty.codec.Worker;
import com.alone.game.netty.net.Packet;
import com.alone.game.netty.protobuf.ResultPro;
import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/2 11:33
 */
public interface Event<T> extends Runnable {
    /**
     * 请求开始时间
     * @return
     */
    long getStartTime();
    /**
     * 包大小：cmd+ret+data
     * @return
     */
    public int getLength();

    /**
     * 请求的cmd
     * @return
     */
    public short getCmd();

    /**
     * 状态
     * @return
     */
    public ResultPro.Result getRet();

    /**
     * 包体数据
     * @return
     */
    public byte[] getData();

    /**
     * 玩家
     * @return
     */
    public T getObject();

    /**
     * 维持的长连接管道
     * @return
     */
    public Channel getChannel();

    /**
     * 往该事件写回数据包
     * @return
     */
    public void write(Packet packet);

    public <T extends Packet> T createSuccess(MessageLite msg) throws Exception;

    /**
     * 当前玩家的消息工作者
     * @return
     */
    public Worker<T, ? extends ReceivedEvent> getWorker();

    public InetSocketAddress getSender();
}
