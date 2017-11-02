package com.github.t1.faulttolerance.failsafe;

import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.inject.Stereotype;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.*;

@Stereotype
@Retry(maxRetries = 0)
@Retention(RUNTIME)
public @interface Gateway {}
