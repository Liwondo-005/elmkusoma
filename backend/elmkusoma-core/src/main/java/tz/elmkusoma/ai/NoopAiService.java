package tz.elmkusoma.ai;

import org.springframework.stereotype.Service;

@Service
public class NoopAiService implements AiService {

    @Override
    public String ask(String question, String context) {
        throw new AiNotImplementedException();
    }
}
