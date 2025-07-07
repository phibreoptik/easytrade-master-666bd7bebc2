package com.dynatrace.easytrade.orderprocessing;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);
    
    // Problem 1: Unbounded cache without eviction
    private Map<String, Order> orderCache = new HashMap<>();
    
    // Problem 2: Static collection that grows indefinitely
    private static List<OrderEvent> allEvents = new ArrayList<>();
    
    // Problem 3: Thread-unsafe collection in concurrent environment
    private List<String> processingQueue = new ArrayList<>();
    
    public Order processOrder(String customerId, List<String> items) {
        String orderId = UUID.randomUUID().toString();
        
        // Problem 4: String concatenation in loop
        String orderDetails = "";
        for (String item : items) {
            orderDetails += "Item: " + item + ", ";
            orderDetails += "Price: " + calculatePrice(item) + ", ";
            orderDetails += "Tax: " + calculateTax(item) + "; ";
        }
        
        Order order = new Order(orderId, customerId, orderDetails);
        
        // Problem 5: Never removing from cache
        orderCache.put(orderId, order);
        
        // Problem 6: Creating unnecessary objects
        for (int i = 0; i < 100; i++) {
            OrderEvent event = new OrderEvent(
                "Processing step " + i,
                new Date(),
                order.deepCopy() // Unnecessary deep copy
            );
            allEvents.add(event);
        }
        
        // Problem 7: Not thread-safe
        processingQueue.add(orderId);
        
        return order;
    }
    
    @Scheduled(fixedDelay = 60000)
    public void processQueue() {
        // Problem 8: Processing entire list every time
        List<String> tempList = new ArrayList<>(processingQueue);
        for (String orderId : tempList) {
            // Simulate processing
            logger.info("Processing order: " + orderId);
        }
        // Problem 9: Not clearing the queue
    }
    
    private double calculatePrice(String item) {
        // Problem 10: Creating Random object each time
        Random random = new Random();
        return random.nextDouble() * 100;
    }
    
    private double calculateTax(String item) {
        return calculatePrice(item) * 0.1;
    }
    
    public List<Order> getAllOrders() {
        // Problem 11: Returning large collection without pagination
        return new ArrayList<>(orderCache.values());
    }
    
    public static class Order {
        private String id;
        private String customerId;
        private String details;
        private Date createdAt;
        private byte[] metadata;
        
        public Order(String id, String customerId, String details) {
            this.id = id;
            this.customerId = customerId;
            this.details = details;
            this.createdAt = new Date();
            // Problem 12: Large metadata for each order
            this.metadata = new byte[1024 * 1024]; // 1MB
        }
        
        public Order deepCopy() {
            Order copy = new Order(this.id, this.customerId, this.details);
            copy.metadata = Arrays.copyOf(this.metadata, this.metadata.length);
            return copy;
        }
        
        // Getters and setters...
    }
    
    public static class OrderEvent {
        private String description;
        private Date timestamp;
        private Order orderSnapshot;
        
        public OrderEvent(String description, Date timestamp, Order orderSnapshot) {
            this.description = description;
            this.timestamp = timestamp;
            this.orderSnapshot = orderSnapshot;
        }
    }
}