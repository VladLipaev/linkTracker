package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "subscriptions")
public class Subscription {

    @EmbeddedId
    private SubscriptionId subscriptionId;

    @MapsId("chatId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @MapsId("linkId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id")
    private Link link;

    @ElementCollection
    @CollectionTable(
            name = "subscriptions_tags",
            joinColumns = {@JoinColumn(name = "chat_id"), @JoinColumn(name = "link_id")})
    @Column(name = "tag")
    @Fetch(FetchMode.SUBSELECT)
    private List<String> tags;

    public Subscription(Long chatId, Long linkId) {
        this.subscriptionId = new SubscriptionId(chatId, linkId);
    }
}
