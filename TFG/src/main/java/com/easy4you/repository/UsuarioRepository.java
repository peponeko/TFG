package com.easy4you.repository;

import com.easy4you.model.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findTopByEmailOrderByIdAsc(String email);

  boolean existsByEmail(String email);
}

