package com.easy4you.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTest {

  @Autowired MockMvc mockMvc;

  @Test
  void apiSinToken_devuelve401() throws Exception {
    mockMvc.perform(get("/api/documentos")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminSinLogin_redirigeALogin() throws Exception {
    // con form login, lo normal es 302 hacia /login
    mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
  }
}

