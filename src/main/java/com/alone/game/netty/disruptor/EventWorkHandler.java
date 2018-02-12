package com.alone.game.netty.disruptor;

import com.lmax.disruptor.WorkHandler;

public class EventWorkHandler implements WorkHandler<Event> {

	@Override
	public void onEvent(Event event) throws Exception {
		event.run();
	}

}
