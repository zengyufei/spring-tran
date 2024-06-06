package com.example.tran.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 异步工具类，封装意义在于用于业务代码编写简洁
 *
 * @author zyf
 * @date 2024/05/30
 */
@Slf4j
public class PkSync {
    private static Executor poolExecutor;
    private final List<CompletableFuture<Void>> futures;
    private final ConcurrentStopWatch sw;

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
            throw new RuntimeException(StrUtil.format("当前处于事务状态无法跨线程, 请尝试使用 本类的countDownLatch() 方法 "));
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
                throw new RuntimeException(StrUtil.format("当前处于事务状态无法跨线程, 请尝试使用 本类的countDownLatch() 方法 "));
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


    public static MySyncMethod countDownLatch(CountDownLatch cd) {
        return new MySyncMethod(cd);
    }

    public static class MySyncMethod {

        CountDownLatch cd;
        List<Runnable> runnables = new ArrayList<>();

        public MySyncMethod(CountDownLatch cd) {
            this.cd = cd;
        }

        public MySyncMethod add(Runnable runnable) {
            runnables.add(runnable);
            return this;
        }

        @Async("taskExecutor")
        @Transactional
        public <V> void execute() {
            for (Runnable runnable : runnables) {
                // 子线程中的事务
                runnable.run();
                cd.countDown();
            }
        }

    }
}
