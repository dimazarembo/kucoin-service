package com.dzarembo.kucoinservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KucoinServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KucoinServiceApplication.class, args);
    }

}
