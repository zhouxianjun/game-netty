package com.alone.game.netty.disruptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Event {
	private Runnable event;

	public void setEvent(Runnable event) {
		this.event = event;
	}
	
	public void run() {
		try {
			this.event.run();
		} catch(Throwable e) {
			log.error(e.getMessage(), e);
		}
	}
	
}
