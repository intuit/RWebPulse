package com.intuit.springwebclient.config;

import lombok.Builder;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
@Builder(toBuilder = true)
public class WebClientRetryConfig {
    @Builder.Default private final int maxAttempts = 0;
    @Builder.Default private final int backOff = 0; // Seconds
    @Builder.Default private final Predicate<? super Throwable> retryFilter = (Throwable ex) -> false;
}
