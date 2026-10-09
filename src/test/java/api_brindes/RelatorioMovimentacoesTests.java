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
    @Test void exclusaoLogicaRetiraDoInventarioEPreservaHistorico() throws Exception {
        registrar(tokenJoao, "SAIDA", 20).andExpect(status().isCreated());
        registrar(tokenMaria, "ENTRADA", 50).andExpect(status().isCreated());
        mvc.perform(delete("/itens/" + caneca.getId()).header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isNoContent());
        assertThat(itens.findById(caneca.getId()).orElseThrow().isAtivo()).isFalse();
        mvc.perform(get("/itens").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/itens/relatorio").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDeBrindesCadastrados").value(0));
        mvc.perform(get("/itens/historico").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].ativo").value(false));
        registrar(tokenJoao, "ENTRADA", 1).andExpect(status().isNotFound());
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
    @Test void logoPublicaEApiSemTokenRetorna401() throws Exception {
        mvc.perform(get("/assets/truckvan-logo.jpeg")).andExpect(status().isOk());
        mvc.perform(get("/itens")).andExpect(status().isUnauthorized());
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"joao\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void somaQuantidadesSemOverflowDeInteger() throws Exception {
        caneca.setQuantidade(Integer.MAX_VALUE); itens.save(caneca);
        itens.save(new Item("MAX-02", "Outro estoque", Integer.MAX_VALUE, 0.1));
        mvc.perform(get("/itens/relatorio").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeBrindesCadastrados").value(4294967294L));
    }

    @Test void rejeitaPrecoComMaisDeDuasCasasOuAcimaDoLimite() throws Exception {
        for (String valor : List.of("1.234", "1000000000", "-1")) {
            mvc.perform(put("/itens/" + caneca.getId()).header("Authorization", "Bearer " + tokenJoao)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":" + valor + "}"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(itens.findById(caneca.getId()).orElseThrow().getValor()).isEqualTo(25.0);
    }

    @Test void itemExcluidoNaoAceitaEdicaoNemPerdeSaldoHistorico() throws Exception {
        mvc.perform(delete("/itens/" + caneca.getId()).header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isNoContent());
        mvc.perform(put("/itens/" + caneca.getId()).header("Authorization", "Bearer " + tokenJoao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10}"))
                .andExpect(status().isNotFound());
        assertThat(itens.findById(caneca.getId()).orElseThrow().getQuantidade()).isEqualTo(100);
    }

    @Test void corsPreflightAceitaOrigemConfigurada() throws Exception {
        mvc.perform(options("/itens").header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

}
