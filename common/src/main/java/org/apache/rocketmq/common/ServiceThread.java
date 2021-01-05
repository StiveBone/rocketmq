/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;

public abstract class ServiceThread implements Runnable {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    private static final long JOIN_TIME = 90 * 1000;

    protected final Thread thread;
    protected final CountDownLatch2 waitPoint = new CountDownLatch2(1);
    protected volatile AtomicBoolean hasNotified = new AtomicBoolean(false);
    protected volatile boolean stopped = false;

    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }

    public abstract String getServiceName();

    public void start() {
        this.thread.start();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        this.stopped = true;
        log.info("shutdown thread " + this.getServiceName() + " interrupt " + interrupt);

        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }

        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            if (!this.thread.isDaemon()) {
                this.thread.join(this.getJointime());
            }
            long eclipseTime = System.currentTimeMillis() - beginTime;
            log.info("join thread " + this.getServiceName() + " eclipse time(ms) " + eclipseTime + " "
                + this.getJointime());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }

    public long getJointime() {
        return JOIN_TIME;
    }

    public void stop() {
        this.stop(false);
    }

    /**
     * 停止任务
     * @param interrupt
     */
    public void stop(final boolean interrupt) {
        this.stopped = true;
        log.info("stop thread " + this.getServiceName() + " interrupt " + interrupt);

        /**
         * 唤醒阻塞的任务
         */
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }

        if (interrupt) {
            this.thread.interrupt();
        }
    }

    public void makeStop() {
        this.stopped = true;
        log.info("makestop thread " + this.getServiceName());
    }

    public void wakeup() {
        /**
         * 设置hasNotified标识为true
         * 如果设置失败表明正在指定任务
         */
        if (hasNotified.compareAndSet(false, true)) {
            /**
             * 通知waitPoint点阻塞的线程
             */
            waitPoint.countDown(); // notify
        }
    }

    /**
     * 任务进入阻塞
     * @param interval
     */
    protected void waitForRunning(long interval) {
        /**
         * 设置等待通知标识为false，
         * 用于上次任务刚刚执行完成之后（hasNotified.set(false);）立马唤醒，这时不再等待，
         * 业务完成之后立马再次执行
         */
        if (hasNotified.compareAndSet(true, false)) {
            this.onWaitEnd();
            return;
        }

        //entry to wait
        /**
         * 重置为1
         */
        waitPoint.reset();

        try {
            /**
             * 阻塞线程
             */
            waitPoint.await(interval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        } finally {
            /**
             * 清除通知标识
             */
            hasNotified.set(false);
            this.onWaitEnd();
        }
    }

    protected void onWaitEnd() {
    }

    public boolean isStopped() {
        return stopped;
    }
}
