package backend.academy.linktracker.bot.aop;

import backend.academy.linktracker.bot.handler.CommandHandler;
import backend.academy.linktracker.bot.handler.UnknownCommandHandler;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    @Pointcut("within(backend.academy.linktracker.bot.handler.CommandHandler+)")
    public void isCommandHandler() {}

    @Pointcut("within(backend.academy.linktracker.bot.handler.UnknownCommandHandler)")
    public void isUnknownCommandHandler() {}

    @Pointcut("execution(* backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher.dispatch(..))")
    public void telegramUpdateDispatch() {}

    @Pointcut(
            "isCommandHandler() && execution(public void backend.academy.linktracker.bot.handler.CommandHandler.handle(com.pengrad.telegrambot.model.Update))")
    public void anyCommandHandlerHandling() {}

    @Pointcut(
            "isUnknownCommandHandler() && execution(public void backend.academy.linktracker.bot.handler.UnknownCommandHandler.handle(com.pengrad.telegrambot.model.Update))")
    public void unknownCommandHandlerHandling() {}

    @Pointcut("execution(* backend.academy.linktracker.bot.configuration.TelegramClient*.*(..))")
    public void telegramClientMethods() {}

    @Around(value = "anyCommandHandlerHandling() && target(handler) && args(update)", argNames = "jp,handler,update")
    public Object commandHandlingContext(ProceedingJoinPoint jp, CommandHandler handler, Update update)
            throws Throwable {
        long startTime = System.nanoTime();
        String chatId = extractChatId(update);

        try (var _ = MDC.putCloseable("chat_id", chatId);
                var _ = MDC.putCloseable("event.type", "command");
                var _ = MDC.putCloseable("command.name", handler.getCommandName());
                var _ = MDC.putCloseable("command.handler", handler.getClass().getSimpleName())) {

            Object res = jp.proceed();

            logSuccess(startTime);
            return res;
        } catch (Throwable e) {
            logFailure(startTime, "command_execution_error", e);
            throw e;
        }
    }

    @Around("telegramUpdateDispatch() && args(update)")
    public Object wrapDispatch(ProceedingJoinPoint jp, Update update) throws Throwable {
        String chatId = extractChatId(update);

        try (var _ = MDC.putCloseable("update_id", String.valueOf(update.updateId()));
                var _ = MDC.putCloseable("chat_id", chatId);
                var _ = MDC.putCloseable("event.type", "telegram_update")) {

            return jp.proceed();
        } catch (Throwable e) {
            log.atError()
                    .setMessage("Failed to dispatch update")
                    .addKeyValue("error.kind", "dispatch_error")
                    .setCause(e)
                    .log();
            throw e;
        }
    }

    @Around(
            value = "unknownCommandHandlerHandling() && target(handler) && args(update)",
            argNames = "jp,handler,update")
    public Object unknownCommandHandlingContext(ProceedingJoinPoint jp, UnknownCommandHandler handler, Update update)
            throws Throwable {
        long startTime = System.nanoTime();
        String chatId = extractChatId(update);
        String rawText = (update.message() != null && update.message().text() != null)
                ? update.message().text()
                : "non-text";

        try (var _ = MDC.putCloseable("chat_id", chatId);
                var _ = MDC.putCloseable("event.type", "unknown_command");
                var _ = MDC.putCloseable("raw_text_length", String.valueOf(rawText.length()));
                var _ = MDC.putCloseable("command.handler", handler.getClass().getSimpleName())) {

            Object res = jp.proceed();

            logSuccess(startTime);
            return res;
        } catch (Throwable e) {
            logFailure(startTime, "unknown_command_error", e);
            throw e;
        }
    }

    @Around("telegramClientMethods()")
    public Object traceTelegramApi(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();

        if (result instanceof BaseResponse response && !response.isOk()) {
            log.atWarn()
                    .setMessage("Telegram API returned error")
                    .addKeyValue("error.kind", "telegram_api_error")
                    .addKeyValue("tg.error_code", response.errorCode())
                    .addKeyValue("tg.error_description", response.description())
                    .log();
        }
        return result;
    }

    private void logSuccess(long startTime) {
        log.atInfo()
                .setMessage("Operation completed successfully")
                .addKeyValue("event.status", "success")
                .addKeyValue("event.duration_ms", formatDuration(startTime))
                .log();
    }

    private void logFailure(long startTime, String errorKind, Throwable e) {
        log.atError()
                .setMessage("Operation failed")
                .addKeyValue("event.status", "failure")
                .addKeyValue("event.duration_ms", formatDuration(startTime))
                .addKeyValue("error.kind", errorKind)
                .setCause(e)
                .log();
    }

    private String extractChatId(Update update) {
        return (update.message() != null && update.message().chat() != null)
                ? String.valueOf(update.message().chat().id())
                : "unknown";
    }

    private String formatDuration(long startTime) {
        return String.format("%.2f", (System.nanoTime() - startTime) / 1_000_000.0);
    }
}
