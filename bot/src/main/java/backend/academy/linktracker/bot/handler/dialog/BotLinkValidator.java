package backend.academy.linktracker.bot.handler.dialog;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BotLinkValidator {
    private static final Pattern GITHUB_PATTERN = Pattern.compile("^https://github\\.com/[\\w.-]+/[\\w.-]+/?$");
    private static final Pattern SO_PATTERN = Pattern.compile("^https://stackoverflow\\.com/questions/\\d+(/.*)?$");

    public boolean isValid(String url) {
        return GITHUB_PATTERN.matcher(url).matches() || SO_PATTERN.matcher(url).matches();
    }
}
