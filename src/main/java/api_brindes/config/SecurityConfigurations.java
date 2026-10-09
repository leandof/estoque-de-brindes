package api_brindes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, exception) -> {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"mensagem\":\"Sessão ausente, inválida ou expirada. Faça login novamente.\"}");
                    })
                    .accessDeniedHandler((request, response, exception) -> {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"mensagem\":\"Você não tem permissão para esta operação.\"}");
                    }))

                /*
                 * Nossa aplicação usa JWT.
                 * Portanto não precisamos de CSRF.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )

                /*
                 * JWT é stateless:
                 * o servidor não guarda sessão.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Configuração das rotas.
                 */
                .authorizeHttpRequests(authorize -> authorize

                        /*
                         * ================================
                         * FRONT-END PÚBLICO
                         * ================================
                         *
                         * Precisamos permitir o navegador
                         * carregar os arquivos HTML/CSS/JS.
                         */

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/favicon.ico",
                                "/error"
                        )
                        .permitAll()


                        /*
                         * Pastas de arquivos estáticos.
                         *
                         * Mesmo que alguma delas ainda não
                         * exista, não há problema.
                         */
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/img/**",
                                "/assets/**"
                        )
                        .permitAll()


                        /*
                         * ================================
                         * LOGIN
                         * ================================
                         */

                        .requestMatchers(
                                "/login"
                        )
                        .permitAll()


                        /*
                         * ================================
                         * SWAGGER
                         * ================================
                         */

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        )
                        .permitAll()


                        /*
                         * ================================
                         * API PROTEGIDA
                         * ================================
                         *
                         * Itens, movimentações, usuários,
                         * relatórios etc. continuam
                         * exigindo JWT.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * Nosso filtro JWT executa antes
                 * do filtro padrão do Spring.
                 */
                .addFilterBefore(
                        securityFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }


    /*
     * Usado pelo AutenticacaoController
     * para validar login e senha.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }


    /*
     * Criptografia das senhas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}