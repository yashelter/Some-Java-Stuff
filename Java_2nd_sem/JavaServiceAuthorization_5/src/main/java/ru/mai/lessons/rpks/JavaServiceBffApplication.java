package ru.mai.lessons.rpks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableFeignClients
@EnableJpaRepositories
@SpringBootApplication
@EnableCaching
public class JavaServiceBffApplication {

  public static void main(String[] args) {
    SpringApplication.run(JavaServiceBffApplication.class, args);
  }
}
