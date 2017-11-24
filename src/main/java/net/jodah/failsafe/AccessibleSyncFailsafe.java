package net.jodah.failsafe;

/**
 * {@link FailsafeConfig#retryPolicy} and {@link FailsafeConfig#circuitBreaker} are package private,
 * but we want to reuse them after creation and don't want to store them separately.
 */
public class AccessibleSyncFailsafe<R> extends SyncFailsafe<R> {
    public AccessibleSyncFailsafe(CircuitBreaker circuitBreaker) { super(circuitBreaker); }

    public AccessibleSyncFailsafe(RetryPolicy retryPolicy) { super(retryPolicy); }

    public RetryPolicy getRetryPolicy() { return retryPolicy; }

    public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
}
