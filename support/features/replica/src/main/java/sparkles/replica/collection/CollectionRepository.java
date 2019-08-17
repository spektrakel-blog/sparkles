package sparkles.replica.collection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {

  boolean existsByName(String name);
}
