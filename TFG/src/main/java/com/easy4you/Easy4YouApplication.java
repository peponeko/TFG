package com.easy4you;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Easy4YouApplication {

  public static void main(String[] args) {
    SpringApplication.run(Easy4YouApplication.class, args);
  }
}

