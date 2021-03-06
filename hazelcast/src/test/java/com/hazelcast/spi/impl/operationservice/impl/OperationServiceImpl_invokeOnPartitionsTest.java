package com.hazelcast.spi.impl.operationservice.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.OperationFactory;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

import static com.hazelcast.spi.properties.GroupProperty.OPERATION_CALL_TIMEOUT_MILLIS;
import static com.hazelcast.spi.properties.GroupProperty.PARTITION_COUNT;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class OperationServiceImpl_invokeOnPartitionsTest extends HazelcastTestSupport {

    @Test
    public void test() throws Exception {
        Config config = new Config()
                .setProperty(PARTITION_COUNT.getName(), "" + 100);
        HazelcastInstance hz = createHazelcastInstance(config);
        OperationServiceImpl opService = getOperationServiceImpl(hz);

        Map<Integer, Object> result = opService.invokeOnAllPartitions(null, new OperationFactoryImpl());

        assertEquals(100, result.size());
        for (Map.Entry<Integer, Object> entry : result.entrySet()) {
            int partitionId = entry.getKey();
            assertEquals(partitionId * 2, entry.getValue());
        }
    }

    @Test
    public void testLongRunning() throws Exception {
        Config config = new Config()
                .setProperty(OPERATION_CALL_TIMEOUT_MILLIS.getName(), "2000")
                .setProperty(PARTITION_COUNT.getName(), "" + 100);
        TestHazelcastInstanceFactory hzFactory = createHazelcastInstanceFactory(2);
        HazelcastInstance hz1 = hzFactory.newHazelcastInstance(config);
        HazelcastInstance hz2 = hzFactory.newHazelcastInstance(config);
        warmUpPartitions(hz1, hz2);
        OperationServiceImpl opService = getOperationServiceImpl(hz1);

        Map<Integer, Object> result = opService.invokeOnAllPartitions(null, new SlowOperationFactoryImpl());

        assertEquals(100, result.size());
        for (Map.Entry<Integer, Object> entry : result.entrySet()) {
            int partitionId = entry.getKey();
            assertEquals(partitionId * 2, entry.getValue());
        }
    }

    private static class OperationFactoryImpl extends AbstractOperationFactor {
        @Override
        public Operation createOperation() {
            return new Operation() {

                private int response;

                @Override
                public void run() throws Exception {
                    response = getPartitionId() * 2;
                }

                @Override
                public Object getResponse() {
                    return response;
                }
            };
        }
    }

    private static class SlowOperationFactoryImpl extends AbstractOperationFactor {
        @Override
        public Operation createOperation() {
            return new Operation() {

                private int response;

                @Override
                public void run() throws Exception {
                    sleepSeconds(5);
                    response = getPartitionId() * 2;
                }

                @Override
                public Object getResponse() {
                    return response;
                }
            };
        }
    }

    private abstract static class AbstractOperationFactor implements OperationFactory {
        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
        }
    }
}
