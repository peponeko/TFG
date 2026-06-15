package com.easy4you.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
  private String basePath = "./uploads";
  private DataSize maxFileSize = DataSize.ofMegabytes(50);
  private DataSize maxZipSize = DataSize.ofMegabytes(200);
}

