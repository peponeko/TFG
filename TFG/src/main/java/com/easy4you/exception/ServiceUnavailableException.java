package com.easy4you.exception;

public class ServiceUnavailableException extends RuntimeException {
  public ServiceUnavailableException(String message) {
    super(message);
  }
}

