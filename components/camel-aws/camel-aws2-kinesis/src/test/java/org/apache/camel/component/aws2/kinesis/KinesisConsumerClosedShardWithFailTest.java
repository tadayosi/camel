/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aws2.kinesis;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.SequenceNumberRange;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KinesisConsumerClosedShardWithFailTest {

    @Mock
    private KinesisClient kinesisClient;
    @Mock
    private AsyncProcessor processor;

    private final CamelContext context = new DefaultCamelContext();
    private final Kinesis2Component component = new Kinesis2Component(context);

    private Kinesis2Consumer underTest;

    @BeforeEach
    public void setup() {
        SequenceNumberRange range = SequenceNumberRange.builder().endingSequenceNumber("20").build();
        Shard shard = Shard.builder().shardId("shardId").sequenceNumberRange(range).build();
        ArrayList<Shard> shardList = new ArrayList<>();
        shardList.add(shard);

        when(kinesisClient
                .getRecords(any(GetRecordsRequest.class)))
                .thenReturn(GetRecordsResponse.builder().nextShardIterator(null).build());
        when(kinesisClient
                .getShardIterator(any(GetShardIteratorRequest.class)))
                .thenReturn(GetShardIteratorResponse.builder().shardIterator("shardIterator").build());
        when(kinesisClient
                .listShards(any(ListShardsRequest.class)))
                .thenReturn(ListShardsResponse.builder().shards(shardList).build());

        component.start();

        Kinesis2Configuration configuration = new Kinesis2Configuration();
        configuration.setAmazonKinesisClient(kinesisClient);
        configuration.setIteratorType(ShardIteratorType.LATEST);
        configuration.setShardClosed(Kinesis2ShardClosedStrategyEnum.fail);
        configuration.setStreamName("streamName");

        Kinesis2Endpoint endpoint = new Kinesis2Endpoint("aws2-kinesis:foo", configuration, component);
        endpoint.start();
        underTest = new Kinesis2Consumer(endpoint, processor);
        underTest.setConnection(component.getConnection());
        underTest.start();
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> !(underTest.getCurrentShardList().isEmpty()));
    }

    @Test
    public void itObtainsAShardIteratorOnFirstPoll() {
        try {
            underTest.poll();
        } catch (Exception e) {
            fail("The first call should not throw an exception");
        }
        assertThrows(IllegalStateException.class, () -> {
            underTest.poll();
        });

        final ArgumentCaptor<GetShardIteratorRequest> getShardIteratorReqCap
                = ArgumentCaptor.forClass(GetShardIteratorRequest.class);
        final ArgumentCaptor<ListShardsRequest> getListShardsCap
                = ArgumentCaptor.forClass(ListShardsRequest.class);

        verify(kinesisClient).getShardIterator(getShardIteratorReqCap.capture());
        assertThat(getShardIteratorReqCap.getValue().streamName(), is("streamName"));
        assertThat(getShardIteratorReqCap.getValue().shardId(), is("shardId"));
        assertThat(getShardIteratorReqCap.getValue().shardIteratorType(), is(ShardIteratorType.LATEST));

        verify(kinesisClient, atLeastOnce()).listShards(getListShardsCap.capture());
        assertThat(getListShardsCap.getValue().streamName(), is("streamName"));
    }
}
