package com.easy4you.controller.admin;

import com.easy4you.controller.admin.form.AdminAsignaturaCreateForm;
import com.easy4you.controller.admin.form.AdminUsuarioCreateForm;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import com.easy4you.service.AdminService;
import java.util.List;
import java.util.Set;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  @GetMapping
  public String dashboard(Model model) {
    model.addAttribute("nav", "dashboard");
    model.addAttribute("totalUsuarios", adminService.countUsuarios());
    model.addAttribute("totalAsignaturas", adminService.countAsignaturas());
    model.addAttribute("totalDocumentos", adminService.countDocumentos());
    return "admin/dashboard";
  }

  @GetMapping("/usuarios")
  public String usuarios(Model model) {
    model.addAttribute("nav", "usuarios");
    model.addAttribute("usuarios", adminService.listarUsuarios());
    if (!model.containsAttribute("usuarioCreateForm")) {
      model.addAttribute("usuarioCreateForm", new AdminUsuarioCreateForm());
    }
    return "admin/usuarios";
  }

  @GetMapping("/usuarios/{id}")
  public String usuarioDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    Usuario detalle = adminService.obtenerUsuario(id);
    if (detalle == null) {
      redirectAttributes.addFlashAttribute("error", "Usuario no encontrado: " + id);
      return "redirect:/admin/usuarios";
    }

    model.addAttribute("nav", "usuarios");
    model.addAttribute("usuarios", adminService.listarUsuarios());
    model.addAttribute("detalleUsuario", detalle);
    if (!model.containsAttribute("usuarioCreateForm")) {
      model.addAttribute("usuarioCreateForm", new AdminUsuarioCreateForm());
    }
    return "admin/usuarios";
  }

  @PostMapping("/usuarios/crear")
  public String crearUsuario(
      @Valid @ModelAttribute("usuarioCreateForm") AdminUsuarioCreateForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("nav", "usuarios");
      model.addAttribute("usuarios", adminService.listarUsuarios());
      return "admin/usuarios";
    }

    Usuario u = new Usuario();
    u.setNombre(form.getNombre().trim());
    u.setApellidos(form.getApellidos() == null ? null : form.getApellidos().trim());
    u.setEmail(form.getEmail().trim().toLowerCase());
    u.setActivo(form.getActivo() != null ? form.getActivo() : true);
    u.setVerificado(form.getVerificado() != null ? form.getVerificado() : false);

    try {
      adminService.crearUsuario(u, form.getPassword(), Set.of("USER"));
      redirectAttributes.addFlashAttribute("ok", "Usuario creado");
      return "redirect:/admin/usuarios";
    } catch (IllegalArgumentException ex) {
      bindingResult.rejectValue("email", "email.duplicado", ex.getMessage());
      model.addAttribute("nav", "usuarios");
      model.addAttribute("usuarios", adminService.listarUsuarios());
      return "admin/usuarios";
    }
  }

  @PostMapping("/usuarios/{id}/eliminar")
  public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      adminService.eliminarUsuario(id);
      redirectAttributes.addFlashAttribute("ok", "Usuario eliminado");
    } catch (EmptyResultDataAccessException ex) {
      redirectAttributes.addFlashAttribute("error", "Usuario no encontrado: " + id);
    } catch (DataIntegrityViolationException ex) {
      redirectAttributes.addFlashAttribute(
          "error", "No se puede eliminar el usuario: tiene datos asociados (asignaturas, documentos, etc.)");
    }
    return "redirect:/admin/usuarios";
  }

  @GetMapping("/asignaturas")
  public String asignaturas(Model model) {
    model.addAttribute("nav", "asignaturas");
    model.addAttribute("asignaturas", adminService.listarAsignaturas());
    model.addAttribute("usuarios", adminService.listarUsuarios());
    if (!model.containsAttribute("asignaturaCreateForm")) {
      model.addAttribute("asignaturaCreateForm", new AdminAsignaturaCreateForm());
    }
    return "admin/asignaturas";
  }

  @GetMapping("/asignaturas/{id}")
  public String asignaturaDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    Asignatura detalle = adminService.obtenerAsignatura(id);
    if (detalle == null) {
      redirectAttributes.addFlashAttribute("error", "Asignatura no encontrada: " + id);
      return "redirect:/admin/asignaturas";
    }

    List<Asignatura> asignaturas = adminService.listarAsignaturas();
    model.addAttribute("nav", "asignaturas");
    model.addAttribute("asignaturas", asignaturas);
    model.addAttribute("detalleAsignatura", detalle);
    model.addAttribute("usuarios", adminService.listarUsuarios());
    if (!model.containsAttribute("asignaturaCreateForm")) {
      model.addAttribute("asignaturaCreateForm", new AdminAsignaturaCreateForm());
    }
    return "admin/asignaturas";
  }

  @PostMapping("/asignaturas/crear")
  public String crearAsignatura(
      @Valid @ModelAttribute("asignaturaCreateForm") AdminAsignaturaCreateForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("nav", "asignaturas");
      model.addAttribute("asignaturas", adminService.listarAsignaturas());
      model.addAttribute("usuarios", adminService.listarUsuarios());
      return "admin/asignaturas";
    }

    Usuario owner = adminService.obtenerUsuario(form.getUsuarioId());
    if (owner == null) {
      bindingResult.rejectValue("usuarioId", "usuarioId.noexiste", "Usuario no válido");
      model.addAttribute("nav", "asignaturas");
      model.addAttribute("asignaturas", adminService.listarAsignaturas());
      model.addAttribute("usuarios", adminService.listarUsuarios());
      return "admin/asignaturas";
    }

    Asignatura a = new Asignatura();
    a.setUsuario(owner);
    a.setNombre(form.getNombre().trim());
    a.setDescripcion(form.getDescripcion() == null ? null : form.getDescripcion().trim());
    a.setColorHex(form.getColorHex() == null ? null : form.getColorHex().trim());
    a.setTrimestre(form.getTrimestre());

    adminService.crearAsignatura(a);
    redirectAttributes.addFlashAttribute("ok", "Asignatura creada");
    return "redirect:/admin/asignaturas";
  }
}
