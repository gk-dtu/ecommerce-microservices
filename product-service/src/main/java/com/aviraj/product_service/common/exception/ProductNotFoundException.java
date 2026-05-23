package com.aviraj.product_service.common.exception;

public class ProductNotFoundException extends RuntimeException{
    public ProductNotFoundException(String message){super(message);}
}
