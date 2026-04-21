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
    return "redirect:/app/notebooks";
  }

  @GetMapping("/login")
  public String login() {
    return "app/login";
  }

  @GetMapping("/notebooks")
  public String notebooks() {
    return "notebook/index";
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
