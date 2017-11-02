package net.jodah.failsafe;

/**
 * {@link FailsafeConfig#retryPolicy} is package private, but we want to access it later.
 */
public class AccessibleSyncFailsafe<R> extends SyncFailsafe<R> {
    public AccessibleSyncFailsafe(CircuitBreaker circuitBreaker) { super(circuitBreaker); }

    public AccessibleSyncFailsafe(RetryPolicy retryPolicy) { super(retryPolicy); }

    public RetryPolicy getRetryPolicy() { return retryPolicy; }
}
