package br.unisinos.cvmsignals.ingestion.domain;

import java.util.Objects;
import java.util.UUID;

/** Companhia aberta registrada na CVM. Identidade de negócio: CNPJ. */
public final class Company {

    private final UUID id;
    private final String cnpj;
    private final String cvmCode;
    private final String name;
    private final String ticker;

    public Company(UUID id, String cnpj, String cvmCode, String name, String ticker) {
        this.id = Objects.requireNonNull(id, "id");
        this.cnpj = normalizeCnpj(cnpj);
        this.cvmCode = cvmCode;
        this.name = Objects.requireNonNull(name, "name");
        this.ticker = ticker;
    }

    public static String normalizeCnpj(String raw) {
        Objects.requireNonNull(raw, "cnpj");
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 14) {
            throw new IllegalArgumentException("CNPJ inválido: " + raw);
        }
        return digits;
    }

    public UUID id() { return id; }
    public String cnpj() { return cnpj; }
    public String cvmCode() { return cvmCode; }
    public String name() { return name; }
    public String ticker() { return ticker; }
}
