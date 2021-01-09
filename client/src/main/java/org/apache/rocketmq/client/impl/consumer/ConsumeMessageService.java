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
package org.apache.rocketmq.client.impl.consumer;

import java.util.List;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;

/**
 * 消息消费分发服务，
 * 通过submitConsumeRequest方法，接收消息消费分发任务，
 * ConsumeMessageConcurrentlyService：通过线程池并行的进行消息的分发；
 * ConsumeMessageOrderlyService：先将Pull到的消息放到本地另外一个缓存队列中，
 * 然后提交一个ConsumeRequest到消费线程池
 */
public interface ConsumeMessageService {
    void start();

    void shutdown();

    void updateCorePoolSize(int corePoolSize);

    void incCorePoolSize();

    void decCorePoolSize();

    int getCorePoolSize();

    ConsumeMessageDirectlyResult consumeMessageDirectly(final MessageExt msg, final String brokerName);

    /**
     * 将消息封装成线程池任务，提交给消费服务，消费服务再将消息传递给业务进行处理
     * 提交消息到消费服务，进行消费
     *
     * @param msgs             消息列表
     * @param processQueue     ProcessQueue
     * @param messageQueue     消息队列
     * @param dispathToConsume 是否转发给消费者
     */
    void submitConsumeRequest(
            final List<MessageExt> msgs,
            final ProcessQueue processQueue,
            final MessageQueue messageQueue,
            final boolean dispathToConsume);
}
