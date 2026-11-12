package back.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import back.persistence.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, String> {
}
