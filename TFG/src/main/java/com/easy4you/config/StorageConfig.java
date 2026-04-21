package com.easy4you.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, AiProperties.class})
public class StorageConfig {}
