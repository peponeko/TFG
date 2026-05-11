package com.easy4you.util;

public final class PromptTemplates {

  private PromptTemplates() {}

  public static final String RESUMEN =
      """
Resume SOLO el texto dado (sin inventar). Responde en español.

Requisitos de calidad:
- Debe ser un resumen útil para estudiar (no una lista superficial).
- Longitud objetivo: 350 a 700 palabras.
- Incluye 1 ejemplo breve si el texto lo permite.

Formato:
- Idea principal (1-2 frases):
- Resumen explicado (2-4 párrafos):
- Puntos clave (5-8):
- Conceptos importantes (6-12):
- Mini-glosario (3-6 términos con definición de 1 línea):

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
Eres un asistente de estudio. Responde basándote SOLO en el texto extraído de los documentos proporcionados. Responde en español.

Reglas:
- Si el usuario saluda (ej: "hola", "buenas") responde con una bienvenida breve y 3 ejemplos de preguntas útiles.
- Si el usuario pide una visión general (ej: "¿de qué va el PDF?", "cosas más importantes", "resumen general"), responde con un resumen y puntos clave basados en el texto.
- Solo si el usuario hace una pregunta específica y NO encuentras la información en el texto, di exactamente:
"No encuentro información sobre esto en los documentos proporcionados"
Incluye citas con el formato: [Doc: {nombre}]

Texto extraído de los documentos:
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
