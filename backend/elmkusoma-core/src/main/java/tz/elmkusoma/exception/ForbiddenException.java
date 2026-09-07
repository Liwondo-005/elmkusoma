package tz.elmkusoma.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("You do not have permission to access this resource");
    }
}
