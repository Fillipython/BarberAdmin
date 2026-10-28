package br.com.barberadmin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.location:uploads}")
    private String storageLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(storageLocation).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        log.info("Mapeando rota de uploads estáticos: /api/uploads/** e /uploads/** -> {}", uploadUri);

        registry.addResourceHandler("/api/uploads/**", "/uploads/**")
                .addResourceLocations(uploadUri)
                .setCachePeriod(3600); // 1 hora de cache no navegador
    }
}
