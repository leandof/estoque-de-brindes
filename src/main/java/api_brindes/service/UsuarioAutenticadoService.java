package api_brindes.service;

import api_brindes.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioAutenticadoService {

    /**
     * Retorna o usuário atualmente autenticado.
     *
     * Esse usuário foi colocado no SecurityContext
     * pelo SecurityFilter após validar o JWT.
     */
    public Usuario obter() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        /*
         * Não existe autenticação.
         */
        if (authentication == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        /*
         * Existe autenticação, mas ela não está válida.
         */
        if (!authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        Object principal =
                authentication.getPrincipal();

        /*
         * Spring pode colocar "anonymousUser"
         * quando não existe usuário autenticado.
         */
        if (principal == null ||
                principal.equals("anonymousUser")) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        /*
         * O SecurityFilter colocou nosso Usuario
         * dentro do SecurityContext.
         */
        if (principal instanceof Usuario usuario) {

            return usuario;
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Não foi possível identificar o usuário autenticado."
        );
    }


    /**
     * Método alternativo.
     *
     * Mantemos este método para evitar problemas
     * caso alguma outra classe esteja usando o nome
     * getUsuarioAutenticado().
     */
    public Usuario getUsuarioAutenticado() {

        return obter();
    }
}