package br.unisinos.cvmsignals.signalextraction.infrastructure.springai;

import br.unisinos.cvmsignals.signalextraction.application.SignalDraft;
import br.unisinos.cvmsignals.signalextraction.application.port.LlmExtractionPort;
import br.unisinos.cvmsignals.signalextraction.domain.LlmProvider;
import br.unisinos.cvmsignals.signalextraction.domain.SignalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Único ponto do sistema que toca LLMs. Os três providers chegam como
 * ChatClients homogêneos construídos pelo Spring AI; a saída é convertida
 * em records via saída estruturada (.entity), eliminando parsing manual.
 */
@Component
public class SpringAiLlmAdapter implements LlmExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmAdapter.class);

    private final Map<LlmProvider, ChatClient> clients;
    private final Map<SignalType, String> templates = new EnumMap<>(SignalType.class);

    public SpringAiLlmAdapter(Map<LlmProvider, ChatClient> chatClients) {
        this.clients = chatClients;
        templates.put(SignalType.SENTIMENTO, loadTemplate("prompts/sentimento.st"));
        templates.put(SignalType.RISCO, loadTemplate("prompts/risco.st"));
        templates.put(SignalType.EVENTO_CORPORATIVO, loadTemplate("prompts/eventos.st"));
    }

    @Override
    public List<SignalDraft> extract(LlmProvider provider, SignalType type, String companyName, String context) {
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("Provider não configurado: " + provider);
        }
        String safeContext = sanitize(context);
        try {
            if (type == SignalType.EVENTO_CORPORATIVO) {
                EventList events = client.prompt()
                        .user(u -> u.text(templates.get(type))
                                .param("empresa", companyName)
                                .param("contexto", safeContext))
                        .call()
                        .entity(EventList.class);
                if (events == null || events.eventos() == null) {
                    return List.of();
                }
                return events.eventos().stream()
                        .map(e -> new SignalDraft(e.tipo(), clamp(e.confianca()), e.descricao(), e.evidencia()))
                        .toList();
            }
            SingleSignal signal = client.prompt()
                    .user(u -> u.text(templates.get(type))
                            .param("empresa", companyName)
                            .param("contexto", safeContext))
                    .call()
                    .entity(SingleSignal.class);
            if (signal == null) {
                return List.of();
            }
            return List.of(new SignalDraft(signal.rotulo(), clamp(signal.confianca()),
                    signal.justificativa(), signal.evidencia()));
        } catch (RuntimeException e) {
            // Um braço falhar (rate limit, timeout local do Gemma) não pode derrubar o comparativo.
            log.error("Extração {} falhou para provider {}: {}", type, provider, e.getMessage());
            return List.of();
        }
    }

    /**
     * Neutraliza o texto do documento antes de ele entrar no prompt.
     *
     * <p>O conteúdo vem de arquivos publicados por terceiros e é montado
     * dentro de um template. Duas coisas precisam ser desarmadas:
     *
     * <ul>
     *   <li>as <b>marcas de delimitação</b> do prompt — sem isso um documento
     *       poderia escrever a marca de fechamento e continuar o texto como se
     *       fosse instrução do sistema;</li>
     *   <li>as <b>chaves</b>, que são a sintaxe de interpolação do template do
     *       Spring AI: um {@code {algo}} vindo do documento seria tratado como
     *       variável, quebrando a renderização ou descartando trecho.</li>
     * </ul>
     *
     * <p>Isto reduz a superfície, mas não elimina a injeção por instrução em
     * linguagem natural: a defesa restante é o próprio prompt mandar tratar o
     * bloco como dado, e a saída ser estruturada (ver {@code .entity}).
     */
    static String sanitize(String context) {
        if (context == null) {
            return "";
        }
        return context
                .replace("{", "(")
                .replace("}", ")")
                .replaceAll("(?i)<<<\\s*(INICIO|FIM)_DOS_TRECHOS\\s*>>>", "[marca removida]");
    }

    private double clamp(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String loadTemplate(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Template ausente: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar template " + path, e);
        }
    }

    record SingleSignal(String rotulo, Double confianca, String justificativa, String evidencia) {
    }

    record EventList(List<EventItem> eventos) {
    }

    record EventItem(String tipo, Double confianca, String descricao, String evidencia) {
    }
}
