package backend.academy.linktracker.scrapper.config.metrics;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DatabaseTimingAspect {

    private final ScrapperMetrics metrics;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object measureTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            String scopeType = joinPoint.getSignature().getDeclaringType().getSimpleName() + "."
                    + joinPoint.getSignature().getName();
            metrics.recordRequestDuration(duration, "database", scopeType);
        }
    }
}
