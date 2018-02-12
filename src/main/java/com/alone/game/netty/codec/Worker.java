package com.alone.game.netty.codec;

import com.alone.game.netty.disruptor.DisruptorEvent;
import com.alone.game.netty.event.ReceivedEvent;
import com.alone.game.netty.protobuf.ResultPro;
import com.xiaoleilu.hutool.util.ClassUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:对象事件工作
 * @date 2015/4/14 14:54
 */
@Slf4j
public abstract class Worker<T, E extends ReceivedEvent> {
    public static final int CORE_NUM = Runtime.getRuntime().availableProcessors();
    public static final AttributeKey PLAYER_KEY = AttributeKey.valueOf("player");
    public final String ip;
    private final Channel channel;
    private volatile T object;
    private AtomicBoolean offlineProcessed = new AtomicBoolean(false);

    private volatile DisruptorEvent taskExec;
    private volatile DisruptorEvent taskInExec;

    public Worker(Channel channel) {
        if (channel == null){
            throw new NullPointerException("新建Worker, channel为null");
        }
        this.channel = channel;
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        if (remoteAddress != null) {
            this.ip = remoteAddress.getAddress().getHostAddress();
        } else {
            this.ip = "0.0.0.0";
        }
        taskInExec = new DisruptorEvent("WorkerIn_", CORE_NUM);
        log.info("connection ip = {}", ip);
    }

    /**
     * 接收到消息
     * @param buffer
     * @param length
     * @param cmd
     * @param result
     * @param body
     */
    public void messageReceived(ByteBuf buffer, int length, short cmd, ResultPro.Result result, byte[] body, InetSocketAddress sender){
        if (!ping(channel, object, cmd)){
            Class<? extends ReceivedEvent> eventClass = getEventClass();
            try {
                Class<?> t = ClassUtil.getTypeArgument(getEventClass());
                Constructor<? extends ReceivedEvent> constructor = eventClass.getDeclaredConstructor(int.class, short.class, t, Channel.class, ResultPro.Result.class, Worker.class, byte[].class, InetSocketAddress.class);
                ReceivedEvent event = constructor.newInstance(length, cmd, object, channel, result, this, body, sender);
                taskInExec.publish(event);
            } catch (Exception e) {
                log.error("事件创建失败!", e);
            }
        }
    }

    /**
     * 断线
     */
    public void processDisconnection() {
        if (offlineProcessed.compareAndSet(false, true)) {
            disconnection(object, channel, ip);
        }
    }

    /**
     * 已通过服务器验证(例如登录后)调用
     * @param object
     * @param task
     */
    public void login(T object, DisruptorEvent task){
        if (task != null) {
            this.taskExec = task;
        }
        this.object = object;
        channel.attr(PLAYER_KEY).set(object);
    }

    /**
     * ping包处理 返回false则执行eventHandler
     * @param channel
     * @param object
     * @param cmd
     * @return
     */
    protected abstract boolean ping(Channel channel, T object, short cmd);

    /**
     * 获取接收事件Event 类
     * @return
     */
    protected Class<? extends ReceivedEvent> getEventClass(){
        return (Class<? extends ReceivedEvent>) ClassUtil.getTypeArgument(getClass(), 1);
    }

    /**
     * 断开连接
     * @param object
     * @param channel
     * @param ip
     */
    protected void disconnection(T object, Channel channel, String ip){}

    /**
     * 在对象线性线程中执行
     * @param event
     */
    public void executeTask(Runnable event) {
        if (taskExec == null){
            throw new NullPointerException("对象线性线程未初始化");
        }
        taskExec.publish(event);
    }

    protected boolean printMsg(short cmd){
        return true;
    }
}
