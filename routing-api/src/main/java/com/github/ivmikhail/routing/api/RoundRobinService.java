package com.github.ivmikhail.routing.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinService {
    private static final Logger logger = LoggerFactory.getLogger(RoundRobinService.class);

    private final CopyOnWriteArrayList<String> addresses;
    // (serverA -> lastHeartBeatEpochMillis), (serverB -> lastHeartBeatEpochMillis)
    private final Map<String, Long> heartbeatMap;
    private final AtomicInteger atomicInt;
    private final long maxKeepAliveMs;

    public RoundRobinService(long maxKeepAliveMs) {
        this.addresses = new CopyOnWriteArrayList<>();
        this.atomicInt = new AtomicInteger();
        this.heartbeatMap = new ConcurrentHashMap<>();
        this.maxKeepAliveMs = maxKeepAliveMs;
    }

    // same as register
    public void heartbeat(String address) {
        addresses.addIfAbsent(address);
        heartbeatMap.put(address, System.currentTimeMillis());
    }

    public void deregister(String addr) {
        logger.info("deregister {}", addr);
        heartbeatMap.remove(addr);
        addresses.remove(addr);
    }

    public Optional<String> getNext() {
        int size = addresses.size();
        if (size == 0) {
            return Optional.empty();
        }
        var addr = addresses.get(atomicInt.getAndIncrement() % size);
        logger.info("getNext, returning {}", addr);
        return Optional.of(addr);
    }

    public List<String> getAllServers() {
        return addresses;
    }

    public int cleanupDeadServers() {
        int total = addresses.size();

        addresses.removeIf(addr -> {
            Long lastBeat = heartbeatMap.get(addr);
            boolean isExpired = lastBeat == null || System.currentTimeMillis() - lastBeat > maxKeepAliveMs;

            if (isExpired) {
                heartbeatMap.remove(addr);
                logger.warn("{} removed due to missed heartbeat", addr);
            }
            return isExpired;
        });
        int remaining = addresses.size();
        int removed = total - remaining;

        logger.info("cleanup dead servers done, removed {}, remaining {}", removed, remaining);

        return removed;
    }
}
