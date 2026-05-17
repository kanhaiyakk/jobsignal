package com.jobsignal.scraper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI scraperServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scraper Service API")
                        .description("Ingests job listings from public APIs and stores them for downstream processing")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("JobSignal")
                                .email("kanhaiyakumar@durucooperation.com")));
    }
}
