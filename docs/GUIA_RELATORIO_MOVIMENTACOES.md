# Relatório de Movimentações — implementação guiada

Base: repositório leandof/estoque-de-brindes, commit 446c66133248b292bf788970576b24a360e4c20a.
Branch de trabalho: feat/relatorio-movimentacoes. Alterações locais; não enviadas ao GitHub nem implantadas.

## O que foi preservado

Java 21, Spring Boot, MySQL, JPA, Spring Security/JWT, as entidades Item/Usuario/Movimentacao,
as pastas controller/service/repository/model/config e o frontend HTML/CSS/JavaScript.
Não foi criado outro sistema de login. DTOs são objetos de entrada/saída, não novas tabelas.
As cópias antigas de Java em src/main/resources não foram usadas: edite as classes de src/main/java.

## Decisões de compatibilidade

- O JSON da movimentação continua usando item: {id}, tipo e quantidade; observacao é opcional.
- IDs, responsável, data e saldos enviados pelo cliente são rejeitados com 400.
- Todos os usuários autenticados continuam consultando o histórico geral, conforme as permissões atuais.
- Usuario possui login, mas não nome completo; a tela usa uma coluna Responsável / login.
- Registros antigos ficam com autor e saldos não registrados. Não estimamos nem atribuímos autoria retroativa.
- Datas antigas permanecem como estavam; seu fuso depende do servidor antigo. Novas datas usam America/Sao_Paulo.
- Exclusão de item com histórico agora retorna 409. Não há endpoint para editar/excluir movimentações.
- Saldo inicial positivo de item novo gera ENTRADA auditada; zero não gera movimentação.
- POST /movimentacoes retorna 201 e DTO seguro; listagens antigas permanecem listas, mas o objeto item no histórico agora é um resumo e responsavel é ID/login.
- Consulta nova paginada, com tamanho de 1 a 100, ordenada por data e ID decrescentes.
- PDF/CSV não implementados. Botão desabilitado sinaliza a evolução; serviço e DTOs podem ser reutilizados.

## Executar localmente

1. Instale/configure JDK 21. Verifique java -version e ./mvnw -version.
2. Configure MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD e JWT_SECRET para seu ambiente de desenvolvimento.
3. Banco novo de desenvolvimento: o ddl-auto=update existente cria as tabelas e colunas.
4. Banco existente: siga a migração da Etapa 1 antes de iniciar a nova versão. Não execute o SQL duas vezes.
5. Execute ./mvnw spring-boot:run (Windows: .\mvnw.cmd spring-boot:run).
6. Acesse http://localhost:8080/login.html usando conta existente.
7. Execute ./mvnw test (Windows: .\mvnw.cmd test). O perfil de teste não usa seu MySQL.

O bootstrap da primeira conta e a distinção administrador/operador continuam sendo evoluções separadas.
O projeto ainda usa Double para preços; a presente entrega não faz migração monetária.

## Contrato da consulta

GET /movimentacoes/relatorio?dataInicial=2026-10-01&dataFinal=2026-10-31&responsavelId=1&tipo=SAIDA&itemId=2&pagina=0&tamanho=20

Todos os filtros são opcionais e se combinam por E. Data inicial e final incluem os respectivos dias.
O parâmetro responsavelId só seleciona registros existentes: não define a autoria de um POST.

```json
{
  "conteudo": [{
    "id": 10,
    "dataMovimentacao": "2026-10-05T10:45:00",
    "tipo": "SAIDA",
    "item": {"id": 2, "codigo": "CAN-01", "nome": "Caneca"},
    "quantidade": 20,
    "responsavel": {"id": 1, "login": "joao"},
    "observacao": "Entrega no evento",
    "estoqueAnterior": 100,
    "estoquePosterior": 80
  }],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

## Validação da entrega

- Java 21: compilação e 16 testes concluídos, zero falhas/erros (15 novos testes de integração + 1 teste de contexto).
- Interface: teste Playwright em Chromium headless aprovado (abas, filtros, vazio, limpar, datas inválidas, legado, texto seguro e largura de 390 px). API simulada nesse teste.
- git diff --check: sem erros de whitespace.
- O teste de período verifica inclusive 23:59:59 do último dia e exclusão do dia seguinte.
- A suíte verifica identidade de João/Maria, saldos, data automática, tentativa de falsificação, preservação de histórico e duas saídas simultâneas.
- Os testes de integração usam H2, não MySQL. Migração e bloqueios devem ser homologados numa cópia do MySQL antes da implantação.
- Nenhuma conexão ao banco de produção, alteração remota no GitHub ou implantação foi realizada.

Neste ambiente, o Mockito exigiu carregar previamente o agente Byte Buddy:
`./mvnw -DargLine=-javaagent:/caminho/byte-buddy-agent-1.14.18.jar test`.
Foi necessário apenas para a restrição de autoanexação da JVM do ambiente de execução.
Em um JDK local normal, tente primeiro `./mvnw test`. A dependência do agente já vem pela infraestrutura de testes.


## Como ler as etapas

Cada arquivo abaixo tem caminho, motivo, conexão, código completo e teste sugerido.
O código completo evita que você precise adivinhar imports ou onde inserir um trecho.
Estude primeiro UsuarioAutenticadoService e MovimentacaoService, depois acompanhe os DTOs e a tela.


## ETAPA 1 — Dependências, configurações e banco

### `pom.xml`

**Por que mudar:** Adiciona Bean Validation para validar DTOs e H2 apenas nos testes. Mantém Java 21, Spring Boot 3.3.2 e MySQL.

**Como se conecta:** O Maven disponibiliza as anotações de validação para os controllers e cria o banco isolado dos testes.

**Como testar:** Execute ./mvnw test (ou .\mvnw.cmd test no Windows), com JDK 21.

**Código completo:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.3.2</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.estoque</groupId>
	<artifactId>api-brindes</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>api-brindes</name>
	<description>Projeto de Controle de Estoque</description>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

		<!-- Web (Permite criar rotas e embutir o Tomcat) -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- Banco de Dados (Comunicação com o MySQL via código) -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>

		<!-- Driver oficial do MySQL -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- Swagger UI para documentar e testar a API -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.5.0</version>
		</dependency>

		<!-- Ferramentas de testes nativas do Spring Boot -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<!-- Spring Security: Tranca a API e gerencia os acessos -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>

		<!-- Auth0 Java JWT: Responsável por criar e validar os tokens "crachás" -->
		<dependency>
			<groupId>com.auth0</groupId>
			<artifactId>java-jwt</artifactId>
			<version>4.4.0</version>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<!-- Aponta exatamente onde está o seu arquivo principal -->
					<mainClass>api_brindes.ApiBrindesApplication</mainClass>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

### `src/main/resources/application.properties`

**Por que mudar:** Rejeita propriedades desconhecidas nos JSONs: o cliente não pode enviar ID, responsável, data ou saldos de uma movimentação. Também rejeita conversão silenciosa de quantidade decimal em inteiro.

**Como se conecta:** O Jackson aplica essas regras antes de o controller receber o DTO. A conexão MySQL existente é preservada.

**Como testar:** Envie responsavelId no POST: deve retornar 400, sem alterar saldo. Envie quantidade 1.5: deve retornar 400.

**Código completo:**

```properties
spring.application.name=api-brindes
# ── CONFIGURAÇÃO DO BANCO DE DADOS MYSQL ──
# ── CONFIGURAÇÃO DO HIBERNATE / JPA ──
# Atualiza o banco automaticamente sem apagar seus dados
spring.jpa.hibernate.ddl-auto=update
# Exibe os comandos SQL no console para você acompanhar o que o Spring está fazendo
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
# Se a nuvem fornecer as credenciais, o Java usa elas. Se não, usa as suas locais (para você continuar testando no seu PC).
spring.datasource.url=${MYSQL_URL:jdbc:mysql://localhost:3306/estoque_brindes?createDatabaseIfNotExist=true}
spring.datasource.username=${MYSQL_USER:root}
spring.datasource.password=${MYSQL_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
api.security.token.secret=${JWT_SECRET}
# Rejeita campos extras como responsavelId, id, estoqueAnterior e data no cadastro.
spring.jackson.deserialization.fail-on-unknown-properties=true
spring.jackson.mapper.accept-case-insensitive-enums=true
spring.jackson.deserialization.accept-float-as-int=false
```

### `docs/sql/01_relatorio_movimentacoes_mysql.sql`

**Por que mudar:** Migração manual aditiva para banco existente: adiciona relacionamento com usuário, observação, saldos, fotografias do produto e índices.

**Como se conecta:** A chave estrangeira aponta para usuarios.id. Campos novos são nulos nos registros antigos. Não se reconstrói autoria ou saldo sem evidência.

**Como testar:** Em uma cópia do MySQL anterior à mudança, com a aplicação parada, faça backup e execute uma única vez. Confira SHOW COLUMNS FROM movimentacao e SHOW CREATE TABLE movimentacao. Depois inicie com SPRING_JPA_HIBERNATE_DDL_AUTO=validate. Não execute após ddl-auto=update criar essas colunas.

**Código completo:**

```sql
-- Migração manual para banco EXISTENTE, anterior a esta funcionalidade.
-- Execute UMA VEZ, com aplicação parada e backup validado, ANTES de iniciar a nova versão.
-- Não executar depois de ddl-auto=update já ter criado essas colunas.
-- MySQL faz commit implícito de DDL. Não há rollback transacional de ALTER TABLE.
-- Verifique primeiro: tipos precisam ser ENTRADA/SAIDA, ignorando caixa.
SELECT DISTINCT tipo FROM movimentacao;

ALTER TABLE movimentacao
    ADD COLUMN id_responsavel BIGINT NULL,
    ADD COLUMN observacao VARCHAR(500) NULL,
    ADD COLUMN estoque_anterior INT NULL,
    ADD COLUMN estoque_posterior INT NULL,
    ADD COLUMN item_codigo VARCHAR(50) NULL,
    ADD COLUMN item_nome VARCHAR(100) NULL,
    ADD CONSTRAINT fk_movimentacao_responsavel
        FOREIGN KEY (id_responsavel) REFERENCES usuarios(id) ON DELETE RESTRICT;

CREATE INDEX idx_mov_data ON movimentacao(data_movimentacao, id);
CREATE INDEX idx_mov_responsavel_data ON movimentacao(id_responsavel, data_movimentacao);
CREATE INDEX idx_mov_item_data ON movimentacao(id_item, data_movimentacao);

-- Não preencher autor/saldos antigos: não existe evidência suficiente para reconstruí-los.
-- O converter Java lê ENTRADA/SAIDA em qualquer caixa sem mudar a coluna VARCHAR existente.
-- Depois da migração, pode-se usar SPRING_JPA_HIBERNATE_DDL_AUTO=validate.
-- Banco novo de desenvolvimento: ddl-auto=update cria tabelas/colunas, sem rodar este arquivo.
```

## ETAPA 2 — Entidade e tipo da movimentação

### `src/main/java/api_brindes/model/TipoMovimentacao.java`

**Por que mudar:** Enum restringe os tipos no Java a ENTRADA e SAIDA.

**Como se conecta:** É usado pelo DTO de entrada, pela entidade, pelo serviço e pelos filtros.

**Como testar:** ENTRADA e SAIDA devem funcionar; INVALIDO deve retornar 400.

**Código completo:**

```java
package api_brindes.model;

public enum TipoMovimentacao {
    ENTRADA, SAIDA
}
```

### `src/main/java/api_brindes/model/TipoMovimentacaoConverter.java`

**Por que mudar:** Converte o Enum para o VARCHAR já existente; lê dados antigos com caixa diferente. Evita mudar a coluna para um ENUM nativo específico do banco.

**Como se conecta:** O JPA chama o converter ao salvar e carregar Movimentacao.

**Como testar:** O teste legadoContinuaVisivelSemInventarAutorOuSaldos insere entrada em minúsculas e consulta pelo filtro ENTRADA.

**Código completo:**

```java
package api_brindes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Mantém o VARCHAR existente e aceita os valores legados em minúsculas. */
@Converter
public class TipoMovimentacaoConverter implements AttributeConverter<TipoMovimentacao, String> {
    public String convertToDatabaseColumn(TipoMovimentacao tipo) {
        return tipo == null ? null : tipo.name();
    }
    public TipoMovimentacao convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoMovimentacao.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
```

### `src/main/java/api_brindes/model/Movimentacao.java`

**Por que mudar:** Evolui a entidade existente com usuário, observação, saldos anterior/posterior e fotografia de código/nome do item. Os novos campos de auditoria não possuem setters públicos.

**Como se conecta:** ManyToOne liga a Usuario e Item. PrePersist gera horário de São Paulo. updatable=false evita que o Hibernate regrave as colunas auditadas em atualizações normais. Isso não protege contra um administrador alterando diretamente o banco.

**Como testar:** Registre saída 20 a partir de 100: histórico precisa guardar 100 e 80, mesmo após entradas posteriores. Data deve ser criada no servidor.

**Código completo:**

```java
package api_brindes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "movimentacao")
public class Movimentacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item", nullable = false, updatable = false)
    private Item item;

    // Nullable somente para preservar movimentações anteriores à auditoria.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsavel", updatable = false)
    private Usuario responsavel;

    @Convert(converter = TipoMovimentacaoConverter.class)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoMovimentacao tipo;

    @Column(nullable = false, updatable = false)
    private Integer quantidade;
    @Column(name = "data_movimentacao", updatable = false)
    private LocalDateTime dataMovimentacao;
    @Column(length = 500, updatable = false)
    private String observacao;
    @Column(name = "estoque_anterior", updatable = false)
    private Integer estoqueAnterior;
    @Column(name = "estoque_posterior", updatable = false)
    private Integer estoquePosterior;
    // Fotografia do produto; o relacionamento com Item continua existindo.
    @Column(name = "item_codigo", length = 50, updatable = false)
    private String itemCodigo;
    @Column(name = "item_nome", length = 100, updatable = false)
    private String itemNome;

    protected Movimentacao() {}

    public Movimentacao(Item item, Usuario responsavel, TipoMovimentacao tipo,
                        Integer quantidade, String observacao, int anterior, int posterior) {
        if (responsavel == null || responsavel.getId() == null) {
            throw new IllegalArgumentException("Responsável autenticado obrigatório.");
        }
        this.item = item;
        this.responsavel = responsavel;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.estoqueAnterior = anterior;
        this.estoquePosterior = posterior;
        this.itemCodigo = item.getCodigo();
        this.itemNome = item.getNome();
    }

    @PrePersist
    void preencherData() {
        dataMovimentacao = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    public Integer getId() { return id; }
    public Item getItem() { return item; }
    public Usuario getResponsavel() { return responsavel; }
    public TipoMovimentacao getTipo() { return tipo; }
    public Integer getQuantidade() { return quantidade; }
    public LocalDateTime getDataMovimentacao() { return dataMovimentacao; }
    public String getObservacao() { return observacao; }
    public Integer getEstoqueAnterior() { return estoqueAnterior; }
    public Integer getEstoquePosterior() { return estoquePosterior; }
    public String getItemCodigo() { return itemCodigo; }
    public String getItemNome() { return itemNome; }
}
```

## ETAPA 3 — Usuário existente e proteção de sua identidade

### `src/main/java/api_brindes/model/Usuario.java`

**Por que mudar:** Adiciona somente um construtor para criar contas a partir dos campos permitidos. Não inventa um nome completo: a entidade atual tem apenas login.

**Como se conecta:** Movimentacao aponta para o ID dessa mesma entidade; Spring Security continua usando UserDetails.

**Como testar:** Login existente deve continuar funcionando; o relatório mostra o login como responsável.

**Código completo:**

```java
package api_brindes.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String login;
    private String senha;

    public Usuario() {
    }

    public Usuario(String login, String senha) {
        this.login = login;
        this.senha = senha;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getSenha() { return senha; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
```

### `src/main/java/api_brindes/dto/CadastrarUsuarioDTO.java`

**Por que mudar:** Restringe o cadastro a login e senha; bloqueia uso de ID enviado pelo cliente para sobrescrever uma conta existente.

**Como se conecta:** O controller recebe esse DTO e constrói um Usuario novo.

**Como testar:** Com token válido, enviar id no cadastro deve retornar 400.

**Código completo:**

```java
package api_brindes.dto;
import jakarta.validation.constraints.*;
public record CadastrarUsuarioDTO(@NotBlank @Size(max = 255) String login,
                                 @NotBlank @Size(max = 72) String senha) {}
```

### `src/main/java/api_brindes/controller/UsuarioController.java`

**Por que mudar:** Reutiliza a verificação de login e BCrypt, agora com DTO validado em vez de entidade externa.

**Como se conecta:** Conversa com UsuarioRepository e PasswordEncoder como antes. A rota continua exigindo autenticação; não foi criada uma política nova de administrador.

**Como testar:** Cadastre usuário com token válido, depois faça login. Banco vazio ainda exige provisionar a primeira conta, como no projeto original.

**Código completo:**

```java
package api_brindes.controller;

import api_brindes.model.Usuario;
import api_brindes.dto.CadastrarUsuarioDTO;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> registrar(@RequestBody @Valid CadastrarUsuarioDTO dados) {
        Usuario novoUsuario = new Usuario(dados.login().trim(), dados.senha());

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
```

## ETAPA 4 — DTOs do relatório e da operação

### `src/main/java/api_brindes/dto/RegistrarMovimentacaoDTO.java`

**Por que mudar:** Define exatamente os dados permitidos: item.id, tipo, quantidade e observação opcional. Mantém o formato de envio atual do frontend.

**Como se conecta:** @Valid valida o objeto item interno. @Positive e @NotNull barram quantidades inválidas antes do serviço.

**Como testar:** Teste corpo vazio, item nulo, quantidade zero, negativa/decimal e observação acima de 500 caracteres: devem retornar 400.

**Código completo:**

```java
package api_brindes.dto;

import api_brindes.model.TipoMovimentacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

// Mantém o JSON atual: {"item":{"id":1},"tipo":"ENTRADA","quantidade":10}.
// Não aceita ID da movimentação, responsável, saldos ou data fornecidos pelo cliente.
public record RegistrarMovimentacaoDTO(
        @NotNull @Valid ItemReferencia item,
        @NotNull TipoMovimentacao tipo,
        @NotNull @Positive Integer quantidade,
        @Size(max = 500) String observacao) {
    public record ItemReferencia(@NotNull @Positive Integer id) {}
}
```

### `src/main/java/api_brindes/dto/MovimentacaoDTO.java`

**Por que mudar:** Define a resposta segura da API. Inclui somente ID/login do responsável, sem serializar senha, password ou authorities de Usuario.

**Como se conecta:** O serviço transforma a entidade em DTO dentro da transação, antes de fechar relações LAZY. Item usa fotografia histórica quando existe, e dados atuais como alternativa nos registros antigos.

**Como testar:** Confira responsável, observação e saldos no JSON. Procure senha/password: não devem aparecer.

**Código completo:**

```java
package api_brindes.dto;

import api_brindes.model.Movimentacao;
import api_brindes.model.TipoMovimentacao;
import java.time.LocalDateTime;

/** Projeção segura: nunca serializa Usuario nem seus hashes de senha. */
public record MovimentacaoDTO(Integer id, LocalDateTime dataMovimentacao,
        TipoMovimentacao tipo, ItemResumo item, Integer quantidade,
        ResponsavelResumo responsavel, String observacao,
        Integer estoqueAnterior, Integer estoquePosterior) {
    public record ItemResumo(Integer id, String codigo, String nome) {}
    public record ResponsavelResumo(Long id, String login) {}

    public static MovimentacaoDTO de(Movimentacao m) {
        var item = m.getItem();
        var usuario = m.getResponsavel();
        return new MovimentacaoDTO(m.getId(), m.getDataMovimentacao(), m.getTipo(),
                new ItemResumo(item.getId(),
                        m.getItemCodigo() == null ? item.getCodigo() : m.getItemCodigo(),
                        m.getItemNome() == null ? item.getNome() : m.getItemNome()),
                m.getQuantidade(), usuario == null ? null :
                        new ResponsavelResumo(usuario.getId(), usuario.getLogin()),
                m.getObservacao(), m.getEstoqueAnterior(), m.getEstoquePosterior());
    }
}
```

### `src/main/java/api_brindes/dto/PaginaMovimentacoesDTO.java`

**Por que mudar:** Representa conteúdo, número da página, tamanho, total de registros e total de páginas.

**Como se conecta:** O serviço converte Page do Spring em um contrato simples para a tela. A consulta e os DTOs podem ser reaproveitados numa exportação futura.

**Como testar:** Consulte tamanho=1 e páginas 0 e 1: os IDs devem ser diferentes, na ordem mais recente primeiro.

**Código completo:**

```java
package api_brindes.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaMovimentacoesDTO(List<MovimentacaoDTO> conteudo, int pagina,
        int tamanho, long totalElementos, int totalPaginas) {
    public static PaginaMovimentacoesDTO de(Page<MovimentacaoDTO> page) {
        return new PaginaMovimentacoesDTO(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
```

## ETAPA 5 — Repositórios e consulta ao banco

### `src/main/java/api_brindes/repository/MovimentacaoRepository.java`

**Por que mudar:** Corrige o ID de Long para Integer. Adiciona Specifications para filtros combináveis, consulta de responsáveis e verificação de histórico. Remove o método usado para apagar histórico junto com o item.

**Como se conecta:** MovimentacaoService usa consultas e Specification; ItemService usa existsByItemId para bloquear exclusão. EntityGraph carrega relações nas consultas de lista.

**Como testar:** Teste filtros isolados e combinados. Responsáveis disponíveis devem incluir somente contas com histórico, sem senhas.

**Código completo:**

```java
package api_brindes.repository;

import api_brindes.model.Movimentacao;
import api_brindes.model.Usuario;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Integer>,
        JpaSpecificationExecutor<Movimentacao> {
    boolean existsByItemId(Integer itemId);

    @EntityGraph(attributePaths = {"item", "responsavel"})
    List<Movimentacao> findAllByOrderByDataMovimentacaoDescIdDesc();

    @EntityGraph(attributePaths = {"item", "responsavel"})
    List<Movimentacao> findByItemIdOrderByDataMovimentacaoDescIdDesc(Integer itemId);

    @Query("select distinct m.responsavel from Movimentacao m where m.responsavel is not null order by m.responsavel.login")
    List<Usuario> findResponsaveisComMovimentacoes();
}
```

### `src/main/java/api_brindes/repository/ItemRepository.java`

**Por que mudar:** Adiciona findByIdForUpdate com bloqueio pessimista. Duas operações no mesmo item precisam esperar a transação anterior terminar.

**Como se conecta:** MovimentacaoService e ItemService usam o mesmo bloqueio ao movimentar, editar valor ou excluir.

**Como testar:** O teste concorrente dispara duas saídas de 70 sobre saldo 100: uma retorna 201, a outra 400; saldo final 30.

**Código completo:**

```java
package api_brindes.repository;

import api_brindes.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {


    // Serializa alterações do mesmo item durante a transação.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Integer id);

    List<Item> findByNomeContainingIgnoreCase(String nome);

    // E aqui está a busca pelo código exato do brinde
    Item findByCodigo(String codigo);
}
```

## ETAPA 6 — Capturar o usuário autenticado

### `src/main/java/api_brindes/service/UsuarioAutenticadoService.java`

**Por que mudar:** Lê o SecurityContextHolder que já é preenchido pelo SecurityFilter existente. Não recebe responsável do navegador.

**Como se conecta:** MovimentacaoService chama obter(); o principal precisa ser um Usuario autenticado com ID. Caso contrário, responde 401.

**Como testar:** Faça a operação com tokens de João e Maria. Cada registro deve receber o respectivo ID. Sem token, a cadeia de segurança bloqueia.

**Código completo:**

```java
package api_brindes.service;

import api_brindes.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioAutenticadoService {
    public Usuario obter() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof Usuario usuario)
                || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        return usuario;
    }
}
```

### `src/main/java/api_brindes/service/TokenService.java`

**Por que mudar:** Passa a usar a propriedade de segredo já existente e corrige expiração para duas horas reais com Instant. Facilita testar sem credenciais de produção.

**Como se conecta:** AutenticacaoController e SecurityFilter continuam chamando os mesmos métodos de geração e verificação.

**Como testar:** Os testes fazem login pela rota real e reutilizam o token; não simulam um usuário no SecurityContext.

**Código completo:**

```java
package api_brindes.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {


    private final String secret;

    public TokenService(@org.springframework.beans.factory.annotation.Value("${api.security.token.secret}") String secret) {
        this.secret = secret;
    }

    // Método que FABRICA o crachá
    public String gerarToken(String login) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("API Estoque Brindes")
                    .withSubject(login)
                    .withExpiresAt(dataExpiracao())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token jwt", exception);
        }
    }

    // Método que LÊ e VALIDA o crachá na porta de entrada
    public String getSubject(String tokenJWT) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("API Estoque Brindes")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    // O crachá vale por 2 horas, depois o usuário precisa logar de novo
    private Instant dataExpiracao() {
        return Instant.now().plus(java.time.Duration.ofHours(2));
    }
}
```

## ETAPAS 7 E 8 — Registrar entrada e saída na mesma regra

### `src/main/java/api_brindes/service/MovimentacaoService.java`

**Por que mudar:** Obtém o usuário, bloqueia o item, calcula saldo, grava o item e cria uma nova movimentação na mesma transação. Também concentra consultas, validação de filtros e montagem do DTO.

**Como se conecta:** Controller chama o serviço; serviço chama UsuarioAutenticadoService e os dois repositories. ENTRADA soma com proteção a estouro do inteiro. SAIDA verifica saldo e subtrai. A data final do relatório é inclusiva, usando limite exclusivo no início do dia seguinte.

**Como testar:** Execute entradaRegistraAutorSaldosHorarioEObservacao, saidaRegistraOutroAutorESaldos, saldoInsuficienteNaoGravaParcialmente, periodoIncluiDiaFinalInteiroEExcluiDiaSeguinte e filtraResponsavelTipoProdutoECombinacao.

**Código completo:**

```java
package api_brindes.service;

import api_brindes.dto.*;
import api_brindes.model.*;
import api_brindes.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovimentacaoService {
    private final MovimentacaoRepository repository;
    private final ItemRepository itens;
    private final UsuarioAutenticadoService usuarioAutenticado;

    public MovimentacaoService(MovimentacaoRepository repository, ItemRepository itens,
                               UsuarioAutenticadoService usuarioAutenticado) {
        this.repository = repository;
        this.itens = itens;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Transactional
    public MovimentacaoDTO registrar(RegistrarMovimentacaoDTO dados) {
        var usuario = usuarioAutenticado.obter();
        if (dados.item() == null || dados.item().id() == null || dados.tipo() == null
                || dados.quantidade() == null || dados.quantidade() <= 0) {
            throw new IllegalArgumentException("Informe item, tipo e quantidade positiva.");
        }
        Item item = itens.findByIdForUpdate(dados.item().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
        int anterior = item.getQuantidade();
        if (dados.tipo() == TipoMovimentacao.SAIDA && anterior < dados.quantidade()) {
            throw new IllegalArgumentException("Estoque insuficiente. Disponível: " + anterior);
        }
        final int posterior;
        try {
            posterior = dados.tipo() == TipoMovimentacao.ENTRADA
                    ? Math.addExact(anterior, dados.quantidade()) : anterior - dados.quantidade();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Quantidade acima do limite suportado.");
        }
        item.setQuantidade(posterior);
        itens.save(item);
        var movimentacao = new Movimentacao(item, usuario, dados.tipo(), dados.quantidade(),
                dados.observacao(), anterior, posterior);
        return MovimentacaoDTO.de(repository.save(movimentacao));
    }

    public List<MovimentacaoDTO> listarTodas() {
        return repository.findAllByOrderByDataMovimentacaoDescIdDesc().stream().map(MovimentacaoDTO::de).toList();
    }

    public List<MovimentacaoDTO> listarPorItem(Integer id) {
        return repository.findByItemIdOrderByDataMovimentacaoDescIdDesc(id).stream().map(MovimentacaoDTO::de).toList();
    }

    public List<MovimentacaoDTO.ResponsavelResumo> responsaveis() {
        return repository.findResponsaveisComMovimentacoes().stream()
                .map(u -> new MovimentacaoDTO.ResponsavelResumo(u.getId(), u.getLogin())).toList();
    }

    // Um único método combina filtros; cada um também funciona isoladamente.
    public PaginaMovimentacoesDTO consultar(LocalDate inicio, LocalDate fim, Long responsavelId,
            TipoMovimentacao tipo, Integer itemId, int pagina, int tamanho) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        }
        if (fim != null && fim.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Data final fora do intervalo suportado.");
        }
        if (pagina < 0 || tamanho < 1 || tamanho > 100
                || (responsavelId != null && responsavelId <= 0) || (itemId != null && itemId <= 0)) {
            throw new IllegalArgumentException("Filtros ou paginação inválidos (tamanho: 1 a 100).");
        }
        Specification<Movimentacao> filtros = (root, query, cb) -> {
            // Fetch apenas na consulta de dados; a contagem não pode conter fetch joins.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("item");
                root.fetch("responsavel", jakarta.persistence.criteria.JoinType.LEFT);
            }
            List<Predicate> condicoes = new ArrayList<>();
            if (inicio != null) condicoes.add(cb.greaterThanOrEqualTo(root.get("dataMovimentacao"), inicio.atStartOfDay()));
            if (fim != null) condicoes.add(cb.lessThan(root.get("dataMovimentacao"), fim.plusDays(1).atStartOfDay()));
            if (responsavelId != null) condicoes.add(cb.equal(root.get("responsavel").get("id"), responsavelId));
            if (tipo != null) condicoes.add(cb.equal(cb.upper(root.get("tipo").as(String.class)), tipo.name()));
            if (itemId != null) condicoes.add(cb.equal(root.get("item").get("id"), itemId));
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        var ordem = Sort.by(Sort.Order.desc("dataMovimentacao"), Sort.Order.desc("id"));
        return PaginaMovimentacoesDTO.de(repository.findAll(filtros, PageRequest.of(pagina, tamanho, ordem))
                .map(MovimentacaoDTO::de));
    }
}
```

## ETAPA 9 — API do relatório

### `src/main/java/api_brindes/controller/MovimentacaoController.java`

**Por que mudar:** Mantém POST /movimentacoes e as consultas anteriores; adiciona GET /movimentacoes/relatorio e /responsaveis. POST bem-sucedido agora retorna 201.

**Como se conecta:** O controller só recebe parâmetros, aplica validação e chama o serviço. Não calcula saldo nem escolhe responsável. As rotas continuam protegidas pela configuração existente.

**Como testar:** Use testes.http: combine dataInicial, dataFinal, responsavelId, tipo e itemId. ID de responsável no GET é somente um filtro, nunca um comando de autoria.

**Código completo:**

```java
package api_brindes.controller;

import api_brindes.dto.*;
import api_brindes.model.TipoMovimentacao;
import api_brindes.service.MovimentacaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService service;
    public MovimentacaoController(MovimentacaoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MovimentacaoDTO> registrar(@RequestBody @Valid RegistrarMovimentacaoDTO dados) {
        return ResponseEntity.status(201).body(service.registrar(dados));
    }
    @GetMapping
    public List<MovimentacaoDTO> listarTodas() { return service.listarTodas(); }
    @GetMapping("/item/{idItem}")
    public List<MovimentacaoDTO> listarPorItem(@PathVariable Integer idItem) { return service.listarPorItem(idItem); }
    @GetMapping("/responsaveis")
    public List<MovimentacaoDTO.ResponsavelResumo> responsaveis() { return service.responsaveis(); }

    @GetMapping("/relatorio")
    public PaginaMovimentacoesDTO relatorio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.consultar(dataInicial, dataFinal, responsavelId, tipo, itemId, pagina, tamanho);
    }
}
```

### `src/main/java/api_brindes/config/ApiExceptionHandler.java`

**Por que mudar:** Padroniza erros de validação, JSON inválido, regras, ausência de item e conflitos de banco sem expor detalhes SQL.

**Como se conecta:** @RestControllerAdvice atende os controllers; o JavaScript lê mensagem e erros para explicar a falha.

**Como testar:** Estoque insuficiente retorna 400; item inexistente 404; exclusão com histórico 409.

**Código completo:**

```java
package api_brindes.config;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> regra(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensagem",
                e.getReason() == null ? "Operação não permitida." : e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
        var campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Verifique os campos informados.", "erros", campos));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> formato(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem",
                "Dados inválidos ou campos não permitidos. Use ENTRADA/SAIDA e datas AAAA-MM-DD."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflito(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("mensagem",
                "Conflito de dados: código duplicado ou registro vinculado a histórico."));
    }
}
```

## ETAPA 10 — Preservar histórico e auditar saldo inicial

### `src/main/java/api_brindes/dto/CadastrarItemDTO.java`

**Por que mudar:** Limita cadastro a código, nome, quantidade inicial e preço. Não aceita ID para substituir item existente.

**Como se conecta:** ItemController valida e ItemService cria um novo item.

**Como testar:** Envie id no cadastro: 400. Envie quantidade inicial positiva: deve gerar entrada auditada.

**Código completo:**

```java
package api_brindes.dto;

import jakarta.validation.constraints.*;

public record CadastrarItemDTO(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 100) String nome,
        @NotNull @PositiveOrZero Integer quantidade,
        @NotNull @PositiveOrZero Double valor) {}
```

### `src/main/java/api_brindes/dto/AtualizarValorDTO.java`

**Por que mudar:** Limita a atualização de preço ao valor não negativo.

**Como se conecta:** ItemController passa ao ItemService; não permite alterar estoque pela rota de preço.

**Como testar:** PUT com valor negativo ou quantidade extra deve retornar 400.

**Código completo:**

```java
package api_brindes.dto;
import jakarta.validation.constraints.*;
public record AtualizarValorDTO(@NotNull @PositiveOrZero Double valor) {}
```

### `src/main/java/api_brindes/service/ItemService.java`

**Por que mudar:** Centraliza cadastro, exclusão e edição de valor. O cadastro começa com saldo zero e registra uma entrada para o saldo inicial positivo. Excluir item com qualquer histórico retorna 409.

**Como se conecta:** Reutiliza MovimentacaoService na mesma transação. Assim, falha na entrada inicial desfaz também o cadastro do item. O bloqueio compartilhado protege contra edição de valor sobrescrever saldo simultâneo.

**Como testar:** Cadastre 12 unidades: a entrada precisa mostrar 0 → 12 e seu usuário. Tente excluir: deve preservar os registros e retornar 409.

**Código completo:**

```java
package api_brindes.service;

import api_brindes.dto.*;
import api_brindes.model.Item;
import api_brindes.model.TipoMovimentacao;
import api_brindes.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemService {
    private final ItemRepository itens;
    private final MovimentacaoRepository movimentacoes;
    private final MovimentacaoService movimentacaoService;

    public ItemService(ItemRepository itens, MovimentacaoRepository movimentacoes, MovimentacaoService movimentacaoService) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
        this.movimentacaoService = movimentacaoService;
    }

    @Transactional
    public Item cadastrar(CadastrarItemDTO dados) {
        // Nunca recebe ID nem altera saldo de item existente através do cadastro.
        Item item = itens.save(new Item(dados.codigo().trim(), dados.nome().trim(), 0, dados.valor()));
        if (dados.quantidade() > 0) {
            movimentacaoService.registrar(new RegistrarMovimentacaoDTO(
                    new RegistrarMovimentacaoDTO.ItemReferencia(item.getId()), TipoMovimentacao.ENTRADA,
                    dados.quantidade(), "Estoque inicial do cadastro"));
        }
        return item;
    }

    @Transactional
    public void apagar(Integer id) {
        var item = buscarComBloqueio(id);
        if (movimentacoes.existsByItemId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este item possui histórico e não pode ser excluído.");
        }
        itens.delete(item);
    }

    @Transactional
    public Item atualizarValor(Integer id, AtualizarValorDTO dados) {
        // Mesmo bloqueio da movimentação: evita sobrescrever um saldo concorrente.
        var item = buscarComBloqueio(id);
        item.setValor(dados.valor());
        return itens.save(item);
    }

    private Item buscarComBloqueio(Integer id) {
        return itens.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
    }
}
```

### `src/main/java/api_brindes/controller/ItemController.java`

**Por que mudar:** Mantém listagem e indicadores existentes, delegando mutações ao ItemService.

**Como se conecta:** DTOs controlam a entrada; service protege saldo e auditoria; repository continua atendendo leituras simples.

**Como testar:** Verifique que o inventário, edição de preço e indicadores antigos continuam funcionando. Item sem histórico continua podendo ser excluído.

**Código completo:**

```java
package api_brindes.controller;

import api_brindes.model.Item;
import api_brindes.dto.CadastrarItemDTO;
import api_brindes.dto.AtualizarValorDTO;
import api_brindes.service.ItemService;
import jakarta.validation.Valid;
import api_brindes.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/itens")
public class ItemController {
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public List<Item> listarTodos() {
        return itemRepository.findAll();
    }

    @PostMapping
    public Item salvar(@RequestBody @Valid CadastrarItemDTO item) {
        return itemService.cadastrar(item);
    }

    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> obterRelatorio() {
        List<Item> itens = itemRepository.findAll();

        int quantidadeTotal = 0;
        double patrimonioTotal = 0.0;

        // Faz a matemática real lendo o banco de dados
        for (Item item : itens) {
            quantidadeTotal += item.getQuantidade();
            patrimonioTotal += (item.getQuantidade() * item.getValor());
        }

        Map<String, Object> relatorio = new HashMap<>();

        // A MÁGICA ACONTECE AQUI: Nomes idênticos ao seu HTML!
        relatorio.put("totalDeBrindesCadastrados", quantidadeTotal);
        relatorio.put("valorTotalArmazenado", patrimonioTotal);

        return ResponseEntity.ok(relatorio);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        itemService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> atualizarValor(@PathVariable Integer id,
                                             @RequestBody @Valid AtualizarValorDTO dados) {
        return ResponseEntity.ok(itemService.atualizarValor(id, dados));
    }
}
```

## ETAPA 11 — Aba, filtros e comportamento da tela

### `src/main/resources/static/index.html`

**Por que mudar:** Adiciona navegação Estoque / Relatório de Movimentações, cinco filtros, tabela, paginação, observação no formulário e exportação futura desabilitada. Mantém HTML/CSS/JS sem framework novo.

**Como se conecta:** api() envia JWT e trata respostas. aplicarFiltros() monta parâmetros; consultarRelatorio() consulta a API. celula() usa textContent para impedir dados de virarem HTML. O envio da movimentação não contém autoria.

**Como testar:** Faça login, abra a aba, filtre por responsável/tipo/item/período, limpe e teste vazio. Em 390px a tabela deve rolar dentro do contêiner. tests/ui-relatorio.cjs testa a interface com API simulada.

**Código completo:**

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Truckvan - Controle de Estoque</title>

    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='20' fill='%23b32025'/><text x='50' y='75' font-size='75' font-family='Arial' font-weight='bold' fill='white' text-anchor='middle'>T</text></svg>">

    <style>
        :root {
            --vermelho-truckvan: #b32025;
            --vermelho-escuro: #8a171c;
            --cinza-fundo: #f4f4f9;
        }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: var(--cinza-fundo); margin: 0; }
        .header { background-color: var(--vermelho-truckvan); color: white; padding: 20px; border-bottom: 5px solid var(--vermelho-escuro); display: flex; align-items: center; justify-content: center; gap: 20px; }
        .logo-t { background-color: white; color: var(--vermelho-truckvan); font-size: 50px; font-weight: 900; width: 75px; height: 75px; display: flex; align-items: center; justify-content: center; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); font-family: 'Arial Black', sans-serif; }
        .header-text { text-align: left; }
        .header h1 { margin: 0; font-size: 2.5em; letter-spacing: 2px; font-weight: 900; line-height: 1; }
        .header p { margin: 5px 0 0 0; font-size: 1.1em; opacity: 0.9; }
        .container { max-width: 1100px; margin: 30px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        .dashboard { display: flex; gap: 20px; margin-bottom: 30px; }
        .card { background: var(--vermelho-truckvan); color: white; padding: 20px; border-radius: 8px; flex: 1; text-align: center; box-shadow: 0 3px 6px rgba(0,0,0,0.15); }
        .card h3 { margin: 0; font-size: 1.1em; font-weight: normal; text-transform: uppercase; letter-spacing: 1px; }
        .card p { margin: 10px 0 0; font-size: 2.5em; font-weight: bold; }
        .painel-acoes { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px; border-left: 5px solid var(--vermelho-truckvan); }
        .painel-acoes h3 { margin-top: 0; color: #333; }
        input, select { padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
        button { background-color: var(--vermelho-truckvan); color: white; border: none; padding: 10px 20px; font-size: 14px; font-weight: bold; border-radius: 4px; cursor: pointer; transition: 0.3s; }
        button:hover { background-color: var(--vermelho-escuro); }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th, td { padding: 15px; border-bottom: 1px solid #ddd; text-align: left; }
        th { background-color: #333; color: white; font-weight: 600; text-transform: uppercase; font-size: 13px; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        tr:hover { background-color: #f1f1f1; }
        .estoque-baixo { color: #dc3545; font-weight: bold; }
        .abas { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:24px; }
        .abas button[aria-pressed="false"] { background:#666; }
        .filtros { display:flex; flex-wrap:wrap; gap:12px; align-items:end; }
        .filtros label { display:flex; flex-direction:column; gap:6px; font-size:14px; }
        .tabela-responsiva { overflow-x:auto; }
        .tipo-entrada { color:#176b39; font-weight:bold; }
        .tipo-saida { color:#b32025; font-weight:bold; }
        .paginacao { display:flex; gap:12px; align-items:center; flex-wrap:wrap; margin-top:15px; }
        button:disabled { opacity:.5; cursor:not-allowed; }
        .erro { color:#b32025; }
        [hidden] { display:none !important; }
        @media(max-width:700px) {
            .container { margin:12px; padding:15px; }
            .dashboard { flex-direction:column; }
            .header h1 { font-size:1.7em; }
            .painel-acoes input, .painel-acoes select { max-width:100%; box-sizing:border-box; margin:4px 0; }
            .filtros label, .filtros input, .filtros select { width:100%; box-sizing:border-box; }
        }
    </style>
</head>
<body>

<div class="header">
    <div class="logo-t">T</div>
    <div class="header-text">
        <h1>TRUCKVAN</h1>
        <p>Sistema de Controle de Estoque</p>
    </div>
</div>

<div class="container">
    <nav class="abas" aria-label="Seções do sistema">
        <button id="aba-estoque" aria-pressed="true" onclick="abrirAba('estoque')">Estoque</button>
        <button id="aba-relatorio" aria-pressed="false" onclick="abrirAba('relatorio')">Relatório de Movimentações</button>
    </nav>
    <section id="painel-estoque" aria-label="Estoque">
    <div class="dashboard">
        <div class="card">
            <h3>Total de Itens</h3>
            <p id="total-brindes">-</p>
        </div>
        <div class="card">
            <h3>Patrimônio Armazenado</h3>
            <p id="valor-total">-</p>
        </div>
    </div>

    <div class="painel-acoes">
        <h3>Gerenciar Estoque</h3>
        <div style="margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #ddd;">
            <strong>Novo Brinde:</strong>
            <input type="text" id="novo-codigo" placeholder="Código (Ex: CAN-01)">
            <input type="text" id="novo-nome" placeholder="Nome do Brinde">
            <input type="number" id="novo-qtd" placeholder="Qtd Inicial" style="width: 100px;">
            <input type="number" id="novo-valor" placeholder="Valor Un (R$)" style="width: 110px;" step="0.01">
            <button onclick="cadastrarItem()">Cadastrar Produto</button>
        </div>
        <div>
            <strong>Movimentar:</strong>
            <select id="mov-id-item">
                <option value="">Selecione o Brinde...</option>
            </select>
            <select id="mov-tipo">
                <option value="ENTRADA">Entrada (Adicionar)</option>
                <option value="SAIDA">Saída (Retirar)</option>
            </select>
            <input type="number" id="mov-qtd" placeholder="Quantidade" style="width: 120px;">
            <input type="text" id="mov-observacao" maxlength="500" placeholder="Observação (opcional)" aria-label="Observação da movimentação">
            <button onclick="registrarMovimentacao()">Confirmar Movimentação</button>
        </div>
    </div>

    <h2>Inventário Atual</h2>
    <div class="tabela-responsiva"><table>
        <thead>
        <tr>
            <th>Código</th>
            <th>Nome do Brinde</th>
            <th>Em Estoque</th>
            <th>Valor Unitário</th>
            <th>Ações</th>
        </tr>
        </thead>
        <tbody id="tabela-itens">
        <tr><td colspan="5" style="text-align:center;">Verificando credenciais...</td></tr>
        </tbody>
    </table></div>
    </section>

    <section id="painel-relatorio" aria-labelledby="titulo-relatorio" hidden>
        <h2 id="titulo-relatorio">Relatório de Movimentações</h2>
        <p>Novos registros usam o horário de Brasília. O responsável é identificado pelo login.</p>
        <form id="form-filtros" class="painel-acoes filtros">
            <label>Data inicial<input type="date" id="filtro-inicio"></label>
            <label>Data final<input type="date" id="filtro-fim"></label>
            <label>Responsável<select id="filtro-responsavel"><option value="">Todos</option></select></label>
            <label>Tipo<select id="filtro-tipo"><option value="">Todos</option><option value="ENTRADA">ENTRADA</option><option value="SAIDA">SAÍDA</option></select></label>
            <label>Produto/item<select id="filtro-item"><option value="">Todos</option></select></label>
            <button type="submit">Filtrar</button>
            <button type="button" onclick="limparFiltros()">Limpar filtros</button>
        </form>
        <p id="status-relatorio" role="status" aria-live="polite"></p>
        <p style="font-size:13px; color:#666;">Role a tabela para o lado para consultar todas as colunas.</p>
        <div class="tabela-responsiva">
            <table aria-label="Movimentações de estoque">
                <thead><tr><th>ID</th><th>Data</th><th>Hora</th><th>Tipo</th><th>Produto/item</th><th>Quantidade</th><th>Responsável / login</th><th>Observação</th><th>Estoque anterior</th><th>Estoque posterior</th></tr></thead>
                <tbody id="tabela-movimentacoes"></tbody>
            </table>
        </div>
        <div class="paginacao">
            <button id="pagina-anterior" onclick="consultarRelatorio(paginaRelatorio - 1)" disabled>Anterior</button>
            <span id="pagina-info"></span>
            <button id="pagina-proxima" onclick="consultarRelatorio(paginaRelatorio + 1)" disabled>Próxima</button>
        </div>
        <p>Registros antigos podem não conter responsável ou saldos. Esses valores não são estimados.</p>
        <button disabled title="Evolução futura: PDF e Excel/CSV">Exportar relatório (em breve)</button>
    </section>

    <div style="text-align: center; margin-top: 40px; padding: 20px; color: #666;">
        <hr style="border: 0; border-top: 1px solid #ddd; margin-bottom: 20px;">
        <p style="font-size: 14px;">Controle de Estoque - Truckvan</p>
        <div id="area-autenticacao"></div>
    </div>
</div>


<script>
    let paginaRelatorio = 0;
    let filtrosAplicados = new URLSearchParams();
    let consultaAtual = 0;
    let operacaoEmAndamento = false;
    const el = id => document.getElementById(id);
    const moeda = valor => Number(valor).toLocaleString('pt-BR', {style:'currency', currency:'BRL'});

    function obterToken() { return localStorage.getItem('meu_token_jwt') || localStorage.getItem('token'); }

    async function api(url, opcoes = {}) {
        const token = obterToken();
        if (!token) throw new Error('Faça login para acessar o sistema.');
        const resposta = await fetch(url, {...opcoes, headers: {
            'Content-Type':'application/json', 'Authorization':'Bearer ' + token, ...opcoes.headers
        }});
        if (!resposta.ok) {
            if (resposta.status === 401 || resposta.status === 403) {
                localStorage.removeItem('token');
                localStorage.removeItem('meu_token_jwt');
                atualizarRodape();
                throw new Error('Acesso negado ou sessão expirada. Faça login novamente.');
            }
            const texto = await resposta.text();
            let mensagem = texto;
            try {
                const erro = JSON.parse(texto);
                mensagem = erro.mensagem || erro.message || texto;
                if (erro.erros) mensagem += ' ' + erro.erros.join('; ');
            } catch (_) { /* A API anterior também devolvia texto simples. */ }
            throw new Error(mensagem || 'Não foi possível concluir a operação.');
        }
        return resposta.status === 204 ? null : resposta.json();
    }

    function celula(linha, texto, classe) {
        const td = document.createElement('td');
        td.textContent = texto ?? 'Não registrado';
        if (classe) td.className = classe;
        linha.appendChild(td);
        return td;
    }
    function mensagemTabela(id, texto, colunas) {
        const corpo = el(id); corpo.replaceChildren();
        const linha = document.createElement('tr');
        celula(linha, texto).colSpan = colunas;
        corpo.appendChild(linha);
    }
    function preencherSelect(id, dados, rotulo, primeiro) {
        const select = el(id), selecionado = select.value;
        select.replaceChildren(new Option(primeiro, ''));
        dados.forEach(dado => select.add(new Option(rotulo(dado), dado.id)));
        select.value = selecionado;
        if (select.selectedIndex < 0) select.selectedIndex = 0;
    }
    function botao(texto, acao) {
        const b = document.createElement('button');
        b.textContent = texto; b.addEventListener('click', acao); return b;
    }

    async function carregarDados() {
        try {
            const [itens, resumo] = await Promise.all([api('/itens'), api('/itens/relatorio')]);
            el('total-brindes').textContent = resumo.totalDeBrindesCadastrados;
            el('valor-total').textContent = moeda(resumo.valorTotalArmazenado);
            preencherSelect('mov-id-item', itens, i => `${i.codigo} - ${i.nome} (Estoque: ${i.quantidade})`, 'Selecione o Brinde...');
            const corpo = el('tabela-itens'); corpo.replaceChildren();
            itens.forEach(item => {
                const linha = document.createElement('tr');
                celula(linha, item.codigo); celula(linha, item.nome);
                celula(linha, item.quantidade + ' un.', item.quantidade < 20 ? 'estoque-baixo' : '');
                celula(linha, moeda(item.valor));
                const acoes = celula(linha, '');
                acoes.append(botao('Editar', () => editarValor(item.id, item.valor)), document.createTextNode(' '),
                    botao('Remover', () => removerItem(item.id)));
                corpo.appendChild(linha);
            });
            if (!itens.length) mensagemTabela('tabela-itens', 'Nenhum brinde cadastrado.', 5);
        } catch (erro) {
            mensagemTabela('tabela-itens', erro.message, 5);
            el('total-brindes').textContent = '—'; el('valor-total').textContent = '—';
        }
    }
    async function executarOperacao(acao) {
        if (operacaoEmAndamento) return;
        operacaoEmAndamento = true;
        try { await acao(); await carregarDados(); }
        catch (erro) { alert(erro.message); }
        finally { operacaoEmAndamento = false; }
    }
    function cadastrarItem() {
        const dados = {codigo:el('novo-codigo').value.trim(), nome:el('novo-nome').value.trim(),
            quantidade:Number(el('novo-qtd').value), valor:Number(el('novo-valor').value)};
        if (!dados.codigo || !dados.nome || el('novo-qtd').value === '' || el('novo-valor').value === ''
            || !Number.isInteger(dados.quantidade) || dados.quantidade < 0 || !Number.isFinite(dados.valor) || dados.valor < 0) {
            alert('Preencha código, nome, quantidade inteira não negativa e valor não negativo.'); return;
        }
        executarOperacao(async () => {
            await api('/itens', {method:'POST', body:JSON.stringify(dados)});
            ['novo-codigo','novo-nome','novo-qtd','novo-valor'].forEach(id => el(id).value = '');
            alert('Produto cadastrado. O saldo inicial positivo foi registrado como ENTRADA.');
        });
    }
    function registrarMovimentacao() {
        const itemId = Number(el('mov-id-item').value), quantidade = Number(el('mov-qtd').value);
        if (!itemId || !Number.isInteger(quantidade) || quantidade <= 0) {
            alert('Selecione o item e informe uma quantidade inteira positiva.'); return;
        }
        executarOperacao(async () => {
            await api('/movimentacoes', {method:'POST', body:JSON.stringify({item:{id:itemId},
                tipo:el('mov-tipo').value, quantidade, observacao:el('mov-observacao').value.trim() || null})});
            el('mov-qtd').value = ''; el('mov-observacao').value = '';
            alert('Movimentação registrada com seu usuário autenticado.');
        });
    }
    function removerItem(id) {
        if (confirm('Excluir este item? Itens com histórico são preservados e não podem ser excluídos.')) {
            executarOperacao(() => api(`/itens/${id}`, {method:'DELETE'}));
        }
    }
    function editarValor(id, atual) {
        const texto = prompt('Novo valor unitário:', atual);
        if (texto === null) return;
        const valor = Number(texto.trim().replace(',', '.'));
        if (!texto.trim() || !Number.isFinite(valor) || valor < 0) { alert('Valor inválido.'); return; }
        executarOperacao(() => api(`/itens/${id}`, {method:'PUT', body:JSON.stringify({valor})}));
    }
    function atualizarRodape() {
        el('area-autenticacao').replaceChildren(obterToken()
            ? botao('Desconectar (Sair)', fazerLogout)
            : botao('Acesso Gerencial (Login)', () => window.location.href = '/login.html'));
    }
    function fazerLogout() {
        localStorage.removeItem('token'); localStorage.removeItem('meu_token_jwt');
        window.location.href = '/login.html';
    }

    async function abrirAba(aba) {
        const relatorio = aba === 'relatorio';
        el('painel-estoque').hidden = relatorio; el('painel-relatorio').hidden = !relatorio;
        el('aba-estoque').setAttribute('aria-pressed', String(!relatorio));
        el('aba-relatorio').setAttribute('aria-pressed', String(relatorio));
        if (!relatorio) { carregarDados(); return; }
        try {
            const [itens, responsaveis] = await Promise.all([api('/itens'), api('/movimentacoes/responsaveis')]);
            preencherSelect('filtro-item', itens, i => `${i.codigo} - ${i.nome}`, 'Todos');
            preencherSelect('filtro-responsavel', responsaveis, u => u.login, 'Todos');
            aplicarFiltros();
        } catch (erro) {
            ++consultaAtual;
            mensagemTabela('tabela-movimentacoes', erro.message, 10);
            el('status-relatorio').textContent = erro.message;
            el('pagina-anterior').disabled = true; el('pagina-proxima').disabled = true;
        }
    }
    function aplicarFiltros() {
        const inicio = el('filtro-inicio').value, fim = el('filtro-fim').value;
        if (inicio && fim && inicio > fim) {
            el('status-relatorio').textContent = 'A data inicial não pode ser posterior à final.'; return;
        }
        filtrosAplicados = new URLSearchParams();
        const campos = {dataInicial:inicio, dataFinal:fim, responsavelId:el('filtro-responsavel').value,
            tipo:el('filtro-tipo').value, itemId:el('filtro-item').value};
        Object.entries(campos).forEach(([chave, valor]) => { if (valor) filtrosAplicados.set(chave, valor); });
        consultarRelatorio(0);
    }
    function limparFiltros() { el('form-filtros').reset(); aplicarFiltros(); }
    async function consultarRelatorio(pagina) {
        const consulta = ++consultaAtual;
        el('status-relatorio').textContent = 'Carregando movimentações...';
        el('pagina-anterior').disabled = true; el('pagina-proxima').disabled = true;
        mensagemTabela('tabela-movimentacoes', 'Carregando...', 10);
        const parametros = new URLSearchParams(filtrosAplicados);
        parametros.set('pagina', pagina); parametros.set('tamanho', '20');
        try {
            const dados = await api('/movimentacoes/relatorio?' + parametros);
            if (consulta !== consultaAtual) return; // Impede uma resposta antiga de sobrescrever filtros novos.
            paginaRelatorio = dados.pagina;
            const corpo = el('tabela-movimentacoes'); corpo.replaceChildren();
            dados.conteudo.forEach(m => {
                const linha = document.createElement('tr');
                // LocalDateTime já representa horário de São Paulo; não converter pelo fuso do navegador.
                const partes = m.dataMovimentacao ? m.dataMovimentacao.split('T') : [];
                celula(linha, m.id);
                celula(linha, partes[0] ? partes[0].split('-').reverse().join('/') : null);
                celula(linha, partes[1] ? partes[1].substring(0,8) : null);
                celula(linha, m.tipo === 'SAIDA' ? 'SAÍDA' : m.tipo, m.tipo === 'ENTRADA' ? 'tipo-entrada' : 'tipo-saida');
                celula(linha, `${m.item.codigo} - ${m.item.nome}`);
                celula(linha, m.quantidade);
                celula(linha, m.responsavel ? m.responsavel.login : 'Responsável não registrado');
                celula(linha, m.observacao || '—'); celula(linha, m.estoqueAnterior); celula(linha, m.estoquePosterior);
                corpo.appendChild(linha);
            });
            if (!dados.conteudo.length) mensagemTabela('tabela-movimentacoes', 'Nenhuma movimentação encontrada.', 10);
            el('status-relatorio').textContent = `${dados.totalElementos} movimentação(ões) encontrada(s).`;
            el('pagina-info').textContent = dados.totalPaginas ? `Página ${dados.pagina + 1} de ${dados.totalPaginas}` : 'Sem resultados';
            el('pagina-anterior').disabled = dados.pagina <= 0;
            el('pagina-proxima').disabled = dados.pagina + 1 >= dados.totalPaginas;
        } catch (erro) {
            if (consulta !== consultaAtual) return;
            el('status-relatorio').textContent = erro.message;
            el('pagina-info').textContent = '';
            mensagemTabela('tabela-movimentacoes', erro.message, 10);
        }
    }
    el('form-filtros').addEventListener('submit', evento => { evento.preventDefault(); aplicarFiltros(); });
    window.onload = () => { atualizarRodape(); carregarDados(); };
</script>
</body>
</html>
```

### `src/main/resources/static/login.html`

**Por que mudar:** Remove a chave antiga token quando um novo login é concluído.

**Como se conecta:** Evita usar token antigo no lugar do recém-retornado pela API. A tela principal prioriza meu_token_jwt.

**Como testar:** Faça novo login com outra conta e confirme o responsável das novas movimentações.

**Código completo:**

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Truckvan</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f9; margin: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; }
        .header-logo { background-color: #b32025; color: white; font-size: 40px; font-weight: 900; width: 80px; height: 80px; display: flex; align-items: center; justify-content: center; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); font-family: 'Arial Black', sans-serif; margin-bottom: 20px; }
        .login-box { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); text-align: center; width: 100%; max-width: 350px; border-top: 5px solid #b32025; }
        .login-box h2 { margin-top: 0; color: #333; }
        input { width: 90%; padding: 12px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
        button { width: 98%; background-color: #b32025; color: white; border: none; padding: 12px; font-size: 16px; font-weight: bold; border-radius: 4px; cursor: pointer; transition: 0.3s; margin-top: 15px; }
        button:hover { background-color: #8a171c; }
        #msg-login { color: red; font-size: 14px; margin-top: 15px; display: none; font-weight: bold; }
    </style>
</head>
<body>

<div class="header-logo">T</div>

<div class="login-box">
    <h2>Acesso Restrito</h2>
    <p style="color: #666; font-size: 14px; margin-bottom: 25px;">Faça login para gerenciar o estoque.</p>

    <input type="text" id="loginUser" placeholder="Usuário">
    <input type="password" id="loginSenha" placeholder="Senha">

    <button onclick="fazerLogin()">Entrar no Sistema</button>

    <p id="msg-login"> Login ou senha inválidos!</p>
    <br>
    <a href="/" style="color: #666; font-size: 12px; text-decoration: none;">&larr; Voltar para o Início</a>
</div>

<script>
    async function fazerLogin() {
        const loginDigitado = document.getElementById('loginUser').value;
        const senhaDigitada = document.getElementById('loginSenha').value;

        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ login: loginDigitado, senha: senhaDigitada })
            });

            if (response.ok) {
                const dados = await response.json();
                localStorage.removeItem('token');
                localStorage.setItem('meu_token_jwt', dados.token);
                // Mágica: Se logar certo, manda o usuário de volta pra tela principal!
                window.location.href = '/';
            } else {
                document.getElementById('msg-login').style.display = 'block';
            }
        } catch (error) {
            console.error("Erro na comunicação de login:", error);
        }
    }
</script>
</body>
</html>
```

## ETAPA 12 — Testes e exemplos reproduzíveis

### `src/test/resources/application-test.properties`

**Por que mudar:** Usa H2 em memória, esquema descartável e segredo exclusivo de teste. Desativa open-in-view para revelar leitura LAZY fora da transação.

**Como se conecta:** Somente classes com perfil test utilizam esta configuração. Não usa o MySQL de produção.

**Como testar:** Execute a suíte sem MYSQL_PASSWORD e sem JWT_SECRET de produção.

**Código completo:**

```properties
spring.datasource.url=jdbc:h2:mem:relatorio;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
api.security.token.secret=segredo-exclusivo-dos-testes-de-integracao-123456
```

### `src/test/java/api_brindes/ApiBrindesApplicationTests.java`

**Por que mudar:** Mantém o teste original de inicialização e ativa o perfil isolado.

**Como se conecta:** Verifica que o contexto Spring sobe com o esquema de teste.

**Como testar:** Execute ./mvnw test.

**Código completo:**

```java
package api_brindes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class ApiBrindesApplicationTests {

	@Test
	void contextLoads() {
	}

}
```

### `src/test/java/api_brindes/RelatorioMovimentacoesTests.java`

**Por que mudar:** 15 testes de integração usando Spring, JPA, MockMvc, autenticação JWT real e banco H2.

**Como se conecta:** As requisições percorrem filtro, controller, serviço e banco; dois usuários de teste são criados a cada cenário. SQL direto aparece somente em fixtures para simular datas antigas.

**Como testar:** Execute ./mvnw test. Não exige uma conta já cadastrada. O teste concorrente também verifica saldo e número de registros.

**Código completo:**

```java
package api_brindes;

import api_brindes.model.*;
import api_brindes.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioMovimentacoesTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ItemRepository itens;
    @Autowired UsuarioRepository usuarios;
    @Autowired MovimentacaoRepository movimentos;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    Item caneca;
    Usuario joao, maria;
    String tokenJoao, tokenMaria;

    @BeforeEach
    void preparar() throws Exception {
        movimentos.deleteAll(); itens.deleteAll(); usuarios.deleteAll();
        joao = usuarios.save(new Usuario("joao", encoder.encode("senha-teste")));
        maria = usuarios.save(new Usuario("maria", encoder.encode("senha-teste")));
        caneca = itens.save(new Item("CAN-01", "Caneca", 100, 25.0));
        tokenJoao = login("joao"); tokenMaria = login("maria");
    }
    String login(String usuario) throws Exception {
        var resultado = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + usuario + "\",\"senha\":\"senha-teste\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }
    String corpo(String tipo, int quantidade) {
        return "{\"item\":{\"id\":" + caneca.getId() + "},\"tipo\":\"" + tipo
                + "\",\"quantidade\":" + quantidade + ",\"observacao\":\"Evento comercial\"}";
    }
    ResultActions registrar(String token, String tipo, int quantidade) throws Exception {
        return mvc.perform(post("/movimentacoes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(tipo, quantidade)));
    }
    JsonNode relatorio(String filtros) throws Exception {
        var resposta = mvc.perform(get("/movimentacoes/relatorio" + filtros)
                .header("Authorization", "Bearer " + tokenJoao)).andExpect(status().isOk()).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString());
    }

    @Test void entradaRegistraAutorSaldosHorarioEObservacao() throws Exception {
        var antes = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusSeconds(1);
        var resposta = registrar(tokenJoao, "ENTRADA", 50).andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavel.id").value(joao.getId()))
                .andExpect(jsonPath("$.responsavel.login").value("joao"))
                .andExpect(jsonPath("$.estoqueAnterior").value(100))
                .andExpect(jsonPath("$.estoquePosterior").value(150))
                .andExpect(jsonPath("$.observacao").value("Evento comercial"))
                .andExpect(jsonPath("$.responsavel.senha").doesNotExist())
                .andReturn();
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(150);
        var data = LocalDateTime.parse(json.readTree(resposta.getResponse().getContentAsString()).get("dataMovimentacao").asText());
        assertThat(data).isBetween(antes, LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusSeconds(1));
    }
    @Test void saidaRegistraOutroAutorESaldos() throws Exception {
        registrar(tokenMaria, "SAIDA", 20).andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavel.id").value(maria.getId()))
                .andExpect(jsonPath("$.estoqueAnterior").value(100))
                .andExpect(jsonPath("$.estoquePosterior").value(80));
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(80);
    }
    @Test void rejeitaResponsavelIdForjadoSemAlterarEstoque() throws Exception {
        for (String campo : List.of("\"responsavelId\":" + maria.getId(),
                "\"responsavel\":{\"id\":" + maria.getId() + "}", "\"id\":12",
                "\"estoqueAnterior\":900", "\"dataMovimentacao\":\"2020-01-01T00:00:00\"")) {
            String dados = corpo("ENTRADA", 10);
            mvc.perform(post("/movimentacoes").header("Authorization", "Bearer " + tokenJoao)
                    .contentType(MediaType.APPLICATION_JSON).content(dados.substring(0, dados.length()-1) + "," + campo + "}"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(movimentos.count()).isZero();
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(100);
    }
    @Test void semTokenOuTokenInvalidoNaoMovimentaNemConsulta() throws Exception {
        mvc.perform(post("/movimentacoes").contentType(MediaType.APPLICATION_JSON).content(corpo("ENTRADA", 10)))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/movimentacoes/relatorio").header("Authorization", "Bearer invalido"))
                .andExpect(status().is4xxClientError());
        assertThat(movimentos.count()).isZero();
    }
    @Test void saldoInsuficienteNaoGravaParcialmente() throws Exception {
        registrar(tokenJoao, "SAIDA", 101).andExpect(status().isBadRequest());
        assertThat(movimentos.count()).isZero();
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(100);
    }
    @Test void validaQuantidadeTipoItemEObservacao() throws Exception {
        registrar(tokenJoao, "ENTRADA", 0).andExpect(status().isBadRequest());
        registrar(tokenJoao, "ENTRADA", -1).andExpect(status().isBadRequest());
        registrar(tokenJoao, "INVALIDO", 1).andExpect(status().isBadRequest());
        for (String dados : List.of("{}", "{\"item\":null,\"tipo\":\"ENTRADA\",\"quantidade\":1}",
                corpo("ENTRADA", 1).replace("Evento comercial", "a".repeat(501)),
                corpo("ENTRADA", 1).replace("\"quantidade\":1", "\"quantidade\":1.5"))) {
            mvc.perform(post("/movimentacoes").header("Authorization", "Bearer " + tokenJoao)
                    .contentType(MediaType.APPLICATION_JSON).content(dados)).andExpect(status().isBadRequest());
        }
        assertThat(movimentos.count()).isZero();
    }
    @Test void filtraResponsavelTipoProdutoECombinacao() throws Exception {
        registrar(tokenJoao, "ENTRADA", 10).andExpect(status().isCreated());
        registrar(tokenMaria, "SAIDA", 5).andExpect(status().isCreated());
        registrar(tokenJoao, "SAIDA", 3).andExpect(status().isCreated());
        assertThat(relatorio("?responsavelId=" + joao.getId()).get("totalElementos").asInt()).isEqualTo(2);
        assertThat(relatorio("?tipo=SAIDA").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(relatorio("?tipo=ENTRADA").get("totalElementos").asInt()).isEqualTo(1);
        assertThat(relatorio("?itemId=" + caneca.getId()).get("totalElementos").asInt()).isEqualTo(3);
        assertThat(relatorio("?responsavelId=" + maria.getId() + "&tipo=SAIDA&itemId=" + caneca.getId())
                .get("conteudo").get(0).get("responsavel").get("login").asText()).isEqualTo("maria");
        assertThat(relatorio("?responsavelId=" + maria.getId() + "&tipo=ENTRADA").get("totalElementos").asInt()).isZero();
        var outro = itens.save(new Item("OUT-01", "Outro", 0, 1.0));
        assertThat(relatorio("?itemId=" + outro.getId()).get("totalElementos").asInt()).isZero();
    }
    @Test void periodoIncluiDiaFinalInteiroEExcluiDiaSeguinte() throws Exception {
        for (String horario : List.of("2026-09-30 23:59:59", "2026-10-01 00:00:00",
                "2026-10-31 23:59:59", "2026-11-01 00:00:00")) {
            var res = registrar(tokenJoao, "ENTRADA", 1).andExpect(status().isCreated()).andReturn();
            int id = json.readTree(res.getResponse().getContentAsString()).get("id").asInt();
            jdbc.update("update movimentacao set data_movimentacao = ? where id = ?", horario, id);
        }
        assertThat(relatorio("?dataInicial=2026-10-01&dataFinal=2026-10-31").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(relatorio("?dataInicial=2026-10-31").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(relatorio("?dataFinal=2026-10-01").get("totalElementos").asInt()).isEqualTo(2);
    }
    @Test void rejeitaPeriodoInvertidoEPaginacaoInvalida() throws Exception {
        for (String filtros : List.of("?dataInicial=2026-11-01&dataFinal=2026-10-01", "?pagina=-1", "?tamanho=101", "?tipo=OUTRO")) {
            mvc.perform(get("/movimentacoes/relatorio" + filtros).header("Authorization", "Bearer " + tokenJoao))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void paginaOrdenaSemPerderRegistros() throws Exception {
        registrar(tokenJoao, "ENTRADA", 1).andExpect(status().isCreated());
        registrar(tokenJoao, "ENTRADA", 2).andExpect(status().isCreated());
        var primeira = relatorio("?tamanho=1&pagina=0");
        var segunda = relatorio("?tamanho=1&pagina=1");
        assertThat(primeira.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(primeira.get("conteudo").get(0).get("id").asInt()).isGreaterThan(segunda.get("conteudo").get(0).get("id").asInt());
    }
    @Test void exclusaoPreservaHistoricoESaldosAntigos() throws Exception {
        registrar(tokenJoao, "SAIDA", 20).andExpect(status().isCreated());
        registrar(tokenMaria, "ENTRADA", 50).andExpect(status().isCreated());
        mvc.perform(delete("/itens/" + caneca.getId()).header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isConflict());
        assertThat(movimentos.count()).isEqualTo(2);
        var antiga = relatorio("").get("conteudo").get(1);
        assertThat(antiga.get("estoqueAnterior").asInt()).isEqualTo(100);
        assertThat(antiga.get("estoquePosterior").asInt()).isEqualTo(80);
    }
    @Test void cadastroInicialGeraEntradaAuditadaERejeitaId() throws Exception {
        mvc.perform(post("/itens").header("Authorization", "Bearer " + tokenJoao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"NOVO\",\"nome\":\"Novo item\",\"quantidade\":12,\"valor\":2.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(12));
        var entrada = relatorio("").get("conteudo").get(0);
        assertThat(entrada.get("estoqueAnterior").asInt()).isZero();
        assertThat(entrada.get("estoquePosterior").asInt()).isEqualTo(12);
        assertThat(entrada.get("responsavel").get("id").asLong()).isEqualTo(joao.getId());
        mvc.perform(post("/itens").header("Authorization", "Bearer " + tokenJoao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + caneca.getId() + ",\"codigo\":\"CAN-01\",\"nome\":\"Caneca\",\"quantidade\":999,\"valor\":2}"))
                .andExpect(status().isBadRequest());
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(100);
    }
    @Test void legadoContinuaVisivelSemInventarAutorOuSaldos() throws Exception {
        jdbc.update("insert into movimentacao (id_item,tipo,quantidade,data_movimentacao) values (?, 'entrada', 10, '2026-01-01 12:00:00')", caneca.getId());
        var dados = relatorio("?tipo=ENTRADA");
        assertThat(dados.get("totalElementos").asInt()).isEqualTo(1);
        var legado = dados.get("conteudo").get(0);
        assertThat(legado.get("responsavel").isNull()).isTrue();
        assertThat(legado.get("estoqueAnterior").isNull()).isTrue();
    }
    @Test void respostaNaoExpoeSenhaEListaSomenteResponsaveisComHistorico() throws Exception {
        registrar(tokenJoao, "ENTRADA", 1).andExpect(status().isCreated());
        mvc.perform(get("/movimentacoes/responsaveis").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].login").value("joao"))
                .andExpect(jsonPath("$[0].senha").doesNotExist()).andExpect(jsonPath("$[0].password").doesNotExist());
        var texto = relatorio("").toString();
        assertThat(texto).doesNotContain("senha", "password", "$2a$");
    }
    @Test void duasSaidasConcorrentesNaoUsamOMesmoSaldo() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var largada = new CountDownLatch(1);
        try {
            Callable<Integer> saida = () -> {
                largada.await();
                return registrar(tokenJoao, "SAIDA", 70).andReturn().getResponse().getStatus();
            };
            var a = pool.submit(saida); var b = pool.submit(saida); largada.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
            assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(30);
            assertThat(movimentos.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
```

### `tests/ui-relatorio.cjs`

**Por que mudar:** Verifica navegação, filtros, vazio, limpeza, datas inválidas, legado, XSS e largura móvel num navegador headless.

**Como se conecta:** A API é simulada; este teste não substitui a integração backend nem homologação ponta a ponta com MySQL.

**Como testar:** Opcional: npm install --no-save playwright; npx playwright install chromium; node tests/ui-relatorio.cjs. PLAYWRIGHT_CHROMIUM_EXECUTABLE permite indicar um Chromium já instalado.

**Código completo:**

```javascript
// Teste da interface com API simulada; não acessa nem modifica o banco real.
// Dependência local opcional: npm install --no-save playwright
// Execução: node tests/ui-relatorio.cjs
const { chromium } = require('playwright');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
    const browser = await chromium.launch({headless:true, ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE ? {executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE} : {})});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', e => errors.push(e.message));
        const html = readFileSync(path.join(__dirname, '../src/main/resources/static/index.html'), 'utf8');
        const item = {id:1, codigo:'CAN-01', nome:'<img src=x onerror="window.xss=true">', quantidade:80, valor:25};
        const movimentos = [
            {id:2, dataMovimentacao:'2026-10-05T09:15:30', tipo:'SAIDA', item, quantidade:20,
                responsavel:{id:2,login:'maria'}, observacao:'Evento', estoqueAnterior:100,estoquePosterior:80},
            {id:1, dataMovimentacao:'2026-10-05T08:30:00', tipo:'ENTRADA', item, quantidade:100,
                responsavel:null, observacao:null, estoqueAnterior:null,estoquePosterior:null}
        ];
        await page.route('http://estoque.test/**', async route => {
            const url = new URL(route.request().url());
            let data;
            if (url.pathname === '/') return route.fulfill({contentType:'text/html',body:html});
            if (url.pathname === '/itens') data=[item];
            else if (url.pathname === '/itens/relatorio') data={totalDeBrindesCadastrados:80,valorTotalArmazenado:2000};
            else if (url.pathname === '/movimentacoes/responsaveis') data=[{id:2,login:'maria'}];
            else if (url.pathname === '/movimentacoes/relatorio') {
                let linhas=movimentos.filter(m => !url.searchParams.get('tipo') || m.tipo === url.searchParams.get('tipo'));
                if(url.searchParams.get('responsavelId')) linhas=linhas.filter(m => String(m.responsavel?.id) === url.searchParams.get('responsavelId'));
                if(url.searchParams.get('dataInicial') === '2030-01-01') linhas=[];
                data={conteudo:linhas,pagina:0,tamanho:20,totalElementos:linhas.length,totalPaginas:linhas.length?1:0};
            } else return route.fulfill({status:404,body:''});
            return route.fulfill({contentType:'application/json',body:JSON.stringify(data)});
        });
        await page.addInitScript(() => localStorage.setItem('meu_token_jwt','token-simulado'));
        await page.goto('http://estoque.test/');
        await page.waitForFunction(() => document.querySelector('#total-brindes').textContent === '80');
        assert.equal(await page.locator('#tabela-itens img').count(),0);
        await page.getByRole('button',{name:'Relatório de Movimentações',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        assert.match(await page.locator('#tabela-movimentacoes').innerText(), /Responsável não registrado/);
        assert.equal(await page.locator('#tabela-movimentacoes img').count(),0);
        await page.locator('#filtro-tipo').selectOption('SAIDA');
        await page.locator('#filtro-responsavel').selectOption('2');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('1 movimentação'));
        assert.equal(await page.locator('#tabela-movimentacoes tr').count(),1);
        await page.locator('#filtro-inicio').fill('2030-01-01');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.getByText('Nenhuma movimentação encontrada.',{exact:true}).waitFor();
        await page.getByRole('button',{name:'Limpar filtros',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        assert.equal(await page.locator('#filtro-tipo').inputValue(),'');
        await page.locator('#filtro-inicio').fill('2026-11-01');
        await page.locator('#filtro-fim').fill('2026-10-01');
        await page.getByRole('button',{name:'Filtrar',exact:true}).click();
        await page.getByText('A data inicial não pode ser posterior à final.',{exact:true}).waitFor();
        await page.getByRole('button',{name:'Limpar filtros',exact:true}).click();
        await page.waitForFunction(() => document.querySelector('#status-relatorio').textContent.includes('2 movimentação'));
        await page.screenshot({path:process.env.RELATORIO_SCREENSHOT || '/tmp/relatorio-desktop.png',fullPage:true});
        await page.setViewportSize({width:390,height:844});
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),true);
        await page.screenshot({path:'/tmp/relatorio-mobile.png',fullPage:true});
        assert.equal(await page.evaluate(() => window.xss === true),false);
        assert.deepEqual(errors,[]);
        console.log('PASS: abas, filtros, vazio, limpar, datas inválidas, legado, texto seguro e largura móvel.');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
```

### `testes.http`

**Por que mudar:** Exemplos de login, cadastro, entrada, saída, consulta, filtros e tentativas proibidas.

**Como se conecta:** Pode ser aberto no IntelliJ HTTP Client ou na extensão REST Client do VS Code.

**Como testar:** Ajuste baseUrl, token, itemId e responsavelId; use contas e dados locais, não produção.

**Código completo:**

```http
@baseUrl = http://localhost:8080
@token = COLE_O_TOKEN_RETORNADO_PELO_LOGIN
@itemId = 1
@responsavelId = 1

### Login: use uma conta já existente no seu ambiente local
POST {{baseUrl}}/login
Content-Type: application/json

{"login":"seu-login","senha":"sua-senha"}

### Cadastro de item: saldo positivo gera ENTRADA com responsável
POST {{baseUrl}}/itens
Authorization: Bearer {{token}}
Content-Type: application/json

{"codigo":"CAN-REL-01","nome":"Caneca relatório","quantidade":100,"valor":25.50}

### ENTRADA — responsável é extraído do token no backend
POST {{baseUrl}}/movimentacoes
Authorization: Bearer {{token}}
Content-Type: application/json

{"item":{"id":{{itemId}}},"tipo":"ENTRADA","quantidade":50,"observacao":"Reposição para evento"}

### SAÍDA
POST {{baseUrl}}/movimentacoes
Authorization: Bearer {{token}}
Content-Type: application/json

{"item":{"id":{{itemId}}},"tipo":"SAIDA","quantidade":20,"observacao":"Entrega no evento"}

### Todos os registros, paginados
GET {{baseUrl}}/movimentacoes/relatorio?pagina=0&tamanho=20
Authorization: Bearer {{token}}

### Combinação de todos os filtros
GET {{baseUrl}}/movimentacoes/relatorio?dataInicial=2026-10-01&dataFinal=2026-10-31&responsavelId={{responsavelId}}&tipo=SAIDA&itemId={{itemId}}
Authorization: Bearer {{token}}

### Opções do filtro de responsável (somente ID e login)
GET {{baseUrl}}/movimentacoes/responsaveis
Authorization: Bearer {{token}}

### Tentativa de falsificar autoria: deve retornar 400 e não alterar saldo
POST {{baseUrl}}/movimentacoes
Authorization: Bearer {{token}}
Content-Type: application/json

{"item":{"id":{{itemId}}},"tipo":"ENTRADA","quantidade":10,"responsavelId":999}

### Item com histórico: deve retornar 409 e preservar item e movimentos
DELETE {{baseUrl}}/itens/{{itemId}}
Authorization: Bearer {{token}}
```

## Fluxo completo explicado

LOGIN → USUÁRIO AUTENTICADO → ENTRADA / SAÍDA → SERVICE → ATUALIZA ESTOQUE → REGISTRA MOVIMENTAÇÃO COM RESPONSÁVEL → BANCO DE DADOS → RELATÓRIO DE MOVIMENTAÇÕES.

Na implementação, a identidade é recuperada ANTES da gravação. Saldo, movimentação e responsável
são persistidos na mesma transação, e não em gravações independentes. O relatório apenas consulta;
ele não recalcula os saldos históricos a partir do saldo atual.

## Roteiro manual com dois usuários

1. Faça login como joao e cadastre Caneca com saldo 100: entrada inicial 0 → 100 com joao.
2. Registre ENTRADA 50: saldo 100 → 150, responsável joao.
3. Saia e entre como maria; registre SAIDA 20: saldo 150 → 130, responsável maria.
4. Filtre maria + SAIDA: deve aparecer somente a saída de 20.
5. Filtre período de hoje e item Caneca: devem aparecer as três operações.
6. Faça nova entrada; os saldos registrados nas linhas anteriores não podem mudar.
7. Tente excluir Caneca: 409 e nenhuma linha de histórico removida.
8. Com token de joao, envie responsavelId de maria no POST: 400, sem alteração de saldo.
9. Sem token, tente registrar/consultar: acesso bloqueado.
10. Consulte data sem registros: mensagem Nenhuma movimentação encontrada.

## Evoluções posteriores

- Exportação CSV/PDF usando os mesmos filtros e regras de permissão; não limitar a exportação somente à página visível.
- Nome completo opcional em Usuario, sem remover a relação por ID nem inventar nomes para contas antigas.
- Perfis de acesso: administrador vê todos; operador vê os próprios, caso essa seja a regra de negócio escolhida.
- Estorno vinculado à movimentação original, sem apagar o registro anterior.
- Migrações automatizadas e teste da concorrência contra MySQL real (por exemplo, Testcontainers).
- Desativar itens em vez de excluí-los, preservando consulta dos itens antigos nos filtros.
- Idempotência para impedir duplicidade em repetição de requisição após falha de rede.

