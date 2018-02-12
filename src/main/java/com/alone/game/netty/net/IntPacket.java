package com.alone.game.netty.net;

import io.netty.buffer.ByteBuf;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/7/8 19:43
 */
public class IntPacket extends Packet {

    @Override
    public ByteBuf write(ByteBuf byteBuf) {
        byteBuf.writeInt(calcSize()); //输出总长度
        byteBuf.writeShort(getCmd()); //命令
        byteBuf.writeShort(getRetData().length); //命令
        byteBuf.writeBytes(getRetData());

        if(getBodyData() != null) {
            byteBuf.writeBytes(getBodyData());
        }else if (getBody() != null){
            byteBuf.writeBytes(getBody().toByteArray());
        }
        return byteBuf;
    }

    public static int readLength(ByteBuf buffer) {
        return buffer.readInt();
    }
}
