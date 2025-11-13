package com.exproject.simplemusicplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SimpleMusicPlayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleMusicPlayerApplication.class, args);
    }

}
