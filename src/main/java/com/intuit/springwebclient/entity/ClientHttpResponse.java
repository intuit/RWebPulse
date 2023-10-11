package com.intuit.springwebclient.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public final class ClientHttpResponse<T>{

    private final T response;
    private final String error;
    private final HttpStatus status;
    private final boolean isSuccess2xx;

}
