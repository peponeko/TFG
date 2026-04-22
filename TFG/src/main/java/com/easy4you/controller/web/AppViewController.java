package com.easy4you.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppViewController {

  @GetMapping
  public String index() {
    return "redirect:/app/home";
  }

  @GetMapping("/login")
  public String login() {
    return "app/login";
  }

  @GetMapping("/notebooks")
  public String notebooks() {
    return "notebook/index";
  }

  @GetMapping("/home")
  public String home() {
    return "home/index";
  }

  @GetMapping("/home/{asignaturaId}/trimestres")
  public String trimestres(@PathVariable Long asignaturaId, Model model) {
    model.addAttribute("asignaturaId", asignaturaId);
    return "home/trimestres";
  }

  @GetMapping("/home/{asignaturaId}/trimestre/{trimestre}/temas")
  public String temas(@PathVariable Long asignaturaId, @PathVariable Integer trimestre, Model model) {
    model.addAttribute("asignaturaId", asignaturaId);
    model.addAttribute("trimestre", trimestre);
    return "home/temas";
  }

  @GetMapping("/notebooks/{id}")
  public String notebookDetalle(@PathVariable Long id, Model model) {
    model.addAttribute("notebookId", id);
    return "notebook/detalle";
  }

  @GetMapping("/notebooks/{id}/fuentes")
  public String notebookFuentes(@PathVariable Long id, Model model) {
    model.addAttribute("notebookId", id);
    return "notebook/fuentes";
  }

  @GetMapping("/chat")
  public String chat() {
    return "chat/index";
  }

  @GetMapping("/estudio/flashcards")
  public String flashcards() {
    return "estudio/flashcards";
  }

  @GetMapping("/estudio/test")
  public String test() {
    return "estudio/test";
  }

  @GetMapping("/notas")
  public String notas() {
    return "notas/index";
  }
}
