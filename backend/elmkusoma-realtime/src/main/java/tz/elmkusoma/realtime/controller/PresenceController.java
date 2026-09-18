package tz.elmkusoma.realtime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.realtime.handler.PresenceHandler;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/realtime/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceHandler presenceHandler;

    @GetMapping("/{institutionId}")
    public ResponseEntity<Map<String, Object>> getOnlineUsers(@PathVariable String institutionId) {
        Set<Object> onlineUsers = presenceHandler.getOnlineUsers(institutionId);
        return ResponseEntity.ok(Map.of(
                "institutionId", institutionId,
                "onlineUsers", onlineUsers,
                "count", onlineUsers.size()
        ));
    }

    @GetMapping("/{institutionId}/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserOnline(
            @PathVariable String institutionId,
            @PathVariable String userId) {
        boolean isOnline = presenceHandler.isUserOnline(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "institutionId", institutionId,
                "online", isOnline
        ));
    }
}
