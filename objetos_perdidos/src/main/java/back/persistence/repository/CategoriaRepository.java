package back.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import back.persistence.entity.CategoriaEntity;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    Optional<CategoriaEntity> findByNombreIgnoreCase(String nombre);
}
