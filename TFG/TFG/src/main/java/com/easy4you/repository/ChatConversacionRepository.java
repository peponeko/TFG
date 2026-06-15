package com.easy4you.repository;

import com.easy4you.model.entity.ChatConversacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversacionRepository extends JpaRepository<ChatConversacion, Long> {
  List<ChatConversacion> findByUsuarioIdOrderByUpdatedAtDesc(Long usuarioId);
}

