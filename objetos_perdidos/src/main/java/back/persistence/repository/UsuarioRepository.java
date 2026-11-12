package back.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import back.persistence.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, String> {

    @Modifying
    @Query("delete from UsuarioEntity u where lower(u.correo) <> lower(:correo)")
    int eliminarTodosExcepto(@Param("correo") String correo);
}
