package com.easy4you.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

  private String provider = "ollama";

  private int maxTokensResumen = 1000;
  private int maxTokensChat = 800;
  private int maxTokensFlashcards = 1500;
  private int maxContextChunks = 5;

  private final Ollama ollama = new Ollama();
  private final Gemini gemini = new Gemini();
  private final Groq groq = new Groq();

  @Getter
  @Setter
  public static class Ollama {
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3.1:8b";
  }

  @Getter
  @Setter
  public static class Gemini {
    private String apiKey = "";
    private String model = "gemini-1.5-flash";
  }

  @Getter
  @Setter
  public static class Groq {
    private String apiKey = "";
    private String model = "llama3-8b-8192";
  }
}

