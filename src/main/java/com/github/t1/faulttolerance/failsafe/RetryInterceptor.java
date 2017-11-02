package com.github.t1.faulttolerance.failsafe;

import com.github.t1.stereotypes.Annotations;
import net.jodah.failsafe.*;
import net.jodah.failsafe.function.CheckedFunction;
import org.eclipse.microprofile.faulttolerance.*;

import javax.annotation.Priority;
import javax.interceptor.*;
import java.lang.reflect.*;

import static javax.interceptor.Interceptor.Priority.*;

@Retry
@Interceptor
@Priority(PLATFORM_AFTER)
public class RetryInterceptor {
    @AroundInvoke public Object intercept(InvocationContext context) {
        AnnotatedElement annotations = Annotations.on(context.getMethod());
        assert annotations.isAnnotationPresent(Retry.class);
        Integer retries = annotations.getAnnotation(Retry.class).maxRetries();
        RetryPolicy policy = new RetryPolicy().withMaxRetries(retries);
        SyncFailsafe<Object> failsafe = Failsafe.with(policy);
        if (annotations.isAnnotationPresent(Fallback.class))
            failsafe.withFallback(findFallback(annotations.getAnnotation(Fallback.class), context));
        return failsafe.get(context::proceed);
    }


    private CheckedFunction<? extends Throwable, ?> findFallback(Fallback annotation, InvocationContext context) {
        String methodName = annotation.fallbackMethod();
        Class<?> targetClass = context.getTarget().getClass();
        Class<?>[] parameters = { Throwable.class }; // getTypes(context.getParameters());
        try {
            Method method = targetClass.getDeclaredMethod(methodName, parameters);
            return throwable -> {
                try {
                    return method.invoke(context.getTarget(), throwable);
                } catch (InvocationTargetException e) {
                    throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
                }
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("can't find fallback method " + signature(methodName, parameters)
                    + " in " + targetClass.getName(), e);
        }
    }

    private Class<?>[] getTypes(Object[] objects) {
        Class[] types = new Class[objects.length];
        for (int i = 0; i < objects.length; i++)
            types[i] = objects[i].getClass();
        return types;
    }

    private String signature(String methodName, Class<?>[] args) {
        StringBuilder out = new StringBuilder();
        out.append(methodName).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                out.append(", ");
            out.append(args[i]);
        }
        out.append(")");
        return out.toString();
    }
}
