package com.alone.game.netty.listeners;

import com.alone.game.netty.AbstractUser;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/2 10:12
 */
public interface UserStateListener<T extends AbstractUser> {
    /**
     * 离线
     * @param user
     */
    void offline(T user);
    /**
     * 上线
     * @param user
     */
    void online(T user);
    /**
     * 重连
     * @param user
     */
    void reconnect(T user);
}
