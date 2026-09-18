package tz.elmkusoma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "tz.elmkusoma.media.domain")
@EnableJpaRepositories(basePackages = "tz.elmkusoma.media.repository")
public class ElmkusomaMediaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElmkusomaMediaApplication.class, args);
    }
}
