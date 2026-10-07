package tz.elmkusoma.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tz.elmkusoma.ai.dto.AiAskRequest;
import tz.elmkusoma.common.ApiResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private AiController aiController;

    @Test
    void health_reportsNotConfigured() {
        ResponseEntity<Map<String, Object>> result = aiController.health();

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = result.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("configured"));
        assertNull(body.get("provider"));
        assertNull(body.get("model"));
        assertEquals("AI_PROVIDER not set", body.get("reason"));
    }

    @Test
    void ask_returnsServiceUnavailableWithCodeWhenNotConfigured() {
        ResponseEntity<ApiResponse<Map<String, String>>> result =
                aiController.ask(new AiAskRequest("Explain fractions"));

        assertEquals(503, result.getStatusCode().value());
        ApiResponse<Map<String, String>> body = result.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("AI_NOT_CONFIGURED", body.getError());
        assertEquals("AI is not configured in this deployment", body.getMessage());
        verifyNoInteractions(aiService);
    }
}
