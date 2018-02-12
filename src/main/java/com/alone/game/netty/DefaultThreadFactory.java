package com.alone.game.netty;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/13 17:08
 */
public class DefaultThreadFactory {
    public static ThreadFactory newThreadFactory(final String name) {

        ThreadFactory threadFactory = new ThreadFactory() {

            volatile AtomicInteger index = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(name + index.getAndIncrement());
                return thread;
            }
        };
        return threadFactory;
    }
}
