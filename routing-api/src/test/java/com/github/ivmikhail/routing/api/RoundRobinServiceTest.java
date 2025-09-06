package com.github.ivmikhail.routing.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinServiceTest {

    private RoundRobinService rrService;
    private String server1;
    private String server2;
    private String server3;

    @BeforeEach
    void setUp() {
        rrService = new RoundRobinService(100); // 100ms maxKeepAlive
        server1 = "http://localhost:8081";
        server2 = "http://localhost:8082";
        server3 = "http://localhost:8083";
    }

    @Test
    void testRegisterAndGetAllServers() {
        rrService.heartbeat(server1);
        rrService.heartbeat(server2);

        var servers = rrService.getAllServers();
        assertEquals(2, servers.size());
        assertTrue(servers.contains(server1));
        assertTrue(servers.contains(server2));
    }

    @Test
    void testDeregister() {
        rrService.heartbeat(server1);
        rrService.heartbeat(server2);

        rrService.deregister(server1);

        var servers = rrService.getAllServers();

        assertEquals(1, servers.size());
        assertFalse(servers.contains(server1));
        assertTrue(servers.contains(server2));
    }

    @Test
    void testRoundRobinGetNext() {
        rrService.heartbeat(server1);
        rrService.heartbeat(server2);
        rrService.heartbeat(server3);

        Set<String> seen = new HashSet<>();
        // call getNext() multiple times
        for (int i = 0; i < 6; i++) {
            Optional<String> next = rrService.getNext();
            assertTrue(next.isPresent());
            seen.add(next.get());
        }

        // All three servers should have been returned at least once
        assertEquals(3, seen.size());
    }

    @Test
    void testGetNextEmpty() {
        Optional<String> next = rrService.getNext();
        assertTrue(next.isEmpty());
    }

    @Test
    void testHeartbeatAndCleanup() throws InterruptedException {
        rrService.heartbeat(server1);
        rrService.heartbeat(server2);

        Thread.sleep(150); // should expire both servers if call cleanup

        // simulate heartbeat for server1
        rrService.heartbeat(server1);

        int removed = rrService.cleanupDeadServers();

        // server2 should be removed (no heartbeat)
        assertEquals(1, removed);

        var servers = rrService.getAllServers();
        assertEquals(1, servers.size());
        assertTrue(servers.contains(server1));
    }

    @Test
    void testMultipleHeartbeatsPreventCleanup() throws InterruptedException {
        rrService.heartbeat(server1);

        for (int i = 0; i < 3; i++) {
            rrService.heartbeat(server1);
            Thread.sleep(50);
        }

        int removed = rrService.cleanupDeadServers();
        // server1 should NOT be removed because heartbeat refreshed
        assertEquals(0, removed);
        assertEquals(1, rrService.getAllServers().size());
    }
}
