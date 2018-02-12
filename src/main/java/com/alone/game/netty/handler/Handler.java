package com.alone.game.netty.handler;

import com.alone.game.netty.event.Event;

public interface Handler<T> {

	public void handle(final Event<T> event) throws Exception;
}
