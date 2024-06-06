package com.example.tran.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 异步工具类，封装意义在于用于业务代码编写简洁
 *
 * @author zyf
 * @date 2024/05/30
 */
@Slf4j
@Component
public class PkSync {
    private static Executor poolExecutor;
    private List<CompletableFuture<Void>> futures;
    private ConcurrentStopWatch sw;

    private static SpringSyncMethod springSyncMethod;

    @Autowired
    public void setSpringSyncMethod(SpringSyncMethod springSyncMethod) {
        PkSync.springSyncMethod = springSyncMethod;
    }

    public PkSync() {
    }

    public PkSync(Executor poolExecutor) {
        this(StrUtil.EMPTY, poolExecutor);
    }

    public PkSync(String id, Executor poolExecutor) {
        PkSync.poolExecutor = poolExecutor;
        this.futures = new ArrayList<>();
        this.sw = new ConcurrentStopWatch(id);
    }

    public static PkSync of(Executor poolExecutor) {
        return of(StrUtil.EMPTY, poolExecutor);
    }

    public static PkSync of(String id, Executor poolExecutor) {
        return new PkSync(id, poolExecutor);
    }

    public PkSync add(String taskName, Runnable runnable) {
        boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        if (inTransaction) {
            throw new RuntimeException(StrUtil.format("当前处于事务状态无法跨线程, 请尝试使用 本类的 sync() 方法 "));
        } else {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                sw.start(taskName);
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("{} 执行错误", taskName);
                    throw new RuntimeException(e);
                } finally {
                    sw.stop(taskName);
                }
            }, poolExecutor)
//                .exceptionally(e -> {
//                    throw new RuntimeException(e);
//                })
                    ;
            futures.add(future);
        }

        return this;
    }

    public void clear() {
        futures.clear();
    }

    public void waitAll(long timeout, TimeUnit unit) throws Exception {
        if (!futures.isEmpty()) {
            boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
            if (inTransaction) {
                throw new RuntimeException(StrUtil.format("当前处于事务状态无法跨线程, 请尝试使用 本类的 sync() 方法 "));
            } else {
                final CompletableFuture[] futuresArray = futures.toArray(CompletableFuture[]::new);
                final CompletableFuture<Void> future = CompletableFuture.allOf(futuresArray);
                try {
                    future.get(timeout, unit);
                } catch (TimeoutException e) {
                    log.error("超过执行设定的时间");
                    sw.setMax(timeout, unit);
                    throw e;
                } finally {
                    log.info(sw.prettyPrint(TimeUnit.MILLISECONDS));
                }
            }

        } else {
            log.warn("没有任务执行");
        }
    }


    public static MySyncMethod sync(Executor poolExecutor) {
        return new MySyncMethod(poolExecutor);
    }


    public static MySyncMethod sync(String id, Executor poolExecutor) {
        return new MySyncMethod(poolExecutor, id);
    }

    public static class MySyncMethod {
        Executor poolExecutor;
        ConcurrentStopWatch sw;
        List<Map<String, Runnable>> runnables = new ArrayList<>();

        public MySyncMethod(Executor poolExecutor) {
            this.poolExecutor = poolExecutor;
            this.sw = new ConcurrentStopWatch(StrUtil.EMPTY);
        }

        public MySyncMethod(Executor poolExecutor, String id) {
            this.poolExecutor = poolExecutor;
            this.sw = new ConcurrentStopWatch(id);
        }

        public MySyncMethod add(String taskName, Runnable runnable) {
            final Map<String, Runnable> map = new HashMap<>();
            map.put(taskName, runnable);
            runnables.add(map);
            return this;
        }

        public void join(long timeout, TimeUnit unit) {
            final CountDownLatch cd = new CountDownLatch(runnables.size());
            for (Map<String, Runnable> runnableMap : runnables) {
                // 子线程中的事务
                for (Map.Entry<String, Runnable> entry : runnableMap.entrySet()) {
                    final String taskName = entry.getKey();
                    final Runnable value = entry.getValue();
                    sw.start(taskName);
                    springSyncMethod.sync(poolExecutor, cd, value);
                    sw.stop(taskName);
                }
            }
            try {
                cd.await(timeout, unit);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info(sw.prettyPrint(TimeUnit.MILLISECONDS));
            runnables.clear();
        }

    }

}
