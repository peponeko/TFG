package com.easy4you.service;

import com.easy4you.dto.auth.AuthResponseDTO;

public interface AuthRefreshService {
  AuthResponseDTO refresh(String token);
}

