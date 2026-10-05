package api_brindes.config;

import api_brindes.repository.UsuarioRepository;
import api_brindes.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;


    public SecurityFilter(
            TokenService tokenService,
            UsuarioRepository usuarioRepository
    ) {

        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Procura:
         *
         * Authorization: Bearer eyJhbGciOi...
         */
        String token = recuperarToken(request);

        if (token != null) {

            /*
             * Descobre qual usuário está dentro do token.
             */
            String login = tokenService.getSubject(token);

            if (login != null) {

                /*
                 * Procura esse usuário no banco.
                 */
                UserDetails usuario =
                        usuarioRepository.findByLogin(login);

                if (usuario != null) {

                    /*
                     * Cria a autenticação do Spring.
                     */
                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    usuario,
                                    null,
                                    usuario.getAuthorities()
                            );

                    /*
                     * Informa ao Spring:
                     *
                     * "este usuário está autenticado"
                     */
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }
        }

        /*
         * Continua a requisição normalmente.
         */
        filterChain.doFilter(request, response);
    }


    /**
     * Recupera o JWT enviado no cabeçalho Authorization.
     */
    private String recuperarToken(HttpServletRequest request) {

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null) {
            return null;
        }

        if (!authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization
                .substring(7)
                .trim();
    }
}