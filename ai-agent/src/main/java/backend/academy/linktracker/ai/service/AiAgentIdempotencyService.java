package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.entity.Event;
import backend.academy.linktracker.ai.repository.IdempotencyRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAgentIdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public Optional<Event> findById(UUID eventId) {
        return idempotencyRepository.findById(eventId);
    }

    @Transactional
    public boolean tryLock(UUID eventId) {
        return idempotencyRepository.trySave(eventId, OffsetDateTime.now()) > 0;
    }

    public boolean exists(UUID eventId) {
        return idempotencyRepository.existsById(eventId);
    }

    @Transactional
    public void deleteOldEvents(OffsetDateTime threshold) {
        idempotencyRepository.deleteOldEvents(threshold);
    }

    @Transactional
    public void removeEvent(UUID eventId) {
        idempotencyRepository.removeEventById(eventId);
    }
}
