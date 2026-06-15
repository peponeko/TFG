package com.easy4you.service;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.dto.auth.LoginRequestDTO;
import com.easy4you.dto.auth.NivelEstudioRequestDTO;
import com.easy4you.dto.auth.PerfilUpdateRequestDTO;
import com.easy4you.dto.auth.RegisterRequestDTO;
import com.easy4you.model.entity.Usuario;

public interface AuthService {
  AuthResponseDTO login(LoginRequestDTO request);

  AuthResponseDTO register(RegisterRequestDTO request);

  Usuario actualizarPerfil(Long usuarioId, PerfilUpdateRequestDTO request);

  Usuario actualizarNivelEstudio(Long usuarioId, NivelEstudioRequestDTO request);
}

