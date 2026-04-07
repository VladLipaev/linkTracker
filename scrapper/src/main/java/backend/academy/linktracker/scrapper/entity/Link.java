package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url")
    private String url;

    @Column(name = "updated_at")
    private OffsetDateTime lastUpdated;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    public List<Chat> getChats() {
        return subscriptions.stream().map(Subscription::getChat).toList();
    }

    @Column(name = "checked_at")
    private OffsetDateTime lastCheckedAt;

    public void addChat(Chat chat) {
        Subscription sub = new Subscription(chat.getId(), this.getId());
        sub.setLink(this);
        sub.setChat(chat);
        sub.setTags(new ArrayList<>());
        this.subscriptions.add(sub);
    }

    @Builder
    public Link(String url, OffsetDateTime lastUpdated) {
        this.url = url;
        this.lastUpdated = lastUpdated;
    }
}
