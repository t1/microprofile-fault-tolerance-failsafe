package com.github.t1.faulttolerance;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface Fallback {
    String value();
}
