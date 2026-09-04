package api_brindes.config;

import api_brindes.service.TokenService;
import api_brindes.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            try {
                // Tenta ler o crachá
                var login = tokenService.getSubject(tokenJWT);
                var usuario = repository.findByLogin(login);

                if (usuario != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException e) {
                // Se o crachá for inválido ou velho, ele cai aqui sem quebrar o servidor!
                // O Spring vai simplesmente barrar o acesso (Erro 403) de forma segura.
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        // Trava de segurança extra
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ") && !authorizationHeader.equals("Bearer null")) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}