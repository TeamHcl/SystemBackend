package com.hcl.systembackend.config;

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
    public OpenAPI systemBackendOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("System Backend Admin API")
                        .description("Admin transaction history and fraud detection endpoints.")
                        .version("v1")
                        .contact(new Contact()
                                .name("SystemBackend Team")))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Local backend server")));
    }
}
