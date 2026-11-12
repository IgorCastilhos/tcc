package br.unisinos.cvmsignals.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "br.unisinos.cvmsignals")
@EnableJpaRepositories(basePackages = "br.unisinos.cvmsignals")
@EntityScan(basePackages = "br.unisinos.cvmsignals")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
