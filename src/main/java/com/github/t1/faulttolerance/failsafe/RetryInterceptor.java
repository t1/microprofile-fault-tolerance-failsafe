package com.github.t1.faulttolerance.failsafe;

import com.github.t1.stereotypes.Annotations;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.annotation.Priority;
import javax.interceptor.*;
import java.lang.reflect.AnnotatedElement;

import static javax.interceptor.Interceptor.Priority.*;

@Retry
@Interceptor
@Priority(PLATFORM_AFTER)
public class RetryInterceptor extends FailsafeInterceptor {
    @AroundInvoke public Object intercept(InvocationContext context) {
        AnnotatedElement annotations = Annotations.on(context.getMethod());
        assert annotations.isAnnotationPresent(Retry.class);
        Integer retries = annotations.getAnnotation(Retry.class).maxRetries();
        retryPolicy(context).withMaxRetries(retries);
        return failsafe(context).get(context::proceed);
    }
}
