package api_brindes.controller;

import api_brindes.model.Usuario;
import api_brindes.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    // Porta onde o gerente manda login e senha
    @PostMapping
    public ResponseEntity efetuarLogin(@RequestBody @Valid Usuario dados) {
        try {
            // Empacota o login e senha e manda pro porteiro verificar
            var authenticationToken = new UsernamePasswordAuthenticationToken(dados.getLogin(), dados.getSenha());
            var authentication = manager.authenticate(authenticationToken);

            // Se a senha estiver certa, ele gera o crachá JWT e devolve na tela!
            var usuarioAutenticado = (Usuario) authentication.getPrincipal();
            var tokenJWT = tokenService.gerarToken(usuarioAutenticado.getLogin());

            // Retorna um JSON bonitinho com o token
            return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
        } catch (Exception e) {
            // Se a senha estiver errada, devolve erro 400 (Bad Request)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Uma "caixinha" interna (Record) só para devolver a resposta do token em formato JSON
    private record DadosTokenJWT(String token) {}
}