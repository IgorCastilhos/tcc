package br.unisinos.cvmsignals.app.config;

import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.EnumMap;
import java.util.Map;

/**
 * Os três braços experimentais como ChatClients homogêneos. Com múltiplos
 * ChatModels no classpath, o builder padrão do Spring AI é desabilitado
 * (spring.ai.chat.client.enabled=false) e os clientes são construídos
 * explicitamente a partir dos beans autoconfigurados de cada provider.
 */
@Configuration
public class AiConfig {

    /**
     * Substitui o AnthropicChatModel autoconfigurado para deixar temperature
     * nulo. AnthropicChatProperties injeta DEFAULT_TEMPERATURE no construtor,
     * de modo que omitir a chave no application.yml não basta — e a geração
     * Sonnet 5 / Opus 5 removeu os parâmetros de amostragem, respondendo
     * HTTP 400 "`temperature` is deprecated for this model". O braço Claude
     * roda, portanto, sob a amostragem própria do modelo, diferente dos braços
     * GPT-4o e Gemma (temperature 0.0) — assimetria a declarar nas limitações.
     */
    @Bean
    public AnthropicChatModel anthropicChatModel(
            AnthropicApi anthropicApi,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry,
            @Value("${spring.ai.anthropic.chat.options.model:}") String model) {
        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(model.isBlank() ? AnthropicChatModel.DEFAULT_MODEL_NAME : model)
                        .maxTokens(AnthropicChatModel.DEFAULT_MAX_TOKENS)
                        .build())
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean
    public Map<LlmProvider, ChatClient> chatClients(AnthropicChatModel anthropicChatModel,
                                                    OpenAiChatModel openAiChatModel,
                                                    OllamaChatModel ollamaChatModel) {
        Map<LlmProvider, ChatClient> clients = new EnumMap<>(LlmProvider.class);
        clients.put(LlmProvider.CLAUDE_SONNET, ChatClient.builder(anthropicChatModel).build());
        clients.put(LlmProvider.GPT_4O, ChatClient.builder(openAiChatModel).build());
        clients.put(LlmProvider.GEMMA, ChatClient.builder(ollamaChatModel).build());
        return clients;
    }

    /**
     * Falha rápido se um braço habilitado do experimento não estiver operante:
     * provider silenciosamente quebrado invalidaria o comparativo entre modelos.
     * Verifica credencial e, em seguida, faz uma chamada mínima a cada provider —
     * credencial válida com modelo inexistente ou parâmetro rejeitado só aparece
     * na chamada real, e antes disso a extração devolvia HTTP 200 com zero sinais.
     */
    @Bean
    public ApplicationRunner llmReadinessCheck(
            Map<LlmProvider, ChatClient> chatClients,
            @Value("${extraction.providers:}") String providers,
            @Value("${spring.ai.anthropic.api-key:}") String anthropicKey,
            @Value("${spring.ai.openai.api-key:}") String openAiKey) {
        return args -> {
            Map<LlmProvider, String> keys = Map.of(
                    LlmProvider.CLAUDE_SONNET, anthropicKey,
                    LlmProvider.GPT_4O, openAiKey);
            List<LlmProvider> habilitados = Arrays.stream(providers.split(","))
                    .map(String::strip)
                    .filter(p -> !p.isEmpty())
                    .map(LlmProvider::valueOf)
                    .toList();

            List<String> semChave = habilitados.stream()
                    .filter(keys::containsKey)
                    .filter(p -> keys.get(p).isBlank() || "not-configured".equals(keys.get(p)))
                    .map(Enum::name)
                    .toList();
            if (!semChave.isEmpty()) {
                throw new IllegalStateException(
                        "Providers em extraction.providers sem credencial: " + semChave
                                + ". Configure a chave no .env ou remova o provider da lista.");
            }

            List<String> inoperantes = new ArrayList<>();
            for (LlmProvider provider : habilitados) {
                try {
                    chatClients.get(provider).prompt().user("ping").call().content();
                } catch (Exception e) {
                    inoperantes.add(provider.name() + " (" + e.getMessage() + ")");
                }
            }
            if (!inoperantes.isEmpty()) {
                throw new IllegalStateException(
                        "Providers em extraction.providers com credencial válida mas inoperantes: "
                                + inoperantes + ". Verifique o nome do modelo e os parâmetros no .env.");
            }
        };
    }
}
