package api_brindes.service;

import api_brindes.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    /*
     * A chave não é mais buscada diretamente em:
     *
     * ${JWT_SECRET}
     *
     * Agora ela vem do application.properties.
     */
    @Value("${api.security.token.secret}")
    private String secret;

    private static final String ISSUER = "api-brindes";


    /**
     * Gera um token JWT para o usuário autenticado.
     */
    public String gerarToken(Usuario usuario) {

        try {

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getLogin())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(gerarDataExpiracao())
                    .sign(algoritmo);

        } catch (JWTCreationException exception) {

            throw new RuntimeException(
                    "Erro ao gerar token JWT.",
                    exception
            );
        }
    }


    /**
     * Lê o token e devolve o login do usuário.
     *
     * O login foi salvo no Subject quando o token foi criado.
     */
    public String getSubject(String token) {

        try {

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            return JWT.require(algoritmo)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getSubject();

        } catch (JWTVerificationException exception) {

            return null;
        }
    }


    /**
     * Token válido por 2 horas.
     */
    private Instant gerarDataExpiracao() {

        return Instant.now()
                .plus(2, ChronoUnit.HOURS);
    }
}