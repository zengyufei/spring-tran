package com.example.tran.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 秒表封装<br>
 * 此工具用于存储一组任务的耗时时间，并一次性打印对比。<br>
 * 比如：我们可以记录多段代码耗时时间，然后一次性打印（StopWatch提供了一个prettyString()函数用于按照指定格式打印出耗时）
 *
 * <p>
 * 此工具来自：https://github.com/spring-projects/spring-framework/blob/master/spring-core/src/main/java/org/springframework/util/StopWatch.java
 *
 * <p>
 * 使用方法如下：
 *
 * <pre>
 * StopWatch stopWatch = new StopWatch("任务名称");
 *
 * // 任务1
 * stopWatch.start("任务一");
 * Thread.sleep(1000);
 * stopWatch.stop();
 *
 * // 任务2
 * stopWatch.start("任务二");
 * Thread.sleep(2000);
 * stopWatch.stop();
 *
 * // 打印出耗时
 * Console.log(stopWatch.prettyPrint());
 *
 * </pre>
 *
 * @author Spring Framework, Looly
 * @since 4.6.6
 */
public class ConcurrentStopWatch {

    /**
     * 创建计时任务（秒表）
     *
     * @param id 用于标识秒表的唯一ID
     * @return StopWatch
     * @since 5.5.2
     */
    public static ConcurrentStopWatch create(String id) {
        return new ConcurrentStopWatch(id);
    }

    /**
     * 秒表唯一标识，用于多个秒表对象的区分
     */
    private final String id;
    private Map<String, TaskInfo> taskMap;
    /**
     * 总任务数
     */
    private final LongAdder taskCount = new LongAdder();
    /**
     * 开始任务数
     */
    private final LongAdder beginTaskCount = new LongAdder();
    /**
     * 停止任务数
     */
    private final LongAdder endTaskCount = new LongAdder();
    /**
     * 总运行时间
     */
    private final List<Long> totalTimeNanos = new CopyOnWriteArrayList<>();

    private final AtomicLong maxNanoTime = new AtomicLong(0);
    // ------------------------------------------------------------------------------------------- Constructor start

    /**
     * 构造，不启动任何任务
     */
    public ConcurrentStopWatch() {
        this(StrUtil.EMPTY);
    }

    /**
     * 构造，不启动任何任务
     *
     * @param id 用于标识秒表的唯一ID
     */
    public ConcurrentStopWatch(String id) {
        this(id, true);
    }

    /**
     * 构造，不启动任何任务
     *
     * @param id           用于标识秒表的唯一ID
     * @param keepTaskList 是否在停止后保留任务，{@code false} 表示停止运行后不保留任务
     */
    public ConcurrentStopWatch(String id, boolean keepTaskList) {
        this.id = id;
        if (keepTaskList) {
            this.taskMap = new ConcurrentSkipListMap<>();
        }
    }
    // ------------------------------------------------------------------------------------------- Constructor end

    /**
     * 获取StopWatch 的ID，用于多个秒表对象的区分
     *
     * @return the ID 默认为空字符串
     * @see #ConcurrentStopWatch(String)
     */
    public String getId() {
        return this.id;
    }
    public void setMax(long max, TimeUnit unit) {
        maxNanoTime.set(TimeUnit.NANOSECONDS.convert(max, unit));
    }

    /**
     * 设置是否在停止后保留任务，{@code false} 表示停止运行后不保留任务
     *
     * @param keepTaskList 是否在停止后保留任务
     */
    public void setKeepTaskList(boolean keepTaskList) {
        if (keepTaskList) {
            if (null == this.taskMap) {
                this.taskMap = new ConcurrentSkipListMap<>();
            }
        } else {
            this.taskMap = null;
        }
    }

    /**
     * 开始默认的新任务
     *
     * @throws IllegalStateException 前一个任务没有结束
     */
    public void start() throws IllegalStateException {
        start(StrUtil.EMPTY);
    }

    /**
     * 开始指定名称的新任务
     *
     * @param taskName 新开始的任务名称
     * @throws IllegalStateException 前一个任务没有结束
     */
    public void start(String taskName) throws IllegalStateException {
        if (null == taskName) {
            throw new IllegalStateException("start(taskName 不能为空!)");
        }

        TaskInfo taskInfo = new TaskInfo(taskName, System.nanoTime(), maxNanoTime);
        if (null != this.taskMap) {
            this.taskMap.put(taskName, taskInfo);
        }
        taskCount.increment();
        beginTaskCount.increment();
    }

    /**
     * 停止当前任务
     *
     * @throws IllegalStateException 任务没有开始
     */
    public void stop(String taskName) throws IllegalStateException {
        if (null == taskName) {
            throw new IllegalStateException("stop(taskName 不能为空!)");
        }
        if (!this.taskMap.containsKey(taskName)) {
            throw new IllegalStateException("taskName 不能 Stop, 因为没有 Start.");
        }

        final TaskInfo taskInfo = this.taskMap.get(taskName);

        final long endTime = System.nanoTime();
        final long lastTime = endTime - taskInfo.startTimeNanos;
        taskInfo.timeNanos = lastTime;

        synchronized (this) {
            final long max = Math.max(lastTime, maxNanoTime.get());
            maxNanoTime.set(max);
        }

        totalTimeNanos.add(lastTime);
        endTaskCount.increment();
    }

    /**
     * 检查是否有正在运行的任务
     *
     * @return 是否有正在运行的任务
     */
    public boolean isRunning() {
        return endTaskCount.longValue() < beginTaskCount.longValue();
    }

    /**
     * 获取所有任务的总花费时间
     *
     * @param unit 时间单位，{@code null}表示默认{@link TimeUnit#NANOSECONDS}
     * @return 花费时间
     * @since 5.7.16
     */
    public long getTotal(TimeUnit unit) {
        return unit.convert(maxNanoTime.longValue(), TimeUnit.NANOSECONDS);
    }

    /**
     * 获取所有任务的总花费时间（纳秒）
     *
     * @return 所有任务的总花费时间（纳秒）
     * @see #getTotalTimeMillis()
     * @see #getTotalTimeSeconds()
     */
    public long getTotalTimeNanos() {
        return maxNanoTime.get();
    }

    /**
     * 获取所有任务的总花费时间（毫秒）
     *
     * @return 所有任务的总花费时间（毫秒）
     * @see #getTotalTimeNanos()
     * @see #getTotalTimeSeconds()
     */
    public long getTotalTimeMillis() {
        return getTotal(TimeUnit.MILLISECONDS);
    }

    /**
     * 获取所有任务的总花费时间（秒）
     *
     * @return 所有任务的总花费时间（秒）
     * @see #getTotalTimeNanos()
     * @see #getTotalTimeMillis()
     */
    public double getTotalTimeSeconds() {
        return getTotal(TimeUnit.SECONDS);
    }

    /**
     * 获取任务数
     *
     * @return 任务数
     */
    public int getTaskCount() {
        return taskCount.intValue();
    }

    /**
     * 获取任务列表
     *
     * @return 任务列表
     */
    public TaskInfo[] getTaskInfo() {
        if (null == this.taskMap) {
            throw new UnsupportedOperationException("Task info is not being kept!");
        }
        return this.taskMap.values().toArray(new TaskInfo[0]);
    }

    /**
     * 获取任务信息，类似于：
     * <pre>
     *     StopWatch '[id]': running time = [total] ns
     * </pre>
     *
     * @return 任务信息
     */
    public String shortSummary() {
        return shortSummary(null);
    }

    /**
     * 获取任务信息，类似于：
     * <pre>
     *     StopWatch '[id]': running time = [total] [unit]
     * </pre>
     *
     * @param unit 时间单位，{@code null}则默认为{@link TimeUnit#NANOSECONDS}
     * @return 任务信息
     */
    public String shortSummary(TimeUnit unit) {
        if (null == unit) {
            unit = TimeUnit.NANOSECONDS;
        }
        return StrUtil.format("StopWatch '{}': running time = {} {}",
                this.id, getTotal(unit), DateUtil.getShotName(unit));
    }

    /**
     * 生成所有任务的一个任务花费时间表，单位纳秒
     *
     * @return 任务时间表
     */
    public String prettyPrint() {
        return prettyPrint(null);
    }

    /**
     * 生成所有任务的一个任务花费时间表
     *
     * @param unit 时间单位，{@code null}则默认{@link TimeUnit#NANOSECONDS} 纳秒
     * @return 任务时间表
     * @since 5.7.16
     */
    public String prettyPrint(TimeUnit unit) {
        if (null == unit) {
            unit = TimeUnit.NANOSECONDS;
        }

        final StringBuilder sb = new StringBuilder(shortSummary(unit));
        sb.append(FileUtil.getLineSeparator());
        if (null == this.taskMap) {
            sb.append("No task info kept");
        } else {
            sb.append("---------------------------------------------").append(FileUtil.getLineSeparator());
            sb.append(DateUtil.getShotName(unit)).append("         %     Task name").append(FileUtil.getLineSeparator());
            sb.append("---------------------------------------------").append(FileUtil.getLineSeparator());

            final NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumIntegerDigits(9);
            nf.setGroupingUsed(false);

            final NumberFormat pf = NumberFormat.getPercentInstance();
            pf.setMinimumIntegerDigits(2);
            pf.setGroupingUsed(false);

            for (TaskInfo task : getTaskInfo()) {
                if (task.timeNanos == 0) {
                    task.timeNanos = maxNanoTime.get();
                }
                sb.append(nf.format(task.getTime(unit))).append("  ");
                sb.append(pf.format((double) task.getTimeNanos() / getTotalTimeNanos())).append("   ");
                sb.append(task.getTaskName()).append(FileUtil.getLineSeparator());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(shortSummary());
        if (null != this.taskMap) {
            for (TaskInfo task : getTaskInfo()) {
                sb.append("; [").append(task.getTaskName()).append("] took ").append(task.getTimeNanos()).append(" ns");
                long percent = Math.round(100.0 * task.getTimeNanos() / getTotalTimeNanos());
                sb.append(" = ").append(percent).append("%");
            }
        } else {
            sb.append("; no task info kept");
        }
        return sb.toString();
    }

    /**
     * 存放任务名称和花费时间对象
     *
     * @author Looly
     */
    public static final class TaskInfo {

        private final String taskName;
        private final long startTimeNanos;
        private long timeNanos = 0;

        private final AtomicLong maxNanoTime;

        /**
         * 构造
         *
         * @param taskName       任务名称
         * @param startTimeNanos 开始时间
         */
        TaskInfo(String taskName, long startTimeNanos, AtomicLong maxNanoTime) {
            this.taskName = taskName;
            this.startTimeNanos = startTimeNanos;
            this.maxNanoTime = maxNanoTime;
        }

        /**
         * 获取任务名
         *
         * @return 任务名
         */
        public String getTaskName() {
            return this.taskName;
        }

        /**
         * 获取指定单位的任务花费时间
         *
         * @param unit 单位
         * @return 任务花费时间
         * @since 5.7.16
         */
        public long getTime(TimeUnit unit) {
            return unit.convert(this.timeNanos, TimeUnit.NANOSECONDS);
        }

        /**
         * 获取任务花费时间（单位：纳秒）
         *
         * @return 任务花费时间（单位：纳秒）
         * @see #getTimeMillis()
         * @see #getTimeSeconds()
         */
        public long getTimeNanos() {
            return this.timeNanos;
        }

        /**
         * 获取任务花费时间（单位：毫秒）
         *
         * @return 任务花费时间（单位：毫秒）
         * @see #getTimeNanos()
         * @see #getTimeSeconds()
         */
        public long getTimeMillis() {
            return getTime(TimeUnit.MILLISECONDS);
        }

        /**
         * 获取任务花费时间（单位：秒）
         *
         * @return 任务花费时间（单位：秒）
         * @see #getTimeMillis()
         * @see #getTimeNanos()
         */
        public double getTimeSeconds() {
            return DateUtil.nanosToSeconds(this.timeNanos);
        }
    }
}
