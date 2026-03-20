package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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

    @ManyToMany(mappedBy = "chats", fetch = FetchType.LAZY)
    private List<Link> links = new ArrayList<>();

    public Chat(Long id) {
        this.id = id;
    }
}
