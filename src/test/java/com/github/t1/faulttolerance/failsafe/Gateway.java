package com.github.t1.faulttolerance.failsafe;

import javax.enterprise.inject.Stereotype;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Safe
@Retention(RUNTIME)
public @interface Gateway {
}
