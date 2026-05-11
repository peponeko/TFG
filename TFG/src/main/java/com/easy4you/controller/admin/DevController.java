package com.easy4you.controller.admin;

import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    Usuario admin = usuarioRepository.findTopByEmailOrderByIdAsc("admin@easy4you.com").orElse(null);
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

  @GetMapping("/schema/tema")
  @ResponseBody
  @Transactional(readOnly = true)
  public Map<String, Object> schemaTema() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("database", resolveDatabaseName());
    out.put("table", "tema");

    List<Map<String, Object>> cols = new ArrayList<>();
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE "
                    + "FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tema' "
                    + "ORDER BY ORDINAL_POSITION")) {
      while (rs.next()) {
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("name", rs.getString("COLUMN_NAME"));
        col.put("nullable", rs.getString("IS_NULLABLE"));
        col.put("type", rs.getString("COLUMN_TYPE"));
        cols.add(col);
      }
    } catch (Exception ex) {
      out.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
    out.put("columns", cols);
    return out;
  }

  @GetMapping("/migrate/tema-schema")
  @ResponseBody
  @Transactional
  public Map<String, Object> migrateTemaSchema() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("database", resolveDatabaseName());
    out.put("table", "tema");

    List<String> actions = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    try (Connection c = dataSource.getConnection()) {
      boolean hasUnidadId = hasColumn(c, "tema", "unidad_id");
      boolean hasAsignaturaId = hasColumn(c, "tema", "asignatura_id");

      out.put("has_unidad_id", hasUnidadId);
      out.put("has_asignatura_id", hasAsignaturaId);

      if (!hasUnidadId && hasAsignaturaId) {
        actions.add("No hay nada que migrar (ya usa asignatura_id).");
        out.put("actions", actions);
        out.put("warnings", warnings);
        return out;
      }

      if (!hasAsignaturaId) {
        actions.add("Añadiendo columna asignatura_id (temporalmente NULL)...");
        try (Statement st = c.createStatement()) {
          st.execute("ALTER TABLE tema ADD COLUMN asignatura_id BIGINT NULL");
        }
        hasAsignaturaId = true;
      }

      if (hasUnidadId) {
        // Intentar rellenar asignatura_id a partir del modelo antiguo si existen tablas legacy.
        boolean hasUnidadTable = tableExists(c, "unidad");
        boolean hasResultadoAprendizajeTable = tableExists(c, "resultado_aprendizaje");
        if (hasUnidadTable && hasResultadoAprendizajeTable) {
          actions.add("Rellenando asignatura_id desde unidad -> resultado_aprendizaje...");
          try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "UPDATE tema t "
                    + "JOIN unidad u ON t.unidad_id = u.id "
                    + "JOIN resultado_aprendizaje ra ON u.resultado_aprendizaje_id = ra.id "
                    + "SET t.asignatura_id = ra.asignatura_id "
                    + "WHERE t.asignatura_id IS NULL");
          }
        } else {
          warnings.add(
              "No se han encontrado tablas legacy (unidad/resultado_aprendizaje). "
                  + "Si ya no existen, asegúrate de que tema.asignatura_id quede relleno antes de ponerlo NOT NULL.");
        }
      }

      long nullAsignatura = countNullAsignaturaId(c);
      out.put("null_asignatura_id", nullAsignatura);
      if (nullAsignatura > 0) {
        warnings.add(
            "Hay " + nullAsignatura + " filas con asignatura_id NULL. "
                + "No se puede poner NOT NULL todavía sin perder datos.");
      } else {
        actions.add("Marcando asignatura_id como NOT NULL...");
        try (Statement st = c.createStatement()) {
          st.execute("ALTER TABLE tema MODIFY COLUMN asignatura_id BIGINT NOT NULL");
        }
      }

      // Añadir FK (si no existe). Nombre fijo para simplificar: fk_tema_asignatura
      if (!foreignKeyExists(c, "tema", "fk_tema_asignatura")) {
        actions.add("Añadiendo FK tema(asignatura_id) -> asignatura(id)...");
        try (Statement st = c.createStatement()) {
          st.execute(
              "ALTER TABLE tema "
                  + "ADD CONSTRAINT fk_tema_asignatura FOREIGN KEY (asignatura_id) "
                  + "REFERENCES asignatura(id) ON DELETE CASCADE");
        }
      }

      if (hasUnidadId) {
        String fkUnidad = findForeignKeyNameForColumn(c, "tema", "unidad_id");
        if (fkUnidad != null) {
          actions.add("Eliminando FK legacy de unidad_id: " + fkUnidad);
          try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE tema DROP FOREIGN KEY " + fkUnidad);
          }
        }

        actions.add("Eliminando columna legacy unidad_id...");
        try (Statement st = c.createStatement()) {
          st.execute("ALTER TABLE tema DROP COLUMN unidad_id");
        }
      }

    } catch (Exception ex) {
      out.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }

    out.put("actions", actions);
    out.put("warnings", warnings);
    return out;
  }

  private boolean tableExists(Connection c, String tableName) {
    try (PreparedStatement ps =
            c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?");
        ) {
      ps.setString(1, tableName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getLong(1) > 0;
      }
    } catch (Exception ex) {
      return false;
    }
  }

  private boolean hasColumn(Connection c, String tableName, String columnName) {
    try (PreparedStatement ps =
            c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?");
        ) {
      ps.setString(1, tableName);
      ps.setString(2, columnName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getLong(1) > 0;
      }
    } catch (Exception ex) {
      return false;
    }
  }

  private long countNullAsignaturaId(Connection c) {
    try (Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tema WHERE asignatura_id IS NULL")) {
      return rs.next() ? rs.getLong(1) : -1;
    } catch (Exception ex) {
      return -1;
    }
  }

  private boolean foreignKeyExists(Connection c, String tableName, String constraintName) {
    try (PreparedStatement ps =
            c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                    + "AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = ?");
        ) {
      ps.setString(1, tableName);
      ps.setString(2, constraintName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getLong(1) > 0;
      }
    } catch (Exception ex) {
      return false;
    }
  }

  private String findForeignKeyNameForColumn(Connection c, String tableName, String columnName) {
    try (PreparedStatement ps =
            c.prepareStatement(
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? "
                    + "AND REFERENCED_TABLE_NAME IS NOT NULL "
                    + "LIMIT 1");
        ) {
      ps.setString(1, tableName);
      ps.setString(2, columnName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    } catch (Exception ex) {
      return null;
    }
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
    Usuario admin = usuarioRepository.findTopByEmailOrderByIdAsc("admin@easy4you.com").orElse(null);

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
