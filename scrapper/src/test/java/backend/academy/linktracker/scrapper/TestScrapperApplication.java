package backend.academy.linktracker.scrapper;

import backend.academy.linktracker.scrapper.config.TestBeans;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.SpringApplication;

@Disabled
public class TestScrapperApplication {

    static void main(String[] args) {
        SpringApplication.from(ScrapperApplication::main).with(TestBeans.class).run(args);
    }
}
