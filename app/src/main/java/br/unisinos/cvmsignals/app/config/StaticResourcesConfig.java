package br.unisinos.cvmsignals.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Política de cache da interface React.
 *
 * <p>O Vite nomeia os bundles com hash do conteúdo, então um arquivo em
 * {@code /assets/} nunca muda de conteúdo sem mudar de nome: pode ser
 * cacheado agressivamente. O {@code index.html}, ao contrário, tem nome fixo
 * e é quem aponta para o bundle da vez — se o navegador o guardar, o
 * participante continua carregando a versão anterior da interface depois de
 * uma nova publicação. Sem estas regras o Spring não envia Cache-Control
 * algum e o navegador decide sozinho, que foi exatamente o que aconteceu
 * durante o desenvolvimento.
 */
@Configuration
public class StaticResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // O snapshot é regenerado a cada semana do estudo, sob o mesmo nome.
        registry.addResourceHandler("/data/**")
                .addResourceLocations("classpath:/static/data/")
                .setCacheControl(CacheControl.noCache());

        registry.addResourceHandler("/", "/index.html")
                .addResourceLocations("classpath:/static/index.html")
                .setCacheControl(CacheControl.noCache());
    }
}
