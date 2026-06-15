package com.easy4you.repository;

import com.easy4you.model.entity.ChatMensaje;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {
  List<ChatMensaje> findByConversacionIdOrderByCreatedAtAsc(Long conversacionId);
}

