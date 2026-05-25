package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaTemplateConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.schema-registry}")
    private String schemaRegistry;

    @Bean
    public ProducerFactory<String, LinkUpdateAvro> producerFactory() {
        Map<String, Object> prodConf = new HashMap<>();
        prodConf.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        prodConf.put(ProducerConfig.ACKS_CONFIG, "all");
        prodConf.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        prodConf.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        prodConf.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);
        prodConf.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        prodConf.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        prodConf.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        prodConf.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        prodConf.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        prodConf.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        prodConf.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024);
        return new DefaultKafkaProducerFactory<>(prodConf);
    }

    @Bean
    public KafkaTemplate<String, LinkUpdateAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
