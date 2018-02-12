package com.alone.game.netty;

import com.alone.game.netty.codec.Worker;
import com.alone.game.netty.listeners.UserStateListener;
import com.alone.game.netty.net.Packet;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/2 10:07
 */
@Slf4j
@Data
public abstract class AbstractUser<IdType, S> implements Serializable {
    private IdType id;

    private String name;

    private int sex;

    @JsonIgnore
    private S server;

    @JsonIgnore
    private volatile Channel channel;

    private long ns = System.nanoTime();

    /**
     * 登陆时间（秒）
     */
    private volatile int loginTime;
    /**
     * 心跳时间
     */
    private volatile long heartTime;

    /**
     * 最后离线时间（秒）
     */
    private volatile long lastOfflineTime;

    @JsonIgnore
    private volatile Worker worker;

    /**
     * 给玩家发数据包
     * @param packet
     */
    public ChannelFuture send(Packet packet) {
        if(isOnline()) {
            return channel.writeAndFlush(packet);
        }
        return null;
    }
    /**
     * 判断玩家是否在线
     * @return
     */
    public boolean isOnline() {
        if(channel == null) return false;
        if(!channel.isActive()) {
            return false;
        }
        return true;
    }

    public void offline() {
        channel.close();
        stateChanged("offline");
    }

    public void reconnect() {
        stateChanged("reconnect");
    }

    public void online() {
        stateChanged("online");
    }

    /**
     * 玩家状态变更
     * @param state
     */
    private void stateChanged(final String state){
        List<? extends UserStateListener> userStateListeners = getUserStateListeners();
        if (userStateListeners == null || userStateListeners.isEmpty())
            return;
        Iterator<? extends UserStateListener> it = userStateListeners.iterator();
        while(it.hasNext()) {
            final UserStateListener listener = it.next();
            worker.executeTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        UserStateListener.class.getMethod(state, AbstractUser.class).invoke(listener, AbstractUser.this);
                    } catch (Exception e) {
                        log.warn("用户:[{}]状态变更:<{}>异常:{}", AbstractUser.this, state, e);
                        log.warn("", e);
                    }
                }
            });
        }
    }

    @JsonIgnore
    protected abstract List<? extends UserStateListener> getUserStateListeners();
}
