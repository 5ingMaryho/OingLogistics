package com.oingmaryho.business.stockservice.batch.buffer;

import com.oingmaryho.business.stockservice.domain.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class OrderRequestBuffer {

    private final Queue<OrderCreatedEvent> queue = new ConcurrentLinkedQueue<>();

    public void add(OrderCreatedEvent event) {
        queue.add(event);
    }

    public List<OrderCreatedEvent> drainBatch(int maxSize) {
        List<OrderCreatedEvent> batch = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            OrderCreatedEvent event = queue.poll();
            if (event == null) break;
            batch.add(event);
        }
        return batch;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}

