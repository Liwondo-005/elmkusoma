package tz.elmkusoma.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI elmkusomaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ELMKUSOMA API")
                        .description("ELMKUSOMA Education Platform - Core Service API")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("ELMKUSOMA Team")
                                .email("api@elmkusoma.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development"),
                        new Server().url("https://api.elmkusoma.com").description("Production")));
    }
}
