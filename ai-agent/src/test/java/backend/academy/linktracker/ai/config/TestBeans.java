package backend.academy.linktracker.ai.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;

@TestConfiguration
public class TestBeans {

    private static final String DLQ_TOPIC = "link-updates-topic-dlt";

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:migrations/db.changelog-master.xml");
        return liquibase;
    }

    @Bean
    public DlqListenerWatcher dlqListenerWatcher() {
        return new DlqListenerWatcher();
    }

    public static class DlqListenerWatcher {
        @KafkaListener(topics = DLQ_TOPIC, groupId = "test-dlq-group")
        public void listenDlq(ConsumerRecord<String, Object> record) {}
    }
}
