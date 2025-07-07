package com.dynatrace.easytrade.accountservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memory Leak Endpoint to simulate GC issues for Dynatrace demo
 * This controller creates various types of memory leaks that will trigger
 * garbage collection problems detectable by Dynatrace
 */
@RestController
@RequestMapping("/api/debug")
public class MemoryLeakEndpoint {
    
    // Static collection that grows indefinitely - classic memory leak
    private static final List<byte[]> memoryLeakList = new ArrayList<>();
    
    // Map that holds strong references preventing GC
    private static final Map<String, Object> cacheWithoutEviction = new HashMap<>();
    
    // Thread pool that's never shut down properly
    private static final ExecutorService leakyThreadPool = Executors.newFixedThreadPool(50);
    
    // Counter for tracking requests
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    /**
     * Endpoint 1: Creates a gradual memory leak
     * Each call adds 10MB to static collection that's never cleared
     */
    @GetMapping("/gradual-leak")
    public Map<String, Object> createGradualLeak(@RequestParam(defaultValue = "10") int mbToLeak) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Allocate specified MB of memory that won't be GC'd
            for (int i = 0; i < mbToLeak; i++) {
                byte[] leak = new byte[1024 * 1024]; // 1MB
                Arrays.fill(leak, (byte) 1); // Fill to ensure allocation
                memoryLeakList.add(leak);
            }
            
            response.put("status", "success");
            response.put("totalLeakedMB", memoryLeakList.size());
            response.put("message", "Added " + mbToLeak + "MB to memory leak");
            
        } catch (OutOfMemoryError e) {
            response.put("status", "error");
            response.put("message", "OutOfMemoryError occurred!");
            response.put("totalLeakedMB", memoryLeakList.size());
        }
        
        return response;
    }
    
    /**
     * Endpoint 2: Creates rapid memory pressure
     * Allocates large objects in tight loop causing frequent GC
     */
    @GetMapping("/gc-storm")
    public Map<String, Object> createGCStorm(@RequestParam(defaultValue = "100") int iterations) {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        List<byte[]> tempList = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            // Create temporary large objects that become garbage quickly
            byte[] temp = new byte[5 * 1024 * 1024]; // 5MB
            tempList.add(temp);
            
            // Every 10 iterations, clear half the list to trigger GC
            if (i % 10 == 0) {
                tempList.subList(0, tempList.size() / 2).clear();
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        response.put("status", "success");
        response.put("iterations", iterations);
        response.put("durationMs", duration);
        response.put("message", "GC storm completed - check GC logs");
        
        return response;
    }
    
    /**
     * Endpoint 3: String concatenation abuse
     * Creates massive string objects causing GC pressure
     */
    @GetMapping("/string-abuse")
    public Map<String, Object> createStringAbuse(@RequestParam(defaultValue = "10000") int iterations) {
        Map<String, Object> response = new HashMap<>();
        
        String result = "";
        for (int i = 0; i < iterations; i++) {
            // Inefficient string concatenation creating many intermediate objects
            result += "Iteration " + i + " creating garbage strings that need collection. ";
            
            // Also add to cache without limit
            cacheWithoutEviction.put("string_" + requestCounter.incrementAndGet(), result);
        }
        
        response.put("status", "success");
        response.put("cacheSize", cacheWithoutEviction.size());
        response.put("stringLength", result.length());
        response.put("message", "String concatenation abuse completed");
        
        return response;
    }
    
    /**
     * Endpoint 4: Thread leak simulation
     * Creates threads that hold references to large objects
     */
    @GetMapping("/thread-leak")
    public Map<String, Object> createThreadLeak(@RequestParam(defaultValue = "10") int threadCount) {
        Map<String, Object> response = new HashMap<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            leakyThreadPool.submit(() -> {
                // Each thread holds a large object
                byte[] threadLocal = new byte[10 * 1024 * 1024]; // 10MB per thread
                
                // Simulate work that never completes
                while (true) {
                    try {
                        Thread.sleep(60000); // Sleep for 1 minute
                        // Access the array to prevent optimization
                        threadLocal[0] = (byte) (threadLocal[0] + 1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
        }
        
        response.put("status", "success");
        response.put("threadsCreated", threadCount);
        response.put("message", "Thread leak created - threads holding memory references");
        
        return response;
    }
    
    /**
     * Endpoint 5: Circular reference creator
     * Creates objects with circular references that complicate GC
     */
    @GetMapping("/circular-reference")
    public Map<String, Object> createCircularReferences(@RequestParam(defaultValue = "1000") int count) {
        Map<String, Object> response = new HashMap<>();
        
        List<CircularNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CircularNode node1 = new CircularNode("Node1_" + i);
            CircularNode node2 = new CircularNode("Node2_" + i);
            CircularNode node3 = new CircularNode("Node3_" + i);
            
            // Create circular references
            node1.next = node2;
            node2.next = node3;
            node3.next = node1;
            
            // Store in static collection
            nodes.add(node1);
            cacheWithoutEviction.put("circular_" + i, nodes);
        }
        
        response.put("status", "success");
        response.put("circularReferencesCreated", count);
        response.put("totalCacheSize", cacheWithoutEviction.size());
        
        return response;
    }
    
    /**
     * Endpoint 6: Combined attack - triggers multiple GC issues simultaneously
     */
    @PostMapping("/gc-apocalypse")
    public Map<String, Object> triggerGCApocalypse() {
        Map<String, Object> response = new HashMap<>();
        
        // Trigger all types of GC issues
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> createGradualLeak(50));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> createGCStorm(50));
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> createStringAbuse(5000));
        CompletableFuture<Void> future4 = CompletableFuture.runAsync(() -> createThreadLeak(5));
        
        try {
            CompletableFuture.allOf(future1, future2, future3, future4).get(30, TimeUnit.SECONDS);
            response.put("status", "success");
            response.put("message", "GC apocalypse triggered - monitor in Dynatrace!");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to complete all GC attacks: " + e.getMessage());
        }
        
        // Add system stats
        Runtime runtime = Runtime.getRuntime();
        response.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        response.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
        response.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        response.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
        
        return response;
    }
    
    /**
     * Utility endpoint to check current memory status
     */
    @GetMapping("/memory-status")
    public Map<String, Object> getMemoryStatus() {
        Map<String, Object> response = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        response.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        response.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
        response.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        response.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
        response.put("leakedMemoryMB", memoryLeakList.size());
        response.put("cacheSize", cacheWithoutEviction.size());
        response.put("availableProcessors", runtime.availableProcessors());
        
        return response;
    }
    
    /**
     * Helper class for circular reference demonstration
     */
    private static class CircularNode {
        String data;
        CircularNode next;
        byte[] payload = new byte[1024 * 100]; // 100KB payload
        
        CircularNode(String data) {
            this.data = data;
        }
    }
}