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

  /**
   * Para Flashcards se usa JSON Mode de Gemini, por lo que el modelo devolverá
   * JSON puro sin markdown ni texto extra. El esquema debe coincidir exactamente
   * con FlashcardResponseDTO en la app Android.
   */
  public static final String FLASHCARDS =
      """
Eres un experto en educación. Genera exactamente {n} flashcards basándote SOLO en el texto proporcionado, sin inventar información.

Responde exclusivamente en formato JSON siguiendo este esquema (array JSON, sin texto adicional, sin markdown):
[{"pregunta":"...","respuesta":"...","dificultad":"BASICA|INTERMEDIA|AVANZADA"}]

Reglas:
- El campo "dificultad" debe ser exactamente uno de: BASICA, INTERMEDIA, AVANZADA
- Las preguntas deben ser claras y concisas
- Las respuestas deben ser completas pero breves
- Basa todo en el texto proporcionado, no inventes datos

Texto:
{texto}
""";

  /**
   * Para Preguntas Test se usa JSON Mode de Gemini. El esquema debe coincidir
   * exactamente con PreguntaTestResponseDTO en la app Android.
   * "respuestaCorrecta" debe ser "a", "b", "c" o "d" (letra minúscula).
   */
  public static final String PREGUNTAS_TEST =
      """
Eres un experto en evaluación educativa. Genera exactamente {n} preguntas tipo test basándote SOLO en el texto proporcionado, sin inventar información.

Responde exclusivamente en formato JSON siguiendo este esquema (array JSON, sin texto adicional, sin markdown):
[{"pregunta":"...","opciones":["a) ...","b) ...","c) ...","d) ..."],"respuestaCorrecta":"a","explicacion":"..."}]

Reglas:
- "opciones" debe tener exactamente 4 elementos con prefijos "a) ", "b) ", "c) ", "d) "
- "respuestaCorrecta" debe ser exactamente una de estas letras: a, b, c, d
- Todas las opciones deben ser plausibles para evitar respuestas obvias
- "explicacion" debe justificar por qué la respuesta correcta es correcta
- Basa todo en el texto proporcionado, no inventes datos

Texto:
{texto}
""";

  public static final String CHAT =
      """
Eres un asistente de estudio. Responde basándote SOLO en los fragmentos de apuntes proporcionados. Responde en español.
Si la información no está en los fragmentos, di exactamente:
"No encuentro información sobre esto en los documentos proporcionados"
Incluye citas con el formato: [Doc: {nombre}, Fragmento {n}]

Fragmentos de los apuntes:
{chunks}

Pregunta del alumno:
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
