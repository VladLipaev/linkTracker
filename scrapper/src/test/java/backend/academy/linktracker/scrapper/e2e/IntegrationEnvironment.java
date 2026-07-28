package backend.academy.linktracker.scrapper.e2e;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class IntegrationEnvironment extends AbstractIntegrationTest{

    private static Path getModulePath(String moduleName) {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        // Если запускаем из корня проекта
        if (userDir.resolve(moduleName).toFile().exists()) {
            return userDir.resolve(moduleName);
        }
        // Если запускаем из внутри самого модуля (например, scrapper)
        if (userDir.getFileName().toString().equals(moduleName)) {
            return userDir;
        }
        // Если запускаем из другого дочернего модуля
        return userDir.getParent().resolve(moduleName);
    }

    protected static final GenericContainer<?> scrapper = new GenericContainer<>(
        new ImageFromDockerfile()
            .withDockerfile(getModulePath("scrapper").resolve("Dockerfile"))
            .withFileFromPath(".", getModulePath("scrapper"))
    )
            .withNetwork(AbstractIntegrationTest.SHARED_NETWORK)
            .withNetworkAliases("scrapper")
            .withExposedPorts(18081)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/scrapper")
            .withEnv("SPRING_DATASOURCE_USERNAME", "user")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "pass")
            .withEnv("SPRING_LIQUIBASE_ENABLED", "true")
            .withEnv("SPRING_LIQUIBASE_CHANGE_LOG", "classpath:migrations/db.changelog-master.xml")
            .withEnv("BOT_BASE_URL", "http://bot:18080")
            .dependsOn(AbstractIntegrationTest.POSTGRES)
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    protected static final GenericContainer<?> bot = new GenericContainer<>(
        new ImageFromDockerfile()
            .withDockerfile(getModulePath("bot").resolve("Dockerfile"))
            .withFileFromPath(".", getModulePath("bot"))
    )
            .withNetwork(AbstractIntegrationTest.SHARED_NETWORK)
            .withNetworkAliases("bot")
            .withExposedPorts(18080)
            .withEnv("SCRAPPER_BASE_URL", "http://scrapper:18081")
            .dependsOn(scrapper)
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    static {
        scrapper.start();
        bot.start();
    }

    protected String getScrapperUrl() {
        return "http://" + scrapper.getHost() + ":" + scrapper.getMappedPort(18081);
    }

    protected String getBotUrl() {
        return "http://" + bot.getHost() + ":" + bot.getMappedPort(18080);
    }
}
