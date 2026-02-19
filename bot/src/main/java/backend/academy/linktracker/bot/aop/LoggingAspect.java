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

    @Pointcut("execution(* backend.academy.linktracker.bot.configuration.TelegramUpdateDispatcher.dispatch(..))")
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

        try (var c1 = MDC.putCloseable("chat_id", chatId);
                var c2 = MDC.putCloseable("event.type", "command");
                var c3 = MDC.putCloseable("command.name", handler.getCommandName());
                var c4 = MDC.putCloseable("command.handler", handler.getClass().getSimpleName())) {

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
        try (var c1 = MDC.putCloseable("update_id", String.valueOf(update.updateId()));
                var c2 = MDC.putCloseable("chat_id", chatId);
                var c3 = MDC.putCloseable("event.type", "telegram_update")) {

            return jp.proceed();
        } catch (Throwable e) {
            MDC.put("error.kind", "dispatch_error");
            log.error("Failed to dispatch update", e);
            MDC.remove("error.kind");
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
        String rawText = (update.message() != null) ? update.message().text() : "non-text";

        try (var c1 = MDC.putCloseable("chat_id", chatId);
                var c2 = MDC.putCloseable("event.type", "unknown_command");
                var c3 = MDC.putCloseable("raw_text_length", String.valueOf(rawText.length()));
                var c4 = MDC.putCloseable("command.handler", handler.getClass().getSimpleName())) {

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
            try (var c1 = MDC.putCloseable("error.kind", "telegram_api_error");
                    var c2 = MDC.putCloseable("tg.error_code", String.valueOf(response.errorCode()))) {

                log.warn("Telegram API returned error: {}", response.description());
            }
        }
        return result;
    }

    private void logSuccess(long startTime) {
        MDC.put("event.status", "success");
        MDC.put("event.duration_ms", formatDuration(startTime));
        log.info("Operation completed successfully");
        MDC.remove("event.status");
        MDC.remove("event.duration_ms");
    }

    private void logFailure(long startTime, String errorKind, Throwable e) {
        MDC.put("event.status", "failure");
        MDC.put("event.duration_ms", formatDuration(startTime));
        MDC.put("error.kind", errorKind);
        log.error("Operation failed", e);
        MDC.remove("event.status");
        MDC.remove("event.duration_ms");
        MDC.remove("error.kind");
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
