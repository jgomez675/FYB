package back.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import back.persistence.entity.ObjetoEntity;

public interface ObjetoRepository extends JpaRepository<ObjetoEntity, Integer> {

    List<ObjetoEntity> findByCorreoUsuario(String correoUsuario);

    long countByEstado(String estado);
}
