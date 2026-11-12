package br.unisinos.cvmsignals.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

/** Clientes HTTP nomeados para as integrações externas (CVM, ENET, TEI, brapi.dev). */
@Configuration
public class RestClientsConfig {

    @Bean("cvmRestClient")
    public RestClient cvmRestClient(@Value("${cvm.base-url:https://dados.cvm.gov.br}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Downloads seguem URLs absolutas divulgadas pela CVM (rad.cvm.gov.br/ENET).
     *
     * <p>O ENET exige duas coisas que um cliente HTTP simples não faz:
     *
     * <ol>
     *   <li>User-Agent de navegador — sem ele o servidor trata a requisição
     *       de forma diferente;</li>
     *   <li><b>cookies</b>. A primeira requisição devolve a página
     *       intermediária do ASP.NET e emite {@code ASP.NET_SessionId} mais
     *       o cookie de afinidade do balanceador F5. Só quem devolve esses
     *       cookies na requisição seguinte recebe o binário — sem eles toda
     *       tentativa parece a primeira, o Tika extrai zero caractere e o
     *       documento vira FAILED.</li>
     * </ol>
     *
     * <p>Daí o CookieManager próprio: o retry do downloader vira uma segunda
     * visita com sessão, como num navegador, em vez de uma estreia eterna.
     */
    @Bean("downloadRestClient")
    public RestClient downloadRestClient(@Value("${cvm.download.user-agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36}") String userAgent) {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept", "*/*")
                .build();
    }

    @Bean("teiRestClient")
    public RestClient teiRestClient(@Value("${tei.url:http://localhost:8081}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("brapiRestClient")
    public RestClient brapiRestClient(@Value("${brapi.base-url:https://brapi.dev}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
