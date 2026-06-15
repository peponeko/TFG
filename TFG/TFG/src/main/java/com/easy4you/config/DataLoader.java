package com.easy4you.config;

import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
// Carga datos iniciales (roles y usuario admin) al arrancar la aplicación
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true", matchIfMissing = false)
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  @Value("${app.bootstrap.admin.email:}")
  private String adminEmail;

  @Value("${app.bootstrap.admin.password:}")
  private String adminPassword;

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    Rol rolAdmin = ensureRol("ADMIN", "Administrador del sistema");
    Rol rolUser = ensureRol("USER", "Usuario estándar");

    if (adminEmail == null || adminEmail.isBlank()) {
      log.info("Bootstrap desactivado: no se ha configurado app.bootstrap.admin.email");
      return;
    }

    if (adminPassword == null || adminPassword.isBlank()) {
      log.warn("Bootstrap desactivado: no se ha configurado app.bootstrap.admin.password");
      return;
    }

    Usuario admin =
        usuarioRepository
            .findTopByEmailOrderByIdAsc(adminEmail.trim())
            .orElseGet(
                () -> {
                  Usuario u = new Usuario();
                  u.setNombre("Administrador");
                  u.setApellidos("Easy4You");
                  u.setEmail(adminEmail.trim());
                  u.setActivo(true);
                  u.setVerificado(true);
                  return u;
                });

    // Por seguridad: no reseteamos contraseñas existentes. Solo creamos si faltaba.
    if (admin.getId() == null) {
      admin.setPasswordHash(passwordEncoder.encode(adminPassword));
      if (admin.getRoles() == null) {
        admin.setRoles(new HashSet<>());
      }
      admin.getRoles().add(rolAdmin);
      admin.getRoles().add(rolUser);
      usuarioRepository.save(admin);
      log.info("Bootstrap: usuario admin creado (email={})", admin.getEmail());
    } else {
      log.info("Bootstrap: usuario admin ya existe (email={})", admin.getEmail());
    }
  }

  private Rol ensureRol(String nombre, String descripcion) {
    return rolRepository
        .findTopByNombreIgnoreCaseOrderByIdAsc(nombre)
        .orElseGet(
            () -> {
              Rol rol = new Rol();
              rol.setNombre(nombre);
              rol.setDescripcion(descripcion);
              return rolRepository.save(rol);
            });
  }

  // Nota: evitamos lógica de "reset admin" automático para no introducir puertas traseras.
}
