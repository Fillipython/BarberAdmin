package br.com.barberadmin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BarberAdmin API")
                        .version("1.0.0")
                        .description("API REST corporativa do BarberAdmin para gestão de barbearias, fluxo de caixa e ingestão de eventos do BarberNotify")
                        .contact(new Contact()
                                .name("BarberAdmin Engineering")
                                .email("contato@barberadmin.com"))
                        .license(new License().name("Proprietário").url("https://barberadmin.com")));
    }
}
