package com.example.tran.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tran.entity.User;
import com.example.tran.mapper.User2Mapper;
import com.example.tran.mapper.UserMapper;
import com.example.tran.utils.PkSync;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User service impl.
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> implements IService<User> {

    /**
     * user dao.
     */
    private final UserMapper userMapper;
    private final User2Mapper user2Mapper;

    public static final ThreadPoolExecutor poolExecutor;

    static {
        poolExecutor = new ThreadPoolExecutor(50, 50,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(500), // 阻塞情况下，最大执行数量： maximumPoolSize + capacity
                (r, executor) -> {
                    try {
                        // 将任务添加到队列中,阻塞直到有可用线程
                        // 这里不会成为问题点，如果你的线程任务内没有阻塞点，即使抛异常，这里也都不会成为问题
                        executor.getQueue().put(r);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
    }


    public List<User> findAll() {
        return userMapper.selectList(Wrappers.emptyWrapper());
    }

    @Transactional(rollbackFor = Exception.class)
    public void check() throws Exception {

        PkSync.of(poolExecutor)
                .add("1", () -> {
                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 1L));
                    System.out.println(temp.get(0).getAge());
                })
                .add("2", () -> {
                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 2L));
                    System.out.println(temp.get(0).getAge());
                })
                .add("3", () -> {
                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 3L));
                    System.out.println(temp.get(0).getAge());
                })
                .add("4", () -> {
                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 4L));
                    System.out.println(temp.get(0).getAge());
                })
                .add("5", () -> {
                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 5L));
                    System.out.println(temp.get(0).getAge());
                })
                .waitAll(3, TimeUnit.SECONDS);

        {
            final List<User> users = userMapper.selectList(Wrappers.emptyWrapper());
            for (User user : users) {
                user.setAge(100);
            }
            this.updateBatchById(users);
        }

        {
            final List<User> users = userMapper.selectList(Wrappers.emptyWrapper());
            for (User user : users) {
                System.out.println(user.getAge());
            }
        }

        final CountDownLatch cd = new CountDownLatch(5);
        PkSync.countDownLatch(cd)
                .add(() -> queryById(1L))
                .add(() -> queryById(2L))
                .add(() -> queryById(3L))
                .add(() -> queryById(4L))
                .add(() -> queryById(5L))
                .execute();
        cd.await();

//        PkSync.of(poolExecutor)
//                .add("1", () -> {
//                    transactionTemplate.execute(satus -> {
//                        final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 1L));
//                        System.out.println(temp.get(0).getAge());
//                        // 子线程中的事务
//                        return null;
//                    });
//                })
//                .add("2", () -> {
//                    transactionTemplate.execute(satus -> {
//                        final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 2L));
//                        System.out.println(temp.get(0).getAge());
//                        // 子线程中的事务
//                        return null;
//                    });
//                })
//                .add("3", () -> {
//                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 3L));
//                    System.out.println(temp.get(0).getAge());
//                })
//                .add("4", () -> {
//                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 4L));
//                    System.out.println(temp.get(0).getAge());
//                })
//                .add("5", () -> {
//                    final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, 5L));
//                    System.out.println(temp.get(0).getAge());
//                })
//                .waitAll(3, TimeUnit.SECONDS);


    }

    public void queryById(long id) {
        // 子线程中的事务
        final List<User> temp = userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getId, id));
        System.out.println(temp.get(0).getAge());
    }
}
