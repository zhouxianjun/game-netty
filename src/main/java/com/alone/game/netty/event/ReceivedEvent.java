package com.alone.game.netty.event;

import com.alone.game.netty.ResultCode;
import com.alone.game.netty.codec.Worker;
import com.alone.game.netty.disruptor.DisruptorEvent;
import com.alone.game.netty.handler.Handler;
import com.alone.game.netty.net.Cmd;
import com.alone.game.netty.net.Packet;
import com.alone.game.netty.protobuf.ResultPro;
import com.google.protobuf.MessageLite;
import com.xiaoleilu.hutool.util.ClassUtil;
import com.xiaoleilu.hutool.util.ReflectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/14 15:51
 */
@Getter
@Slf4j
public abstract class ReceivedEvent<T, P extends Packet> implements Event<T> {

    private long startTime;
    private int length;
    private short cmd;
    private ResultPro.Result ret;
    private byte[] data;
    private Worker<T, ? extends ReceivedEvent> worker;
    private Channel channel;
    private T object;
    private InetSocketAddress sender;

    public ReceivedEvent(int length, short cmd, T object, Channel channel, ResultPro.Result ret, Worker<T, ? extends ReceivedEvent> worker, byte[] data, InetSocketAddress sender) {
        this.length = length;
        this.cmd = cmd;
        this.object = object;
        this.channel = channel;
        this.ret = ret;
        this.worker = worker;
        this.data = data;
        this.sender = sender;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void write(Packet packet) {
        if (packet.getCmd() == null) {
            packet.setCmd(cmd);
            packet.init();
        }
        if (channel instanceof NioDatagramChannel) {
            Method method = ReflectUtil.getMethod(object.getClass(), "send", Packet.class);
            if (method != null) {
                try {
                    method.invoke(object, packet);
                } catch (Exception e) {
                    log.warn("采用Object send发送数据异常 object {}", object, e);
                }
                return;
            }
            ByteBuf buf = packet.write();
            try {
                channel.writeAndFlush(new DatagramPacket(buf, sender));
            } finally {
                buf.release();
            }
            return;
        }
        packet.setStartTime(startTime);
        channel.writeAndFlush(packet);
        //log.debug("回复消息：IP:{}, 玩家:{}, CMD:0x{}, 耗时:{}毫秒", new Object[]{worker.ip, object, Integer.toHexString(cmd), System.currentTimeMillis() - startTime});
    }

    @Override
    public P createSuccess(MessageLite msg) throws Exception {
        Method method = ReflectUtil.getMethod(ClassUtil.getTypeArgument(getClass(), 1), "createSuccess", Short.class, MessageLite.class);
        return ReflectUtil.invokeStatic(method, cmd, msg);
    }

    protected void handle(final HandlerEvent<Handler> handlerEvent) {
        if(!handlerEvent.isAsync() || getDisruptorEvent() == null) {
            handle(this, handlerEvent.getHandler());
        } else {
            getDisruptorEvent().publish(() -> {
                try {
                    handle(ReceivedEvent.this, handlerEvent.getHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    protected boolean checkAuthorized(Event event, Handler handler) {
        if(handler == null) {
            Method method = ReflectUtil.getMethod(ClassUtil.getTypeArgument(getClass(), 1), "createGlobalException", int.class);
            event.write(ReflectUtil.invokeStatic(method, ResultCode.NOT_FOUND));
            return false;
        }
        if (handler.getClass().getAnnotation(Cmd.class).login() && object == null){
            Method method = ReflectUtil.getMethod(ClassUtil.getTypeArgument(getClass(), 1), "createGlobalException", int.class);
            event.write(ReflectUtil.invokeStatic(method, ResultCode.PARAM_FAIL));
            return false;
        }
        return true;
    }
    
    protected void handle(Event event, Handler handler) {
        try {
            if (checkAuthorized(event, handler)) {
                handler.handle(event);
            }
        } catch(Exception e) {
            Method method = ReflectUtil.getMethod(ClassUtil.getTypeArgument(getClass(), 1), "createError", int.class);
            event.write(ReflectUtil.invokeStatic(method, ResultCode.UNKNOWN_ERROR));
            log.error("Handler异常:", e);
        }
    }

    protected DisruptorEvent getDisruptorEvent(){
        return null;
    }

    protected P getPacketObject() {
        return (P) ReflectUtil.newInstance(ClassUtil.getTypeArgument(getClass(), 1));
    }
}
