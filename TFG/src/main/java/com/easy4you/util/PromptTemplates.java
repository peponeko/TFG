package com.easy4you.util;

public final class PromptTemplates {

  private PromptTemplates() {}

  public static final String RESUMEN =
      """
Eres un asistente académico experto. Basándote ÚNICAMENTE en el siguiente texto,
genera un resumen estructurado con: 1) Idea principal, 2) Puntos clave (máximo 5),
3) Conceptos importantes. No añadas información externa.

TEXTO:
{texto}

Responde en español. Sé conciso y preciso.
""";

  public static final String FLASHCARDS =
      """
Basándote ÚNICAMENTE en el siguiente texto académico, genera exactamente {n} flashcards
de estudio. Cada flashcard debe tener una pregunta clara y una respuesta concisa.
Devuelve SOLO un JSON array con el formato:
[{"pregunta": "...", "respuesta": "...", "dificultad": "BASICA|INTERMEDIA|AVANZADA"}]

TEXTO:
{texto}
""";

  public static final String PREGUNTAS_TEST =
      """
Basándote ÚNICAMENTE en el siguiente texto académico, genera exactamente {n} preguntas
de test de opción múltiple. Devuelve SOLO un JSON array con el formato:
[{"pregunta": "...", "opciones": ["a)...", "b)...", "c)...", "d)..."],
  "respuestaCorrecta": "a", "explicacion": "..."}]

TEXTO:
{texto}
""";

  public static final String CHAT =
      """
Eres un asistente de estudio académico. Tu función es responder preguntas
basándote EXCLUSIVAMENTE en los fragmentos de documentos proporcionados.

REGLAS ESTRICTAS:
- Responde SOLO con información de los fragmentos dados
- Si la respuesta no está en los fragmentos, di: "No encuentro información sobre esto en los documentos proporcionados"
- Cita siempre el documento y fragmento de origen usando el formato [Doc: {nombre}, Fragmento {n}]
- No inventes ni añadas información externa
- Responde en español

FRAGMENTOS DE DOCUMENTOS:
{chunks}

PREGUNTA:
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

