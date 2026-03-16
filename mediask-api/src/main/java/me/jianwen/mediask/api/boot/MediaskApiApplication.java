package me.jianwen.mediask.api.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "me.jianwen.mediask")
public class MediaskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaskApiApplication.class, args);
    }
}
