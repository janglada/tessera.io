# Tessera

Tessera is a clean, idiomatic Java library for Tesseract OCR, built using Java's Foreign Function & Memory (FFM) API (JEP 454).

## Prerequisites

- **Java 22+**: This library uses the FFM API, which became final in Java 22.
- **Tesseract OCR**: You must have Tesseract installed on your system.
  - Ubuntu: `sudo apt install libtesseract-dev tesseract-ocr-eng`
  - macOS: `brew install tesseract`
  - Windows: Install Tesseract via [UB Mannheim](https://github.com/UB-Mannheim/tesseract/wiki) and ensure the DLLs are in your PATH.

## Features

- No JNI or Tess4J dependencies.
- Native interop via `java.lang.foreign`.
- Fluent Builder API.
- Support for `Path`, `BufferedImage`, and raw pixel buffers.
- Automatic memory management using `Arena`.

## Usage

```java
import io.github.tessera.TesseractEngine;
import io.github.tessera.PageSegMode;
import io.github.tessera.OcrResult;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try (var engine = TesseractEngine.builder()
                .dataPath("/usr/share/tesseract-ocr/5/tessdata") // Optional
                .language("eng")
                .pageSegMode(PageSegMode.AUTO)
                .variable("tessedit_char_whitelist", "0123456789")
                .build()) {

            OcrResult result = engine.recognize(Path.of("receipt.png"));
            System.out.println("Text: " + result.text());
            System.out.println("Confidence: " + result.confidence());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Maven Setup

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.tessera</groupId>
    <artifactId>tessera</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Note: You may need to enable preview features if you are using a Java version where FFM is still in preview, or if you use other preview features.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

## License

MIT
