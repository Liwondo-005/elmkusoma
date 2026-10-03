package tz.elmkusoma.exception;

/**
 * Thrown when a layered authentication throttle is exhausted.
 * Always maps to HTTP 429 with a generic message (no account oracle).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many requests. Please try again later.");
    }
}
