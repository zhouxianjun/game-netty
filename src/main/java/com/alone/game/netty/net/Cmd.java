package com.alone.game.netty.net;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cmd {

	/**
	 * 请求消息包ID
	 * @return
	 */
	short value();
	/**
	 * 是否异步
	 * @return
	 */
	boolean async() default false;

	/**
	 * 是否需要登录
	 * @return
	 */
	boolean login() default false;

	short GLOBAL_EXCEPTION = 0x0000;
	short PING = 0x0001;
}
