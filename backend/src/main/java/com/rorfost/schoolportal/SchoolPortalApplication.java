package com.rorfost.schoolportal;

import com.rorfost.schoolportal.common.config.AdminBootstrapProperties;
import com.rorfost.schoolportal.common.config.CorsProperties;
import com.rorfost.schoolportal.common.config.PortalProperties;
import com.rorfost.schoolportal.common.config.StorageProperties;
import com.rorfost.schoolportal.common.config.UploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(
    basePackageClasses = {
      AdminBootstrapProperties.class,
      CorsProperties.class,
      PortalProperties.class,
      StorageProperties.class,
      UploadProperties.class
    })
public class SchoolPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchoolPortalApplication.class, args);
  }
}
