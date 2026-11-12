package back.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import back.persistence.entity.MovimientoPuntosEntity;

public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntosEntity, Long> {

    List<MovimientoPuntosEntity> findByCorreoOrderByIdDesc(String correo);

    @Query("select coalesce(sum(m.cantidad), 0) from MovimientoPuntosEntity m where m.correo = :correo")
    long saldo(@Param("correo") String correo);
}
