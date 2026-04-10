package backend.academy.linktracker.scrapper.e2e;

import backend.academy.linktracker.scrapper.config.TestBeans;
import java.nio.file.Paths;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestBeans.class)
@Testcontainers
public abstract class IntegrationEnvironment {

    protected static final GenericContainer<?> scrapper = new GenericContainer<>(
                    new ImageFromDockerfile().withFileFromPath(".", Paths.get(".")))
            .withNetwork(TestBeans.SHARED_NETWORK)
            .withNetworkAliases("scrapper")
            .withExposedPorts(8081)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/scrapper")
            .withEnv("SPRING_DATASOURCE_USERNAME", "user")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "pass")
            .withEnv("SPRING_LIQUIBASE_ENABLED", "true")
            .withEnv("SPRING_LIQUIBASE_CHANGE_LOG", "classpath:migrations/db.changelog-master.xml")
            .withEnv("BOT_BASE_URL", "http://bot:8080")
            .dependsOn(TestBeans.POSTGRES)
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    protected static final GenericContainer<?> bot = new GenericContainer<>(
                    new ImageFromDockerfile().withFileFromPath(".", Paths.get("../bot")))
            .withNetwork(TestBeans.SHARED_NETWORK)
            .withNetworkAliases("bot")
            .withExposedPorts(8080)
            .withEnv("SCRAPPER_BASE_URL", "http://scrapper:8081")
            .dependsOn(scrapper)
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    static {
        scrapper.start();
        bot.start();
    }

    protected String getScrapperUrl() {
        return "http://" + scrapper.getHost() + ":" + scrapper.getMappedPort(8081);
    }

    protected String getBotUrl() {
        return "http://" + bot.getHost() + ":" + bot.getMappedPort(8080);
    }
}
