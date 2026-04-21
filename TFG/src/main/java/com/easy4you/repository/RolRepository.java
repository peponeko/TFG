package com.easy4you.repository;

import com.easy4you.model.entity.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {
  Optional<Rol> findTopByNombreIgnoreCaseOrderByIdAsc(String nombre);
}
