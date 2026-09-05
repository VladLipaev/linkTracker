package backend.academy.linktracker.bot;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportGrpcClients(basePackages = "backend.academy.linktracker.grpc")
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class BotApplication {

    static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
