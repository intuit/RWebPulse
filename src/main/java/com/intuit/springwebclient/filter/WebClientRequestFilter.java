package com.intuit.springwebclient.filter;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * Interface to add https request filters (interceptors).
 */
public interface WebClientRequestFilter {
    default ExchangeFilterFunction getFilter() {
        return (request, next) -> next.exchange(execute(request));
    }
    default ClientRequest execute(ClientRequest request) {
        return request;
    }
    
}
