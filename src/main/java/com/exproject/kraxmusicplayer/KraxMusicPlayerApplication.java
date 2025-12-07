package com.exproject.kraxmusicplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KraxMusicPlayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KraxMusicPlayerApplication.class, args);
    }

}
