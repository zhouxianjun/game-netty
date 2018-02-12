package com.alone.game.netty.codec;

import com.alone.game.netty.event.ReceivedEvent;
import com.alone.game.netty.net.Cmd;
import com.alone.game.netty.net.Packet;
import com.alone.game.netty.net.PacketResult;
import com.alone.game.netty.protobuf.ResultPro;
import com.xiaoleilu.hutool.util.ClassUtil;
import com.xiaoleilu.hutool.util.ReflectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2018/1/18 16:45
 */
@Slf4j
public abstract class AbstractSimpleChannelInboundHandler<W extends Worker<T, ? extends ReceivedEvent<T, P>>, T, P extends Packet, D> extends SimpleChannelInboundHandler<D> {
    private Channel channel;
    protected Worker<T, ? extends ReceivedEvent<T, P>> worker;
    @Setter
    private Class workerClass;
    @Setter
    private Class packetClass;
    private Method packetReadMethod;

    protected InetSocketAddress getSender(ChannelHandlerContext ctx, D d) {
        if (d instanceof DatagramPacket) {
            return ((DatagramPacket)d).sender();
        }
        if (ctx.channel().remoteAddress() != null && ctx.channel().remoteAddress() instanceof InetSocketAddress) {
            return (InetSocketAddress) ctx.channel().remoteAddress();
        }
        return null;
    }

    protected abstract ByteBuf getData(ChannelHandlerContext ctx, D d);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, D d) throws Exception {
        if (!beforeRead(ctx, d)) {
            return;
        }
        InetSocketAddress sender = getSender(ctx, d);
        try {
            Method method = getPacketReadMethod();
            if (method == null) {
                log.error("获取包对象异常!Class:{}", getPacketClass());
                return;
            }
            ByteBuf buf = getData(ctx, d);
            PacketResult result = ReflectUtil.invokeStatic(method, buf, 0);
            if (result == null) {
                log.warn("message length error......");
                channel.close();
                return;
            }
            short cmd = result.getCmd();
            if (cmd != Cmd.PING && worker.printMsg(cmd)) {
                log.info("接收到消息:CMD:0x{}, ip:{}, 总长度:{}, ret长度:{}, body长度:{}, code:{}, msg:{}",
                        Integer.toHexString(cmd), sender.getHostName(), result.getLength(), result.getRetSize(), result.getBody().length, result.getResult().getCode(), result.getResult().getMsg());
            }
            messageReceived(result.getBuffer(), result.getLength(), cmd, result.getResult(), result.getBody(), sender);
        } catch (Exception e) {
            log.error("IP: {} 接收消息异常:不符合标准!", sender, e);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connection(channel);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        disconnection();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error("IP: {} 连接异常，关闭连接", ctx.channel().remoteAddress(), cause);
        ctx.close().sync();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            timeOut(e.state(), ctx);
        }
    }

    /**
     * 收到消息
     * @param buffer
     * @param length
     * @param cmd
     * @param result
     * @param body
     */
    protected void messageReceived(ByteBuf buffer, int length, short cmd, ResultPro.Result result, byte[] body, InetSocketAddress sender){
        worker.messageReceived(buffer, length, cmd, result, body, sender);
    }
    protected void connection(Channel channel){
        Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>> eventClass = getWorkerClass();
        try {
            Constructor<? extends Worker<T, ? extends ReceivedEvent<T, P>>> constructor = eventClass.getDeclaredConstructor(Channel.class);
            worker = constructor.newInstance(channel);
        } catch (Exception e) {
            log.error("工作创建失败!", e);
        }
    }
    protected Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>> getWorkerClass(){
        return workerClass == null ? (Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>>) ClassUtil.getTypeArgument(getClass()) : workerClass;
    }
    protected Class<? extends P> getPacketClass(){
        return packetClass == null ? (Class<? extends P>) ClassUtil.getTypeArgument(getClass(), 2) : packetClass;
    }
    protected Method getPacketReadMethod() {
        if (packetReadMethod == null) {
            packetReadMethod = ReflectUtil.getMethod(getPacketClass(), "extractFrame", ByteBuf.class, int.class);
        }
        return packetReadMethod;
    }
    /**
     * 断开连接
     */
    protected void disconnection(){
        if (worker != null)
            worker.processDisconnection();
    }

    /**
     * 超时
     * @param state
     * @param ctx
     * @throws Exception
     */
    protected void timeOut(IdleState state, ChannelHandlerContext ctx) throws Exception {
        log.warn("IP:{}, {} 超时，关闭连接", ctx.channel().remoteAddress(), state.name());
        ctx.close().sync();
    }

    protected boolean beforeRead(ChannelHandlerContext ctx, D d) {return true;}
}
