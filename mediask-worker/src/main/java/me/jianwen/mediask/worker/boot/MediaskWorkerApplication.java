package me.jianwen.mediask.worker.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "me.jianwen.mediask")
public class MediaskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaskWorkerApplication.class, args);
    }
}
