package com.intuit.springwebclient.entity;

import com.intuit.springwebclient.config.WebClientRetryConfig;
import lombok.Builder;
import lombok.Getter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * The HttpRequest object.
 *
 * @param <REQUEST> the type parameter
 * @param <RESPONSE> the type parameter
 */
@Getter
@Builder(toBuilder = true)
public final class ClientHttpRequest<REQUEST, RESPONSE> {
    private final String url;
    private final REQUEST request;
    @Builder.Default
    private final ParameterizedTypeReference<REQUEST> requestType = new ParameterizedTypeReference<>() {};
    @Builder.Default
    private final ParameterizedTypeReference<RESPONSE> responseType = new ParameterizedTypeReference<>() {};
    @Builder.Default
    private final HttpHeaders requestHeaders = new HttpHeaders();
    @Builder.Default
    private final HttpMethod httpMethod = HttpMethod.GET;
    @Builder.Default private List<String> retryHandlers = new ArrayList<>();
    @Builder.Default private WebClientRetryConfig clientRetryConfig = WebClientRetryConfig.builder().build();
}
