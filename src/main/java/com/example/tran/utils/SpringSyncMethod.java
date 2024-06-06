package com.example.tran.utils;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Component
public  class SpringSyncMethod {

        @Async
        @Transactional(rollbackFor = Exception.class)
        public void sync(Executor poolExecutor, CountDownLatch cd, Runnable runnable) {
            runnable.run();
            cd.countDown();
        }


    }
