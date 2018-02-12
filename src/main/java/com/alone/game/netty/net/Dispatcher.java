package com.alone.game.netty.net;

import com.alone.game.netty.event.HandlerEvent;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/14 14:36
 */
@Slf4j
public class Dispatcher {
    /**
     * 获取业务逻辑处理Handler
     * @param handlerClass
     * @param <T>
     * @return
     */
    public static <T> Map<Short, HandlerEvent<T>> getHandlers(Class<T> handlerClass, AbstractApplicationContext applicationContext){
        Map<Short, HandlerEvent<T>> result = Maps.newConcurrentMap();
        Map<String, T> handlerMap = applicationContext.getBeansOfType(handlerClass);
        for(Map.Entry<String, T> entry : handlerMap.entrySet()) {
            T handler = entry.getValue();
            Cmd cmd = handler.getClass().getAnnotation(Cmd.class);
            if(cmd == null) continue;
            short inCmd = cmd.value();
            HandlerEvent event = new HandlerEvent(inCmd, cmd.async(), handler);
            result.put(inCmd, event);
            log.info("注册Handler<{}>--CMD:0x{},Handler:{}", new Object[]{handlerClass, Integer.toHexString(inCmd), handler.getClass()});
        }
        return result;
    }
}
