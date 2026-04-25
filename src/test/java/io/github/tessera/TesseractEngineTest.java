package io.github.tessera;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TesseractEngineTest {

    private final String datapath = "/usr/share/tesseract-ocr/5/tessdata";
    private final String testResourcesDataPath = "src/test/resources/test-data";
    private final String expOCRResult = "The (quick) [brown] {fox} jumps!\nOver the $43,456.78 <lazy> #90 dog";

    @Test
    public void testEngineInitialization() {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .build()) {
            assertNotNull(engine);
        } catch (Exception e) {
            fail("Initialization failed: " + e.getMessage());
        }
    }

    @Test
    public void testDoOCR_File() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .pageSegMode(PageSegMode.AUTO)
                .build()) {
            Path imagePath = Path.of(testResourcesDataPath, "eurotext.png");
            OcrResult result = engine.recognize(imagePath);
            assertNotNull(result);
            assertTrue(result.text().contains("The (quick) [brown] {fox} jumps!"), "OCR text should contain expected string");
            assertTrue(result.confidence() > 0, "Confidence should be greater than 0");
        }
    }

    @Test
    public void testDoOCR_BufferedImage() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .build()) {
            File imageFile = new File(testResourcesDataPath, "eurotext.png");
            BufferedImage image = ImageIO.read(imageFile);
            OcrResult result = engine.recognize(image);
            assertNotNull(result);
            assertTrue(result.text().contains("The (quick) [brown] {fox} jumps!"), "OCR text should contain expected string");
        }
    }

    @Test
    public void testSetVariable() throws Exception {
        // Test whitelist variable
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .variable("tessedit_char_whitelist", "0123456789")
                .build()) {
            File imageFile = new File(testResourcesDataPath, "eurotext.png");
            OcrResult result = engine.recognize(ImageIO.read(imageFile));
            // With whitelist 0-9, it shouldn't contain "The"
            assertFalse(result.text().contains("The"), "OCR text should not contain 'The' due to whitelist");
        }
    }
}
