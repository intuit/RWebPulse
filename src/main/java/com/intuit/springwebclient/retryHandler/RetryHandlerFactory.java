package com.intuit.springwebclient.retryHandler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * This class has the utility to get the retry handler implementation based on the name
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RetryHandlerFactory {

  private static final Map<String, RetryHandler> RETRY_HANDLER_MAP = new HashMap<>();

  /**
   * Adds a retry handler.
   *
   * @param retryHandlerName the retry handler name
   * @param retryHandler     the retry handler
   */
  public static void addHandler(String retryHandlerName, RetryHandler retryHandler) {

    RETRY_HANDLER_MAP.put(retryHandlerName, retryHandler);
  }

  /**
   * Gets a retry handler.
   *
   * @param handlerName input handler name
   * @return action handler impl
   */
  public static RetryHandler getHandler(String handlerName) {
    return RETRY_HANDLER_MAP.get(handlerName);
  }
}
