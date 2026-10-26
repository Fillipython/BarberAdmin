package br.com.barberadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class BarberAdminApplication {

    public static void main(String[] args) {
        // Assegura fuso horário padrão de Brasília/São Paulo
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(BarberAdminApplication.class, args);
    }
}
