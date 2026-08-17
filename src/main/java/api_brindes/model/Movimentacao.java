package api_brindes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // É assim que o Spring faz o JOIN com a tabela de Itens!
    @ManyToOne
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    @Column(nullable = false, length = 10)
    private String tipo; // "ENTRADA" ou "SAIDA"

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "data_movimentacao")
    private LocalDateTime dataMovimentacao;

    // Esse método roda automaticamente antes de salvar no banco, preenchendo a data e hora atuais
    @PrePersist
    public void preencherData() {
        this.dataMovimentacao = LocalDateTime.now();
    }

    // Construtor vazio (obrigatório para o Spring)
    public Movimentacao() {}

    // --- Getters e Setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public LocalDateTime getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(LocalDateTime dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }
}