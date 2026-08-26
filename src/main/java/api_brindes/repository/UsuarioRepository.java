package api_brindes.repository;

import api_brindes.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método mágico do Spring que busca um usuário pelo login no banco de dados
    UserDetails findByLogin(String login);

}