package com.alone.game.netty.net;

import com.alone.game.netty.ResultCode;
import com.alone.game.netty.protobuf.ResultPro;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.xiaoleilu.hutool.setting.Setting;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/3/6 10:29
 */
@Slf4j
@Getter
@NoArgsConstructor
public class Packet {
    @Setter
    private Short cmd;

    private ResultPro.Result ret;

    private MessageLite body;

    @Setter
    private long startTime;

    private byte[] retData;

    private byte[] bodyData;

    @Setter
    private boolean print = true;

    @Setter
    private InetSocketAddress dest;

    private int size;

    private ByteBuf byteBuffer;

    private int resultLength;
    private int bodyLength;

    public static Setting DESC = new Setting("error-desc.config.properties");

    public Packet(Short cmd, ResultPro.Result ret, MessageLite body, byte[] bodyData, InetSocketAddress dest, Long startTime) {
        this.cmd = cmd;
        this.ret = ret == null ? getRet() : ret;
        this.body = body;
        this.bodyData = bodyData;
        this.startTime = startTime == null ? System.currentTimeMillis() : startTime;
        this.dest = dest;
        this.init();
    }

    public Packet(Short cmd) {
        this(cmd, null, null, null, null, null);
    }

    public Packet(Short cmd, ResultPro.Result ret) {
        this(cmd, ret, null, null, null, null);
    }

    public Packet(Short cmd, MessageLite body) {
        this(cmd, null, body);
    }

    public Packet(Short cmd, byte[] bodyData) {
        this(cmd, null, bodyData);
    }

    public Packet(Short cmd, ResultPro.Result ret, MessageLite body) {
        this(cmd, ret, body, null, null, null);
    }

    public Packet(Short cmd, ResultPro.Result ret, byte[] bodyData) {
        this(cmd, ret, null, bodyData, null, null);
    }


    public Packet init() {
        if (this.cmd == null) return null;
        if (this.byteBuffer != null) {
            ReferenceCountUtil.safeRelease(this.byteBuffer);
        }
        this.retData = this.getRet().toByteArray();
        this.bodyData = this.bodyData != null ? this.bodyData : this.body == null ? null : this.body.toByteArray();
        this.resultLength = this.getRetData().length;
        this.bodyLength = this.getBodyData() == null ? 0 : this.getBodyData().length;
        this.size = this.calcSize();
        this.byteBuffer = this.write();
        return this;
    }

    public ResultPro.Result getRet(){
        if (ret == null) {
            ret = ResultPro.Result.getDefaultInstance();
        }
        if (ret.getCode() != ret.getDefaultInstanceForType().getCode()) {
            try {
                ResultPro.Result.Builder builder = ResultPro.Result.parseFrom(ret.toByteArray()).toBuilder();
                if (!builder.hasMsg()) {
                    builder.setMsg(DESC.getStr(String.valueOf(builder.getCode()), "code", ""));
                    return builder.build();
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("获取ret异常!", e);
            }
        }
        return ret;
    }

    public static <T extends Packet> T createException(Short cmd, int errorCode, MessageLite body){
        return (T) new Packet(cmd, ResultPro.Result.newBuilder().setCode(errorCode).build(), body);
    }

    public static <T extends Packet> T createGlobalException(){
        return createException(Cmd.GLOBAL_EXCEPTION, ResultCode.UNKNOWN_ERROR, null);
    }

    public static <T extends Packet> T createGlobalException(int errorCode){
        return createException(Cmd.GLOBAL_EXCEPTION, errorCode, null);
    }

    public static <T extends Packet> T createSuccess(Short cmd, MessageLite body){
        return (T) new Packet(cmd, body);
    }
    public static <T extends Packet> T createSuccess(MessageLite body){
        return createSuccess(null, body);
    }
    public static <T extends Packet> T createSuccess(){
        return createSuccess(null);
    }

    public static <T extends Packet> T createError(int errorCode, MessageLite body){
        return createException(null, errorCode, body);
    }
    public static <T extends Packet> T createError(int errorCode){
        return createException(null, errorCode, null);
    }

    public int calcSize(){
        //cmd 指令长度
        int size = 4;
        size += getResultLength();
        size += getBodyLength();
        return size;
    }

    public ByteBuf write(ByteBuf byteBuf){
        if (byteBuf == null) {
            byteBuf = Unpooled.directBuffer();
        }
        //输出总长度
        byteBuf.writeShort(getSize());
        //命令
        byteBuf.writeShort(getCmd());
        byteBuf.writeShort(getResultLength());
        byteBuf.writeBytes(getRetData());

        if(getBodyData() != null) {
            byteBuf.writeBytes(getBodyData());
        }
        return byteBuf;
    }

    public ByteBuf write() {
        return write(null);
    }

    public static int readLength(ByteBuf buffer){
        return buffer.readShort();
    }

    public static final Packet PING = Packet.createSuccess(Cmd.PING, null);

    public static PacketResult extractFrame(ByteBuf buffer, int length) {
        if(buffer.readableBytes() < 1){
            log.warn("message length error......");
            return null;
        }
        //整个消息包大小
        final int totalLength = readLength(buffer);
        //当前请求CMD
        final short cmd = buffer.readShort();
        //消息RET 长度
        final short retSize = buffer.readShort();
        //消息RET
        final byte[] ret = new byte[retSize];
        buffer.readBytes(ret);
        ResultPro.Result result;
        try {
            result = ResultPro.Result.parseFrom(ret);
        } catch (InvalidProtocolBufferException e) {
            result = ResultPro.Result.getDefaultInstance();
        }
        //消息body
        int bodyLen = totalLength - 4 - retSize;
        int readableBytes = buffer.readableBytes();
        final byte[] body = new byte[bodyLen];
        buffer.readBytes(body);
        if (readableBytes != bodyLen){
            log.warn("消息0x{}, body异常,表示长度:{},实际长度:{}, 接收长度:{}", Integer.toHexString(cmd), bodyLen, readableBytes, length);
        }
        return new PacketResult(buffer, totalLength, retSize, cmd, result, body);
    }

    @Override
    public Packet clone() {
        Packet packet = new Packet();
        packet.cmd = this.getCmd();
        packet.ret = this.getRet();
        packet.body = this.getBody();
        packet.bodyData = this.getBodyData();
        packet.startTime = this.getStartTime();
        packet.dest = this.getDest();
        return packet.init();
    }
}
