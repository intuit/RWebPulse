package com.intuit.springwebclient.retryHandler;

/**
 * Interface for handling retries
 */
public interface RetryHandler {

  /**
   * Logic to retry or not based on an exception goes here
   * @param ex
   */
  void checkAndThrowRetriableException(Exception ex);
  String getName();
}
