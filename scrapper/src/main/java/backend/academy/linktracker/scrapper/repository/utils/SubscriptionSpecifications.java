package backend.academy.linktracker.scrapper.repository.utils;

import backend.academy.linktracker.scrapper.entity.Subscription;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class SubscriptionSpecifications {

    public static Specification<Subscription> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.join("tags")), "%" + tag.toLowerCase() + "%");
        };
    }

    public static Specification<Subscription> urlContains(String substring) {
        return (root, query, cb) -> {
            if (substring == null) return cb.conjunction();
            return cb.like(cb.lower(root.join("link").get("url")), "%" + substring.toLowerCase() + "%");
        };
    }

    public static Specification<Subscription> chatIdEquals(Long chatId) {
        return (root, query, cb) -> {
            if (chatId == null) return cb.conjunction();
            return cb.equal(root.get("chat").get("id"), chatId);
        };
    }

    public static Specification<Subscription> updatedAfter(Instant date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.join("link").get("lastCheckedAt"), date);
        };
    }

    public static Specification<Subscription> updatedBefore(Instant date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.join("link").get("lastCheckedAt"), date);
        };
    }

}
