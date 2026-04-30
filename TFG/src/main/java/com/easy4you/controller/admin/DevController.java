package com.easy4you.controller.admin;

import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.sql.DataSource;

/**
 * Controlador de desarrollo para resolver problemas de login.
 * Solo usar en entorno local. Deshabilitar en producción.
 */
@Controller
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;
  private final DataSource dataSource;

  @GetMapping("/status")
  @ResponseBody
  @Transactional(readOnly = true)
  public Map<String, Object> status() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("database", resolveDatabaseName());
    out.put("dbUser", resolveDbUser());

    Usuario admin = usuarioRepository.findByEmail("admin@easy4you.com").orElse(null);
    out.put("adminExists", admin != null);

    if (admin != null) {
      out.put("adminId", admin.getId());
      out.put("adminActivo", admin.isActivo());
      out.put("adminVerificado", admin.isVerificado());
      out.put(
          "adminRoles",
          admin.getRoles() == null ? null : admin.getRoles().stream().map(Rol::getNombre).sorted().toList());
      out.put("adminPasswordMatches_Admin1234", passwordEncoder.matches("Admin1234!", admin.getPasswordHash()));
      out.put("adminPasswordMatches_password", passwordEncoder.matches("password", admin.getPasswordHash()));
    }

    return out;
  }

  private String resolveDatabaseName() {
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
      if (rs.next()) {
        return rs.getString(1);
      }
      return null;
    } catch (Exception ex) {
      return "ERROR: " + ex.getClass().getSimpleName();
    }
  }

  private String resolveDbUser() {
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT CURRENT_USER()")) {
      if (rs.next()) {
        return rs.getString(1);
      }
      return null;
    } catch (Exception ex) {
      return "ERROR: " + ex.getClass().getSimpleName();
    }
  }

  /**
   * Resetea la contraseña del admin a Admin1234!
   * Visita: http://localhost:8080/dev/reset-admin
   */
  @GetMapping("/reset-admin")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public String resetAdmin(RedirectAttributes redirectAttributes) {
    Usuario admin = usuarioRepository.findByEmail("admin@easy4you.com").orElse(null);

    Rol rolAdmin = rolRepository.findTopByNombreIgnoreCaseOrderByIdAsc("ADMIN").orElseGet(() -> {
      Rol r = new Rol();
      r.setNombre("ADMIN");
      r.setDescripcion("Administrador");
      return rolRepository.save(r);
    });
    Rol rolUser = rolRepository.findTopByNombreIgnoreCaseOrderByIdAsc("USER").orElseGet(() -> {
      Rol r = new Rol();
      r.setNombre("USER");
      r.setDescripcion("Usuario");
      return rolRepository.save(r);
    });

    if (admin == null) {
      admin = new Usuario();
      admin.setNombre("Administrador");
      admin.setApellidos("Easy4You");
      admin.setEmail("admin@easy4you.com");
      admin.setActivo(true);
      admin.setVerificado(true);
    }

    if (admin.getRoles() == null) {
      admin.setRoles(new HashSet<>());
    }
    admin.getRoles().add(rolAdmin);
    admin.getRoles().add(rolUser);

    admin.setActivo(true);
    admin.setVerificado(true);
    admin.setPasswordHash(passwordEncoder.encode("Admin1234!"));
    usuarioRepository.save(admin);

    redirectAttributes.addFlashAttribute("ok", "Contraseña del admin restablecida a: Admin1234!");
    return "redirect:/login";
  }
}
