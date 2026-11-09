package com.firstpages;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class FirstpagesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstpagesBackendApplication.class, args);
    }

}
