package api_brindes.config;

import api_brindes.model.Usuario;
import api_brindes.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Cria o primeiro usuário somente em desenvolvimento, mediante configuração explícita. */
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class UsuarioInicialLocalConfig {
    @Bean
    ApplicationRunner criarPrimeiroUsuario(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${BOOTSTRAP_LOGIN:}") String login,
            @Value("${BOOTSTRAP_PASSWORD:}") String senha) {
        return args -> {
            if (usuarios.count() != 0) return;
            if (login.isBlank() || login.trim().length() > 255 || senha.length() < 8
                    || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Configure BOOTSTRAP_LOGIN e BOOTSTRAP_PASSWORD (mínimo 8 caracteres, máximo 72 bytes).");
            }
            usuarios.save(new Usuario(login.trim(), encoder.encode(senha)));
        };
    }
}
