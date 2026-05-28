// order-service/common/exception/ServiceUnavailableException.java
package com.aviraj.order_service.common.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}