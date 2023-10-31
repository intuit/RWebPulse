package com.intuit.springwebclient.retryHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryHandlerFactoryTest {
  
    @Test
    public void testAddAndGetHandler() {
        String handlerTestName = "retryHandlerTestName1";
        RetryHandler retryHandler1 = new RetryHandler() {
          
            @Override
            public void checkAndThrowRetriableException(Exception ex) {
                throw new RuntimeException("test exception");
            }

            @Override
            public String getName() {
                return "defaultHandlerName";
            }

        };

        RetryHandlerFactory.addHandler(handlerTestName, retryHandler1);
      
        Assertions.assertEquals(retryHandler1, RetryHandlerFactory.getHandler(handlerTestName));
        Assertions.assertEquals("defaultHandlerName", RetryHandlerFactory.getHandler(handlerTestName).getName());
        Assertions.assertNull(RetryHandlerFactory.getHandler("retryHandlerTestName2"));
    }
}
