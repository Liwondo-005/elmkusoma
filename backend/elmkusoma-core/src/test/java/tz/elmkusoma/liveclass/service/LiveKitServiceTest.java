package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.Test;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LiveKitServiceTest {

    private LiveKitConfig config() {
        LiveKitConfig c = new LiveKitConfig();
        c.getServer().setUrl("ws://localhost:7880");
        c.getServer().setApiKey("devkey");
        c.getServer().setApiSecret("devsecret-devsecret-devsecret-dev01!");
        return c;
    }

    private static class StubService extends LiveKitService {
        final AtomicInteger probes = new AtomicInteger();
        final boolean reachable;

        StubService(LiveKitConfig config, boolean reachable) {
            super(config);
            this.reachable = reachable;
        }

        @Override
        protected boolean probeServer() {
            probes.incrementAndGet();
            return reachable;
        }
    }

    @Test
    void unavailableWhenNotConfiguredAndNeverProbes() {
        LiveKitConfig c = config();
        c.getServer().setApiKey("");
        StubService s = new StubService(c, true);
        assertFalse(s.isAvailable());
        assertEquals(0, s.probes.get());
    }

    @Test
    void reportsUnreachableServerAsUnavailable() {
        StubService s = new StubService(config(), false);
        assertFalse(s.isAvailable());
    }

    @Test
    void reportsReachableServerAsAvailable() {
        StubService s = new StubService(config(), true);
        assertTrue(s.isAvailable());
    }

    @Test
    void availabilityResultIsCached() {
        StubService s = new StubService(config(), false);
        assertFalse(s.isAvailable());
        assertFalse(s.isAvailable());
        assertFalse(s.isAvailable());
        assertEquals(1, s.probes.get(), "probe must be cached instead of running per call");
    }

    @Test
    void probeDetectsOpenLocalPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            LiveKitConfig c = config();
            c.getServer().setUrl("ws://127.0.0.1:" + socket.getLocalPort());
            assertTrue(new LiveKitService(c).probeServer());
        }
    }

    @Test
    void probeRejectsClosedLocalPort() {
        LiveKitConfig c = config();
        c.getServer().setUrl("ws://127.0.0.1:1");
        assertFalse(new LiveKitService(c).probeServer());
    }

    @Test
    void probeRejectsMalformedUrl() {
        LiveKitConfig c = config();
        c.getServer().setUrl("not a url");
        assertFalse(new LiveKitService(c).probeServer());
    }
}
