package com.intuit.springwebclient.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class HttpClientConfig {

  private Integer connectTimeoutMillis = 30000;
  private Integer socketTimeoutMillis = 30000;
  private Map<String, Integer> maxTTLPerRoute = new HashMap<>();

  public void setMaxTTLForRoute(String route, Integer ttl) {
    maxTTLPerRoute.put(route, ttl);
  }

  public Integer getMaxTTLForRoute(String route) {
    return maxTTLPerRoute.get(route);
  }
}
