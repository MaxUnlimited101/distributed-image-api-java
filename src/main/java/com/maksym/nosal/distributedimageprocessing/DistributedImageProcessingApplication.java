package com.maksym.nosal.distributedimageprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedImageProcessingApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DistributedImageProcessingApplication.class);

        String profile = System.getProperty("spring.profiles.active");
        if (profile.contains("worker")) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }

        app.run(args);
    }

}
