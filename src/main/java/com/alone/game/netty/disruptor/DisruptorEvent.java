package com.alone.game.netty.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alone
 */
@Slf4j
public class DisruptorEvent {
	private RingBuffer<Event> buffer = null;
	private ExecutorService executor = null;
	private volatile int INDEX = 0;
	private int size = 0;
	
	public DisruptorEvent(final String name, final int thread) {
		this(name, thread, 1 << 10);
	}
	
	public DisruptorEvent(final String name, final int thread, final int bufferSize) {
		this.size = bufferSize;
		executor = Executors.newFixedThreadPool(thread, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				String threadName = name;
				if(thread > 1) {
					threadName = threadName + INDEX ++;
				}
				Thread thread = new Thread(r, threadName);
				return thread;
			}
		});
		buffer = RingBuffer.create(ProducerType.SINGLE,
				new EventFactory(), bufferSize, new BlockingWaitStrategy());
		
		EventWorkHandler[] dbWorkHandlers = new EventWorkHandler[thread];
		for(int i = 0; i < dbWorkHandlers.length; i ++) {
			dbWorkHandlers[i] = new EventWorkHandler();
		}
		
		WorkerPool<Event> workerPool = new WorkerPool<Event>(buffer,
				buffer.newBarrier(), new IgnoreExceptionHandler(), dbWorkHandlers);
		Sequence[] sequences = workerPool.getWorkerSequences();
		buffer.addGatingSequences(sequences);
		
		workerPool.start(executor);
		
	}
	public void publish(final Runnable event) {
		if(event !=null) {
			long next = buffer.next();
			try {
				Event tmp = buffer.get(next);
				tmp.setEvent(event);
			} finally {
				buffer.publish(next);
			}
		}
	}
	
	public int stack() {
		return this.size - (int)buffer.remainingCapacity();
	}
	public long cursor() {
		return buffer.getCursor();
	}
	public void execute(final Runnable event) {
		publish(event);
	}
	public void shutdown() {
		executor.shutdown();
	}
	
	public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		executor.awaitTermination(timeout, unit);
	}
	public static void main(String args[]) {
		long start = System.currentTimeMillis();
		final DisruptorEvent disruptor = new DisruptorEvent("DisruptorEvent-thread", 1);
		final CountDownLatch count = new CountDownLatch(10000);
		final AtomicInteger n = new AtomicInteger(0);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i = 0; i < 10000; i ++) {
					disruptor.publish(new Runnable() {
						
						@Override
						public void run() {
							log.info("task-" + n.getAndAdd(1));
							count.countDown();
						}
					});
				}
			}
		}).start();
		try {
			count.await();
			System.out.println(System.currentTimeMillis() - start);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
