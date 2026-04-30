package com.easy4you.controller.admin;

import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UsuarioRepository usuarioRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final DocumentoRepository documentoRepository;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public String dashboard(Model model) {
    model.addAttribute("nav", "dashboard");
    model.addAttribute("totalUsuarios", usuarioRepository.count());
    model.addAttribute("totalAsignaturas", asignaturaRepository.count());
    model.addAttribute("totalDocumentos", documentoRepository.count());
    return "admin/dashboard";
  }

  @GetMapping("/usuarios")
  public String usuarios(Model model) {
    model.addAttribute("nav", "usuarios");
    model.addAttribute("usuarios", usuarioRepository.findAll());
    return "admin/usuarios";
  }

  @GetMapping("/usuarios/{id}")
  public String usuarioDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    Usuario detalle = usuarioRepository.findById(id).orElse(null);
    if (detalle == null) {
      redirectAttributes.addFlashAttribute("error", "Usuario no encontrado: " + id);
      return "redirect:/admin/usuarios";
    }

    model.addAttribute("nav", "usuarios");
    model.addAttribute("usuarios", usuarioRepository.findAll());
    model.addAttribute("detalleUsuario", detalle);
    return "admin/usuarios";
  }

  @PostMapping("/usuarios/{id}/eliminar")
  public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      usuarioRepository.deleteById(id);
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
    model.addAttribute("asignaturas", asignaturaRepository.findAll());
    return "admin/asignaturas";
  }

  @GetMapping("/asignaturas/{id}")
  public String asignaturaDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    Asignatura detalle = asignaturaRepository.findById(id).orElse(null);
    if (detalle == null) {
      redirectAttributes.addFlashAttribute("error", "Asignatura no encontrada: " + id);
      return "redirect:/admin/asignaturas";
    }

    List<Asignatura> asignaturas = asignaturaRepository.findAll();
    model.addAttribute("nav", "asignaturas");
    model.addAttribute("asignaturas", asignaturas);
    model.addAttribute("detalleAsignatura", detalle);
    return "admin/asignaturas";
  }
}
