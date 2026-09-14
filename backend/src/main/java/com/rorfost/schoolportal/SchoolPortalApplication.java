package com.rorfost.schoolportal;

import com.rorfost.schoolportal.common.config.CorsProperties;
import com.rorfost.schoolportal.common.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = {CorsProperties.class, StorageProperties.class})
public class SchoolPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchoolPortalApplication.class, args);
  }
}
