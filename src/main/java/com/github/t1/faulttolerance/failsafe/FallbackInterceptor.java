package com.github.t1.faulttolerance.failsafe;

import com.github.t1.stereotypes.Annotations;
import net.jodah.failsafe.SyncFailsafe;
import net.jodah.failsafe.function.CheckedFunction;
import org.eclipse.microprofile.faulttolerance.*;

import javax.annotation.Priority;
import javax.interceptor.*;
import java.lang.reflect.*;
import java.util.*;

import static javax.interceptor.Interceptor.Priority.*;

@Fallback
@Interceptor
@Priority(PLATFORM_AFTER)
public class FallbackInterceptor extends FailsafeInterceptor {
    @AroundInvoke public Object intercept(InvocationContext context) {
        AnnotatedElement annotations = Annotations.on(context.getMethod());
        assert annotations.isAnnotationPresent(Fallback.class);
        SyncFailsafe<Object> failsafe = failsafe(context);
        failsafe.withFallback(findFallback(annotations.getAnnotation(Fallback.class), context));
        return failsafe.get(context::proceed);
    }

    private CheckedFunction<? extends Throwable, ?> findFallback(Fallback annotation, InvocationContext context) {
        String methodName = annotation.fallbackMethod();
        if (methodName.isEmpty())
            return fallbackHandler(context, annotation.value());
        else
            return fallbackMethod(context, methodName);
    }

    private CheckedFunction<? extends Throwable, ?> fallbackHandler(InvocationContext invocationContext,
            Class<? extends FallbackHandler<?>> fallbackHandlerClass) {
        ExecutionContext executionContext = new ExecutionContext() {
            @Override public Method getMethod() { return invocationContext.getMethod(); }

            @Override public Object[] getParameters() { return invocationContext.getParameters(); }
        };
        return dummy -> newInstance(invocationContext, fallbackHandlerClass).handle(executionContext);
    }

    private FallbackHandler<?> newInstance(InvocationContext context, Class<? extends FallbackHandler<?>> type) {
        try {
            constructors:
            for (Constructor<?> c : type.getDeclaredConstructors()) {
                @SuppressWarnings("unchecked")
                Constructor<? extends FallbackHandler<?>> constructor = (Constructor<? extends FallbackHandler<?>>) c;
                List<Object> args = new ArrayList<>();
                for (Class<?> parameterType : constructor.getParameterTypes()) {
                    if (parameterType.isInstance(context.getTarget()))
                        args.add(context.getTarget());
                    else
                        continue constructors;
                }
                constructor.setAccessible(true);
                return constructor.newInstance(args.toArray());
            }
            throw new RuntimeException("no matching constructor for " + type.getName());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("can't instantiate fallback handler class " + type.getName(), e);
        }
    }

    private CheckedFunction<? extends Throwable, ?> fallbackMethod(InvocationContext context, String methodName) {
        Class<?> targetClass = context.getTarget().getClass();
        Class<?>[] parameters = { Throwable.class }; // getTypes(context.getParameters());
        try {
            Method method = targetClass.getDeclaredMethod(methodName, parameters);
            method.setAccessible(true);
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
