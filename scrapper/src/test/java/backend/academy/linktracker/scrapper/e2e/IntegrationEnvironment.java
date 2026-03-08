package backend.academy.linktracker.scrapper.e2e;

import java.nio.file.Paths;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class IntegrationEnvironment {

    protected static final Network network = Network.newNetwork();

    protected static final GenericContainer<?> scrapper = new GenericContainer<>(
                    new ImageFromDockerfile().withFileFromPath(".", Paths.get(".")))
            .withNetwork(network)
            .withNetworkAliases("scrapper")
            .withExposedPorts(8081)
            .withEnv("BOT_BASE_URL", "http://bot:8080")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    protected static final GenericContainer<?> bot = new GenericContainer<>(
                    new ImageFromDockerfile().withFileFromPath(".", Paths.get("../bot")))
            .withNetwork(network)
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
