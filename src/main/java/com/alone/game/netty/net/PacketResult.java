package com.alone.game.netty.net;

import com.alone.game.netty.protobuf.ResultPro;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2018/1/16 13:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PacketResult {
    private ByteBuf buffer;
    private int length;
    private int retSize;
    private short cmd;
    ResultPro.Result result;
    byte[] body;
}
