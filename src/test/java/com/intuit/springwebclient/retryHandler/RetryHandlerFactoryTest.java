package com.intuit.springwebclient.retryHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class RetryHandlerFactoryTest {
  
  private RetryHandlerFactory retryHandlerFactory;

  @BeforeEach
  public void setUp() {
    retryHandlerFactory = new RetryHandlerFactory();
  }

  @Test
  public void testAddAndGetHandler() {
    RetryHandler retryHandler = new RetryHandler();
    String handlerTestName = "default";

    retryHandlerFactory.addHandler(handlerTestName, retryHandler);

    Assertions.assertEquals(retryHandler, retryHandlerFactory.getHandler(handlerTestName));
  }

  @Test
  public void testGetNonExistentHandler() {
    String handlerTestName = "non-existent";

    Assertions.assertNull(retryHandlerFactory.getHandler(handlerTestName));
  }
}
