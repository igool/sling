/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.queue.impl.simple;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ScheduledDistributionQueueProcessorTask}
 */
public class ScheduledDistributionQueueProcessorTaskTest {

    @Test
    public void testRunWithNoQueue() throws Exception {
        SimpleDistributionQueueProvider queueProvider = mock(SimpleDistributionQueueProvider.class);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        ScheduledDistributionQueueProcessorTask scheduledDistributionQueueProcessorTask = new ScheduledDistributionQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledDistributionQueueProcessorTask.run();
    }

    @Test
    public void testRunWithOneEmptyQueue() throws Exception {
        SimpleDistributionQueueProvider queueProvider = mock(SimpleDistributionQueueProvider.class);
        Collection<DistributionQueue> queues = new LinkedList<DistributionQueue>();
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queue.isEmpty()).thenReturn(true);
        queues.add(queue);
        when(queueProvider.getQueues()).thenReturn(queues);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        ScheduledDistributionQueueProcessorTask scheduledDistributionQueueProcessorTask = new ScheduledDistributionQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledDistributionQueueProcessorTask.run();
    }

    @Test
    public void testRunWithOneNonEmptyQueue() throws Exception {
        SimpleDistributionQueueProvider queueProvider = mock(SimpleDistributionQueueProvider.class);
        Collection<DistributionQueue> queues = new LinkedList<DistributionQueue>();
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queue.isEmpty()).thenReturn(false).thenReturn(true);
        DistributionQueueItem item = mock(DistributionQueueItem.class);
        when(queue.getHead()).thenReturn(item);

        queues.add(queue);
        when(queueProvider.getQueues()).thenReturn(queues);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        ScheduledDistributionQueueProcessorTask scheduledDistributionQueueProcessorTask = new ScheduledDistributionQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledDistributionQueueProcessorTask.run();
    }
}