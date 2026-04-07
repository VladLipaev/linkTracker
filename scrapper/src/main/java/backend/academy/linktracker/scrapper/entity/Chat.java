package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chats")
public class Chat {

    @Id
    @Setter
    private Long id;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    public Chat(Long id) {
        this.id = id;
    }

    public List<Link> getLinks() {
        return subscriptions.stream()
                .map(Subscription::getLink)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public void addLink(Link link) {
        Subscription sub = new Subscription(this.id, link.getId());
        sub.setChat(this);
        sub.setLink(link);
        this.subscriptions.add(sub);
    }
}
