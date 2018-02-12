package com.alone.game.netty.disruptor;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * 线程池管理类
 */
@SuppressWarnings("unchecked")
public class ThreadPoolMG
{

    private static Hashtable<String, ThreadPoolMG> threadPoolMGtb = new Hashtable<String, ThreadPoolMG>();

    private final static int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;

    private static int MAX_POOL_SIZE = 20;

    private final static int KEEP_ALIVE_TIME = 180;

    private static int WORK_QUEUE_SIZE = 10000;

    public Queue<Callable<?>> taskQueue = new LinkedList<Callable<?>>();

    public final LinkedBlockingQueue<Future<?>> result = new LinkedBlockingQueue<Future<?>>();

    public ThreadPoolMG() {
    	
    }
    public ThreadPoolMG(int maxPoolSize, int queueSize) {
    	MAX_POOL_SIZE = maxPoolSize;
    	WORK_QUEUE_SIZE = queueSize;
    }
    final Runnable accessBufferThread = new Runnable()
    {
        public void run()
        {
            // 查看是否有待定请求，如果有，则添加到线程池中
            if (hasMoreAcquire())
            {
                Future<?> fu = threadPool.submit(taskQueue.poll());
                result.offer(fu);
            }
        }
    };

    final RejectedExecutionHandler handler = new RejectedExecutionHandler()
    {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
        {
            System.out
                .println(r.toString()
                        + " task put in queue restart waiting..... "
                        + r.toString());
            Callable<?> callable = (Callable<?>) r;
            taskQueue.offer(callable);
        }
    };

    @SuppressWarnings("rawtypes")
	final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new ArrayBlockingQueue(WORK_QUEUE_SIZE), this.handler);

    final ScheduledExecutorService scheduler = Executors
        .newScheduledThreadPool(1);

    @SuppressWarnings("rawtypes")
    final ScheduledFuture taskHandler = scheduler.scheduleAtFixedRate(
            accessBufferThread, 0, 1, TimeUnit.SECONDS);

    /**
     * 根据key取得对应线程池实例
     * 
     * @param key
     * @return
     */
    public static synchronized ThreadPoolMG getInstance(String key)
    {
        ThreadPoolMG obj = threadPoolMGtb.get(key);
        if (obj == null)
        {
            obj = new ThreadPoolMG();
            threadPoolMGtb.put(key, obj);
        }
        return obj;
    }

    public void addTask(Callable<?> task)
    {
        Future<?> fu = threadPool.submit(task);
        result.offer(fu);
    }
    
    public void addTask(Runnable task)
    {
        threadPool.submit(task);
    }
    
    public void publish(Runnable task) {
    	threadPool.submit(task);
    }

    public void execute(Runnable task) {
    	threadPool.submit(task);
    }
    private boolean hasMoreAcquire()
    {
        return !taskQueue.isEmpty();
    }

    public LinkedBlockingQueue<Future<?>> getResult()
    {
        return result;
    }

    public boolean isEndTask()
    {
        while (true)
        {
            if (threadPool.getActiveCount() == 0)
            {
                return true;
            }

        }
    }

    public void shutdown()
    {
        threadPool.shutdown();
        scheduler.shutdown();
    }

}
