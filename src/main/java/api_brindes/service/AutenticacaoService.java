package api_brindes.service;

import api_brindes.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService
        implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;


    public AutenticacaoService(
            UsuarioRepository usuarioRepository
    ) {

        this.usuarioRepository = usuarioRepository;
    }


    /**
     * Spring chama automaticamente este método
     * quando alguém tenta fazer login.
     */
    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        UserDetails usuario =
                usuarioRepository.findByLogin(username);

        if (usuario == null) {

            throw new UsernameNotFoundException(
                    "Usuário não encontrado: " + username
            );
        }

        return usuario;
    }
}