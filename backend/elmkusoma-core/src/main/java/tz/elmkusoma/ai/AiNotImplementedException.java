package tz.elmkusoma.ai;

public class AiNotImplementedException extends RuntimeException {

    public AiNotImplementedException() {
        super("AI provider integration is not implemented in this deployment");
    }
}
