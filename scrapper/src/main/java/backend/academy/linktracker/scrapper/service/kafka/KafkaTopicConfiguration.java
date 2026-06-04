package backend.academy.linktracker.scrapper.service.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic newTopic(
            @Value("${app.kafka.topic.name:link.raw-updates}") String topicName,
            @Value("${app.kafka.topic.partitions:3}") int partitions,
            @Value("${app.kafka.min-insync-replicas:2}") int inSync,
            @Value("${app.kafka.topic.replicas:3}") int replicas) {
        return TopicBuilder.name(topicName)
                // сделал 3 партиции так как у нас 3 брокера чтобы равномерно была распределена нагрузка между ними
                .partitions(partitions)
                // сделал 3 реплики то есть чтобы каждая партиция хранилась на разных брокерах если какой то из них
                // слетит чтобы не потерять сообщения
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(inSync))
                .build();
    }
}
