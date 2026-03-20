package backend.academy.linktracker.scrapper.repository;

import java.util.List;
import java.util.Optional;

public interface BaseRepository<E, T> {

    E save(E entity);

    Optional<E> findById(T id);

    List<E> findAll();

    void deleteById(T id);

    boolean existsById(T id);
}
