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
