package io.github.tessera;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TesseractEngineTest {

    private final String datapath = "/usr/share/tesseract-ocr/5/tessdata";
    private final String testResourcesDataPath = "src/test/resources/test-data";
    private final String language = "eng";
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

    @Test
    public void testSetPageSegMode() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .pageSegMode(PageSegMode.SINGLE_LINE)
                .build()) {
            File imageFile = new File(testResourcesDataPath, "eurotext.png");
            OcrResult result = engine.recognize(ImageIO.read(imageFile));
            assertNotNull(result);
            // In SINGLE_LINE mode on this multi-line image, results might be poor or different, 
            // but we just want to verify it doesn't crash and returns something.
            assertFalse(result.text().isEmpty(), "OCR text should not be empty even in SINGLE_LINE mode");
        }
    }

    @Test
    public void testDoOCR_BMP() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .build()) {
            Path imagePath = Path.of(testResourcesDataPath, "eurotext.bmp");
            OcrResult result = engine.recognize(imagePath);
            assertNotNull(result);
            assertTrue(result.text().contains("The (quick) [brown] {fox} jumps!"), "OCR text should contain expected string from BMP");
        }
    }

    @Test
    public void testDoOCR_TIFF() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .build()) {
            Path imagePath = Path.of(testResourcesDataPath, "eurotext.tif");
            // Note: TIFF support in ImageIO depends on the environment (e.g., jai-imageio)
            try {
                OcrResult result = engine.recognize(imagePath);
                assertNotNull(result);
                assertTrue(result.text().contains("The (quick) [brown] {fox} jumps!"), "OCR text should contain expected string from TIFF");
            } catch (IOException e) {
                if (e.getMessage().contains("Failed to decode image")) {
                    System.err.println("Skipping TIFF test: ImageIO could not decode TIFF. Ensure jai-imageio is available.");
                } else {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testDoOCR_PDF() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language(language)
                .build()) {
            Path imagePath = Path.of(testResourcesDataPath, "multipage-img.pdf");
            OcrResult result = engine.recognize(imagePath);
            assertNotNull(result);
            assertFalse(result.text().isEmpty(), "OCR text should not be empty from PDF");
        }
    }

    @Test
    public void testDoOCR_MultipagePDF() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language(language)
                .build()) {
            Path imagePath = Path.of(testResourcesDataPath, "multipage-img.pdf");
            List<OcrResult> results = engine.recognizePages(imagePath);
            assertNotNull(results);
            assertTrue(results.size() > 1, "Should recognize multiple pages from multipage PDF");
            for (OcrResult result : results) {
                assertFalse(result.text().isEmpty(), "Each page should have some text");
            }
        }
    }

    @Test
    public void testDoOCR_RawPixels() throws Exception {
        try (var engine = TesseractEngine.builder()
                .dataPath(datapath)
                .language("eng")
                .build()) {
            File imageFile = new File(testResourcesDataPath, "eurotext.png");
            BufferedImage image = ImageIO.read(imageFile);
            
            // Convert to 3-byte BGR if not already
            BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            var g = bgrImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            byte[] pixels = ((java.awt.image.DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();
            OcrResult result = engine.recognize(pixels, bgrImage.getWidth(), bgrImage.getHeight(), 3, 3 * bgrImage.getWidth());
            
            assertNotNull(result);
            assertTrue(result.text().contains("The (quick) [brown] {fox} jumps!"), "OCR text should contain expected string from raw pixels");
        }
    }
}
