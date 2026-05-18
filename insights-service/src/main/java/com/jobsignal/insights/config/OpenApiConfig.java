package com.jobsignal.insights.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI insightsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobSignal Insights API")
                        .description("Weekly job market trend reports and skill analytics powered by AI")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Kanhaiya Kumar")
                                .email("kanhaiyakumar@durucooperation.com")));
    }
}
