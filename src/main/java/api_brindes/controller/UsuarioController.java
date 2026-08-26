package api_brindes.controller;

import api_brindes.model.Usuario;
import api_brindes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Puxa o nosso criptografador BCrypt

    @PostMapping("/registrar")
    public ResponseEntity registrar(@RequestBody Usuario novoUsuario) {

        // 1. Verifica se o login já existe no banco para não dar conflito
        if (repository.findByLogin(novoUsuario.getLogin()) != null) {
            return ResponseEntity.badRequest().body("ERRO: Este usuário já existe!");
        }

        // 2. Pega a senha em texto puro (ex: "123456") e transforma no código gigante
        String senhaCriptografada = passwordEncoder.encode(novoUsuario.getSenha());

        // 3. Coloca a senha embaralhada de volta no usuário
        novoUsuario.setSenha(senhaCriptografada);

        // 4. Salva no banco de dados!
        repository.save(novoUsuario);

        return ResponseEntity.ok().body("Usuário criado com sucesso!");
    }
}