package com.vicente.taskmanager.config;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String SCHEME = "bearer";
    private static final String BEARER_FORMAT = "JWT";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(this.apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(createComponents());
    }

    private Info apiInfo() {
        var contact = new Contact();
        var license = new License();
        contact.setEmail("vicenteluz1994@hotmail.com");
        contact.setName("Vicente Luz");
        contact.setUrl("https://www.linkedin.com/in/vicentejluz/");
        license.setName("MIT License");
        license.setUrl("https://github.com/vicentejluz/taskmanager/blob/main/LICENSE");
        return new Info()
                .title("Task Manager")
                .description(
                                    """
                                    REST API for managing tasks, deadlines and statuses.
                                    Includes scheduling, automatic status updates and JWT authentication.
                                    """)
                .license(license)
                .contact(contact)
                .version("0.6");
    }

    private Components createComponents() {
        var components = new Components();
        components.addSecuritySchemes(SECURITY_SCHEME_NAME, this.createSecurityScheme());
        return components;
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme(SCHEME).bearerFormat(BEARER_FORMAT);
    }
}
