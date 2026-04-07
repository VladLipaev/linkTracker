package backend.academy.linktracker.scrapper.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface BaseRepository<E, T> {

    Slice<E> findAll(Pageable pageable);

    E save(E entity);

    Optional<E> findById(T id);

    List<E> findAll();

    void deleteById(T id);

    boolean existsById(T id);
}
