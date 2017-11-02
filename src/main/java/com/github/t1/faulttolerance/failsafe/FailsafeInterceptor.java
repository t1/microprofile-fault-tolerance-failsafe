package com.github.t1.faulttolerance.failsafe;

import net.jodah.failsafe.*;

import javax.interceptor.InvocationContext;
import java.util.Map;
import java.util.function.Supplier;

public class FailsafeInterceptor {
    private static final String BASE = FailsafeInterceptor.class.getName();
    private static final String FAILSAFE = BASE + "#FAILSAFE";

    protected AccessibleSyncFailsafe<Object> failsafe(InvocationContext context) {
        return this.create(context, () -> new AccessibleSyncFailsafe<>(new RetryPolicy()));
    }

    protected RetryPolicy retryPolicy(InvocationContext context) { return failsafe(context).getRetryPolicy(); }

    protected AccessibleSyncFailsafe<Object> create(InvocationContext context,
            Supplier<AccessibleSyncFailsafe<Object>> supplier) {
        return computeIfAbsent(FAILSAFE, supplier, context.getContextData());
    }

    private <T> T computeIfAbsent(String key, Supplier<T> supplier, Map<String, Object> contextData) {
        @SuppressWarnings("unchecked") T o = (T) contextData.get(key);
        if (o == null)
            contextData.put(key, o = supplier.get());
        return o;
    }
}
