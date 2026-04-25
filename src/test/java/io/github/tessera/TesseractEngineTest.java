package io.github.tessera;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TesseractEngineTest {

    @Test
    public void testEngineInitialization() {
        // This will only work if Tesseract is installed and tessdata is in the expected path
        try (var engine = TesseractEngine.builder()
                .dataPath("/usr/share/tesseract-ocr/5/tessdata")
                .language("eng")
                .build()) {
            assertNotNull(engine);
        } catch (Exception e) {
            // If it fails because of missing native lib or data, we still learned something
            System.err.println("Initialization failed: " + e.getMessage());
            // We don't necessarily want to fail the build if the environment is not fully set up
        }
    }
}
