package com.intuit.springwebclient.config;

import lombok.Data;

@Data
public class HttpClientConfig {
    private Integer connectTimeoutMillis = 30000;
    private Integer socketTimeoutMillis = 30000;
}
