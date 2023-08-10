package com.intuit.springwebclient.config;

import lombok.Data;

@Data
public class HttpConnectionPoolConfig {
    private int maxConnections = 400;
    private Long pendingAcquireTimeout = 31000L;
    private Long maxIdleTime = 31000L;
    private Long maxLifeTime = 300000L;
}
