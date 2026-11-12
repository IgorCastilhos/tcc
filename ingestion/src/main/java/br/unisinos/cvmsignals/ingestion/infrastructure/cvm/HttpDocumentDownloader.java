package br.unisinos.cvmsignals.ingestion.infrastructure.cvm;

import br.unisinos.cvmsignals.ingestion.application.port.DocumentDownloadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Download dos binários no ENET da CVM.
 *
 * <p>Sob rajada de requisições o ENET devolve, com HTTP 200, a página
 * intermediária do ASP.NET em vez do documento — o Tika então extrai zero
 * caractere e o documento inteiro é marcado como FAILED. Como a mesma URL
 * costuma funcionar segundos depois, vale detectar esse corpo e repetir
 * com uma pausa crescente em vez de descartar o documento.
 *
 * <p><b>A URL vem de um CSV externo</b> (o dataset da CVM) e o cliente segue
 * redirecionamentos, então baixar às cegas seria um SSRF: bastaria uma linha
 * adulterada, ou um redirect, para a aplicação buscar um endereço interno da
 * rede em que ela roda. Por isso só se baixa de hosts explicitamente
 * permitidos, e só por HTTPS.
 */
@Component
public class HttpDocumentDownloader implements DocumentDownloadPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDocumentDownloader.class);

    /** A página intermediária é pequena e traz este título. */
    private static final int INTERSTITIAL_MAX_BYTES = 20_000;
    private static final String INTERSTITIAL_MARKER = "ENET Download de Documento";

    private final RestClient downloadRestClient;
    private final int maxAttempts;
    private final long backoffMillis;
    private final Set<String> allowedHosts;
    private final int maxBytes;

    public HttpDocumentDownloader(@Qualifier("downloadRestClient") RestClient downloadRestClient,
                                  @Value("${cvm.download.max-attempts:4}") int maxAttempts,
                                  @Value("${cvm.download.backoff-ms:1500}") long backoffMillis,
                                  @Value("${cvm.download.allowed-hosts:rad.cvm.gov.br,www.rad.cvm.gov.br,dados.cvm.gov.br}") String allowedHosts,
                                  @Value("${cvm.download.max-bytes:52428800}") int maxBytes) {
        this.downloadRestClient = downloadRestClient;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMillis = Math.max(0, backoffMillis);
        this.maxBytes = maxBytes;
        this.allowedHosts = Arrays.stream(allowedHosts.split(","))
                .map(String::strip)
                .filter(h -> !h.isEmpty())
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public byte[] download(String url) {
        URI uri = requireAllowed(url);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                byte[] body = downloadRestClient.get().uri(uri).retrieve().body(byte[].class);
                if (body == null || body.length == 0) {
                    throw new IllegalStateException("Download vazio: " + url);
                }
                if (body.length > maxBytes) {
                    // Não adianta repetir: o arquivo é grande demais e insistir
                    // só queimaria banda. Falha direto para o documento virar
                    // FAILED e ser inspecionado à mão.
                    throw new DocumentTooLargeException(url, body.length, maxBytes);
                }
                if (!isInterstitial(body)) {
                    return body;
                }
                last = new IllegalStateException(
                        "ENET devolveu a página intermediária em vez do documento: " + url);
                log.warn("Tentativa {}/{}: interstitial de {} bytes em {}",
                        attempt, maxAttempts, body.length, url);
            } catch (DocumentTooLargeException e) {
                throw e;
            } catch (RuntimeException e) {
                last = e;
                log.warn("Tentativa {}/{}: {} em {}", attempt, maxAttempts, e.toString(), url);
            }
            if (attempt < maxAttempts) {
                sleep(backoffMillis * attempt);
            }
        }
        throw last;
    }

    /** Só HTTPS e só para os hosts da CVM: a URL de origem não é confiável. */
    private URI requireAllowed(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BlockedDownloadException("URL malformada: " + url);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme)) {
            throw new BlockedDownloadException("Esquema não permitido (" + scheme + "): " + url);
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!allowedHosts.contains(host)) {
            throw new BlockedDownloadException("Host fora da lista permitida (" + host + "): " + url);
        }
        return uri;
    }

    /** Corpo pequeno contendo o título da página de download é o interstitial. */
    private boolean isInterstitial(byte[] body) {
        if (body.length > INTERSTITIAL_MAX_BYTES) {
            return false;
        }
        return new String(body, StandardCharsets.UTF_8).contains(INTERSTITIAL_MARKER);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Download interrompido", e);
        }
    }

    /** Destino recusado antes de qualquer requisição sair da aplicação. */
    public static class BlockedDownloadException extends RuntimeException {
        public BlockedDownloadException(String message) {
            super(message);
        }
    }

    public static class DocumentTooLargeException extends RuntimeException {
        public DocumentTooLargeException(String url, int size, int limit) {
            super("Documento excede o limite de " + limit + " bytes (" + size + "): " + url);
        }
    }
}
