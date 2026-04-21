package com.easy4you.service.impl;

import com.easy4you.dto.contenido.FlashcardGeneradaDTO;
import com.easy4you.dto.contenido.PreguntaTestGeneradaDTO;
import com.easy4you.service.ContenidoService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ContenidoServiceImpl implements ContenidoService {

  private static final int MAX_RESUMEN_CARACTERES = 900;
  private static final int NUM_PREGUNTAS_TEST = 5;
  private static final int NUM_FLASHCARDS = 6;

  private static final Set<String> STOPWORDS =
      Set.of(
          "a",
          "al",
          "algo",
          "algunos",
          "ante",
          "antes",
          "como",
          "con",
          "contra",
          "cual",
          "cuando",
          "de",
          "del",
          "desde",
          "donde",
          "durante",
          "e",
          "el",
          "ella",
          "ellas",
          "ellos",
          "en",
          "entre",
          "era",
          "es",
          "esa",
          "ese",
          "eso",
          "esta",
          "este",
          "esto",
          "estos",
          "fue",
          "ha",
          "haber",
          "hace",
          "hacia",
          "han",
          "hasta",
          "hay",
          "la",
          "las",
          "le",
          "les",
          "lo",
          "los",
          "mas",
          "más",
          "me",
          "mi",
          "mis",
          "mismo",
          "mucho",
          "muy",
          "no",
          "o",
          "otra",
          "otro",
          "para",
          "pero",
          "por",
          "porque",
          "que",
          "quien",
          "se",
          "ser",
          "si",
          "sin",
          "sobre",
          "su",
          "sus",
          "tambien",
          "también",
          "te",
          "tener",
          "tiene",
          "toda",
          "todo",
          "todos",
          "tu",
          "tus",
          "un",
          "una",
          "uno",
          "unos",
          "y",
          "ya");

  @Override
  public String generarResumen(String texto) {
    String limpio = normalizar(texto);
    if (limpio.isBlank()) {
      return "";
    }

    List<String> frases = extraerFrases(limpio);
    String cuerpo =
        frases.isEmpty()
            ? recortar(limpio, MAX_RESUMEN_CARACTERES)
            : String.join(" ", frases.subList(0, Math.min(frases.size(), 4)));

    cuerpo = recortar(cuerpo, MAX_RESUMEN_CARACTERES);

    List<String> claves = extraerPalabrasClave(limpio, 6);
    if (claves.isEmpty()) {
      return cuerpo;
    }

    String ideas =
        claves.stream()
            .limit(6)
            .map(this::capitalizar)
            .map(k -> "• " + k)
            .collect(Collectors.joining("\n"));

    return "Ideas principales:\n" + ideas + "\n\nResumen:\n" + cuerpo;
  }

  @Override
  public List<PreguntaTestGeneradaDTO> generarPreguntasTest(String texto) {
    String limpio = normalizar(texto);
    List<String> frases = extraerFrases(limpio);
    List<String> claves = extraerPalabrasClave(limpio, 12);

    if (claves.isEmpty()) {
      claves = List.of("contenido", "tema", "concepto");
    }

    List<PreguntaTestGeneradaDTO> preguntas = new ArrayList<>();
    Random random = new Random();

    for (int i = 0; i < NUM_PREGUNTAS_TEST; i++) {
      String keyword = claves.get(i % claves.size());
      String enunciado = "¿Cuál es una afirmación correcta sobre \"" + capitalizar(keyword) + "\"?";

      String correcta = construirRespuesta(keyword, frases);

      LinkedHashSet<String> opciones = new LinkedHashSet<>();
      opciones.add(correcta);
      opciones.add("No aparece de forma relevante en el texto.");
      opciones.add("Se menciona solo como ejemplo y no como idea principal.");
      opciones.add("El texto no ofrece información suficiente para afirmarlo.");

      List<String> opcionesList = new ArrayList<>(opciones);
      while (opcionesList.size() > 4) {
        opcionesList.remove(opcionesList.size() - 1);
      }

      Collections.shuffle(opcionesList, random);
      int indiceCorrecto = opcionesList.indexOf(correcta);

      preguntas.add(new PreguntaTestGeneradaDTO(enunciado, opcionesList, indiceCorrecto));
    }

    return preguntas;
  }

  @Override
  public List<FlashcardGeneradaDTO> generarFlashcards(String texto) {
    String limpio = normalizar(texto);
    List<String> frases = extraerFrases(limpio);
    List<String> claves = extraerPalabrasClave(limpio, 10);

    if (claves.isEmpty()) {
      return List.of(new FlashcardGeneradaDTO("¿De qué trata el texto?", generarResumen(limpio)));
    }

    int n = Math.min(NUM_FLASHCARDS, claves.size());
    List<FlashcardGeneradaDTO> cards = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      String keyword = claves.get(i);
      String pregunta = "¿Qué es \"" + capitalizar(keyword) + "\"?";
      String respuesta = construirRespuesta(keyword, frases);
      cards.add(new FlashcardGeneradaDTO(pregunta, respuesta));
    }
    return cards;
  }

  private String construirRespuesta(String keyword, List<String> frases) {
    if (keyword == null || keyword.isBlank()) {
      return "Es un concepto relacionado con el tema principal del texto.";
    }

    String kw = keyword.toLowerCase(Locale.ROOT);
    for (String frase : frases) {
      if (frase.toLowerCase(Locale.ROOT).contains(kw)) {
        return recortar(frase, 160);
      }
    }
    return "Es un concepto relacionado con el tema principal del texto.";
  }

  private List<String> extraerFrases(String texto) {
    if (texto == null || texto.isBlank()) {
      return List.of();
    }

    String[] raw = texto.split("[\\.!?\\n]+");
    List<String> frases = new ArrayList<>();
    for (String f : raw) {
      String t = f.trim();
      if (t.length() >= 25) {
        frases.add(t);
      }
    }
    return frases;
  }

  private List<String> extraerPalabrasClave(String texto, int max) {
    if (texto == null || texto.isBlank()) {
      return List.of();
    }

    String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
    normalizado = normalizado.replaceAll("\\p{M}", "");
    normalizado = normalizado.toLowerCase(Locale.ROOT);
    normalizado = normalizado.replaceAll("[^\\p{L}\\p{N}\\s]", " ");

    String[] tokens = normalizado.split("\\s+");
    Map<String, Integer> freq = new HashMap<>();
    for (String token : tokens) {
      String t = token.trim();
      if (t.length() < 5) {
        continue;
      }
      if (STOPWORDS.contains(t)) {
        continue;
      }
      freq.put(t, freq.getOrDefault(t, 0) + 1);
    }

    return freq.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
        .limit(Math.max(max, 0))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  private String normalizar(String texto) {
    if (texto == null) {
      return "";
    }
    return texto.replaceAll("\\s+", " ").trim();
  }

  private String recortar(String texto, int max) {
    if (texto == null) {
      return "";
    }
    if (texto.length() <= max) {
      return texto;
    }
    return texto.substring(0, Math.max(0, max - 1)).trim() + "…";
  }

  private String capitalizar(String s) {
    if (s == null || s.isBlank()) {
      return "";
    }
    String t = s.trim();
    if (t.length() == 1) {
      return t.toUpperCase(Locale.ROOT);
    }
    return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
  }
}

