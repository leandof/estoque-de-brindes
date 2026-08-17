package api_brindes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Libera o acesso para TODAS as rotas (/itens, /movimentacoes)
                .allowedOrigins("*") // Permite que qualquer computador da intranet acesse a API
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permite operações de leitura e gravação
                .allowedHeaders("*");
    }
}