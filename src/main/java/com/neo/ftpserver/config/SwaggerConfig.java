package com.neo.ftpserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Value("${spring.application.name}")
    private String name;
    @Value("${spring.application.version}")
    private String version;
    @Value("${spring.application.des}")
    private String des;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title(name)
                        .version(version)
                        .description(des));
    }
}

