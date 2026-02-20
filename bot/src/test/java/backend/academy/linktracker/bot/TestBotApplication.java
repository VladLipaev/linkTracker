package backend.academy.linktracker.bot;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.SpringApplication;

@Disabled
public class TestBotApplication {

    static void main(String[] args) {
        SpringApplication.from(BotApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
