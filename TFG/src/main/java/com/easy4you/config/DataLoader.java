package com.easy4you.config;

import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
// Carga datos iniciales (roles y usuario admin) al arrancar la aplicación
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  private static final String ADMIN_EMAIL = "admin@easy4you.com";
  private static final String ADMIN_PASSWORD = "Admin1234!";

  /**
   * Hash de ejemplo de BCrypt para la contraseña "password" (el que venía en el SQL inicial).
   * Si detectamos este hash, lo consideramos placeholder y lo reemplazamos por la contraseña del admin.
   */
  private static final String PLACEHOLDER_BCRYPT_PASSWORD_HASH =
      "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p922MY8W0QFH0Y2VqLHBci";

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    Rol rolAdmin = ensureRol("ADMIN", "Administrador del sistema");
    Rol rolUser = ensureRol("USER", "Usuario estándar");

    Usuario admin =
        usuarioRepository
            .findTopByEmailOrderByIdAsc(ADMIN_EMAIL)
            .orElseGet(
                () -> {
                  Usuario u = new Usuario();
                  u.setNombre("Administrador");
                  u.setApellidos("Easy4You");
                  u.setEmail(ADMIN_EMAIL);
                  return u;
                });

    admin.setActivo(true);
    admin.setVerificado(true);

    if (admin.getRoles() == null) {
      admin.setRoles(new HashSet<>());
    }
    admin.getRoles().add(rolAdmin);
    admin.getRoles().add(rolUser);

    if (shouldResetAdminPassword(admin.getPasswordHash())) {
      admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
      log.warn("Admin bootstrap: password actualizado para {} (valor por defecto)", ADMIN_EMAIL);
    }

    usuarioRepository.save(admin);
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

  private boolean shouldResetAdminPassword(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      return true;
    }
    if (PLACEHOLDER_BCRYPT_PASSWORD_HASH.equals(passwordHash)) {
      return true;
    }
    return !looksLikeBcrypt(passwordHash);
  }

  private boolean looksLikeBcrypt(String hash) {
    return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
  }
}
