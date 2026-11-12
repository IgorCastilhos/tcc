package br.unisinos.cvmsignals.app.config;

import br.unisinos.cvmsignals.shared.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declara os quatro tópicos do backbone (Seção 4.2); criados na subida da aplicação. */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic docsDiscovered() {
        return TopicBuilder.name(Topics.DOCS_DISCOVERED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic docsParsed() {
        return TopicBuilder.name(Topics.DOCS_PARSED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic chunksEmbedded() {
        return TopicBuilder.name(Topics.CHUNKS_EMBEDDED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic signalsExtracted() {
        return TopicBuilder.name(Topics.SIGNALS_EXTRACTED).partitions(3).replicas(1).build();
    }
}
