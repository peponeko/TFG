package com.easy4you.util;

public final class PromptTemplates {

  private PromptTemplates() {}

  public static final String RESUMEN =
      """
Resume SOLO el texto dado (sin inventar). Responde en español.
Formato:
- Idea principal:
- Puntos clave (máx 5):
- Conceptos importantes:

Texto:
{texto}
""";

  public static final String FLASHCARDS =
      """
Genera exactamente {n} flashcards basándote SOLO en el texto.
Devuelve SOLO un JSON array válido (sin markdown ni texto extra).
Ejemplo:
[{"pregunta":"...","respuesta":"...","dificultad":"BASICA|INTERMEDIA|AVANZADA"}]

Texto:
{texto}
""";

  public static final String PREGUNTAS_TEST =
      """
Genera exactamente {n} preguntas tipo test basándote SOLO en el texto.
Devuelve SOLO un JSON array válido (sin markdown ni texto extra).
Ejemplo:
[{"pregunta":"...","opciones":["a) ...","b) ...","c) ...","d) ..."],"respuestaCorrecta":"a","explicacion":"..."}]

Texto:
{texto}
""";

  public static final String CHAT =
      """
Responde basándote SOLO en los fragmentos. Responde en español.
Si no está en los fragmentos, di exactamente:
"No encuentro información sobre esto en los documentos proporcionados"
Incluye citas con el formato: [Doc: {nombre}, Fragmento {n}]

Fragmentos:
{chunks}

Pregunta:
{pregunta}
""";

  public static String formatResumen(String texto) {
    return RESUMEN.replace("{texto}", safe(texto));
  }

  public static String formatFlashcards(String texto, int n) {
    return FLASHCARDS.replace("{texto}", safe(texto)).replace("{n}", String.valueOf(n));
  }

  public static String formatPreguntasTest(String texto, int n) {
    return PREGUNTAS_TEST.replace("{texto}", safe(texto)).replace("{n}", String.valueOf(n));
  }

  public static String formatChat(String chunks, String pregunta) {
    return CHAT.replace("{chunks}", safe(chunks)).replace("{pregunta}", safe(pregunta));
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}
