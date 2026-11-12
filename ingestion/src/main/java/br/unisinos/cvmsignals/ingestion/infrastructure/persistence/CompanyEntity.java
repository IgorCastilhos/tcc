package br.unisinos.cvmsignals.ingestion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "empresa")
public class CompanyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "codigo_cvm")
    private String codigoCvm;

    @Column(nullable = false)
    private String nome;

    private String ticker;

    protected CompanyEntity() {
    }

    public CompanyEntity(UUID id, String cnpj, String codigoCvm, String nome, String ticker) {
        this.id = id;
        this.cnpj = cnpj;
        this.codigoCvm = codigoCvm;
        this.nome = nome;
        this.ticker = ticker;
    }

    public UUID getId() { return id; }
    public String getCnpj() { return cnpj; }
    public String getCodigoCvm() { return codigoCvm; }
    public String getNome() { return nome; }
    public String getTicker() { return ticker; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCodigoCvm(String codigoCvm) { this.codigoCvm = codigoCvm; }
}
