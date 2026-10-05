package api_brindes.controller;

import api_brindes.model.Usuario;
import api_brindes.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;


    public AutenticacaoController(
            AuthenticationManager authenticationManager,
            TokenService tokenService
    ) {

        this.authenticationManager =
                authenticationManager;

        this.tokenService =
                tokenService;
    }


    /**
     * Recebe:
     *
     * {
     *   "login": "usuario",
     *   "senha": "123"
     * }
     */
    @PostMapping
    public ResponseEntity<TokenResponse> login(
            @RequestBody
            @Valid
            LoginRequest dados
    ) {

        /*
         * Monta um objeto temporário contendo
         * login e senha enviados pelo usuário.
         */
        var authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        dados.login(),
                        dados.senha()
                );


        /*
         * Spring Security verifica usuário e senha.
         */
        var authentication =
                authenticationManager.authenticate(
                        authenticationToken
                );


        /*
         * Após autenticar, o principal será nosso Usuario.
         */
        Usuario usuario =
                (Usuario) authentication.getPrincipal();


        /*
         * Gera JWT.
         */
        String token =
                tokenService.gerarToken(usuario);


        /*
         * Retorna:
         *
         * {
         *     "token": "eyJhbG..."
         * }
         */
        return ResponseEntity.ok(
                new TokenResponse(token)
        );
    }


    /**
     * DTO utilizado somente pelo login.
     */
    public record LoginRequest(

            @NotBlank
            String login,

            @NotBlank
            String senha

    ) {
    }


    /**
     * DTO retornado depois do login.
     */
    public record TokenResponse(
            String token
    ) {
    }
}