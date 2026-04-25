package io.github.tessera;

import io.github.tessera.internal.TesseractBindings;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level API for Tesseract OCR.
 * This class provides a fluent, idiomatic Java interface to the Tesseract native engine
 * using the Foreign Function &amp; Memory (FFM) API.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (var engine = TesseractEngine.builder()
 *         .language("eng")
 *         .build()) {
 *     OcrResult result = engine.recognize(Path.of("image.png"));
 *     System.out.println(result.text());
 * }
 * }</pre>
 *
 * <p>Note: This class is NOT thread-safe. Each thread should create its own instance,
 * as the underlying Tesseract C API handle is not re-entrant.</p>
 */
public final class TesseractEngine implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TesseractEngine.class);

    private final Arena arena;
    private final MemorySegment handle;

    private TesseractEngine(Builder builder) {
        this.arena = Arena.ofConfined();
        try {
            this.handle = (MemorySegment) TesseractBindings.TessBaseAPICreate.invokeExact();
            if (this.handle.equals(MemorySegment.NULL)) {
                throw new TesseraException("Failed to create Tesseract API handle");
            }
            logger.debug("Tesseract API handle created: {}", handle);

            MemorySegment dataPath = builder.dataPath != null ? arena.allocateFrom(builder.dataPath) : MemorySegment.NULL;
            MemorySegment language = builder.language != null ? arena.allocateFrom(builder.language) : MemorySegment.NULL;

            logger.info("Initializing Tesseract with dataPath={}, language={}", builder.dataPath, builder.language);
            int res = (int) TesseractBindings.TessBaseAPIInit3.invokeExact(handle, dataPath, language);
            if (res != 0) {
                throw new TesseraException("Failed to initialize Tesseract with dataPath=" + builder.dataPath + ", language=" + builder.language);
            }

            if (builder.pageSegMode != null) {
                logger.debug("Setting PageSegMode: {}", builder.pageSegMode);
                TesseractBindings.TessBaseAPISetPageSegMode.invokeExact(handle, builder.pageSegMode.getValue());
            }

            for (var entry : builder.variables.entrySet()) {
                logger.debug("Setting Tesseract variable: {}={}", entry.getKey(), entry.getValue());
                MemorySegment name = arena.allocateFrom(entry.getKey());
                MemorySegment value = arena.allocateFrom(entry.getValue());
                int success = (int) TesseractBindings.TessBaseAPISetVariable.invokeExact(handle, name, value);
                if (success == 0) {
                    logger.warn("Failed to set Tesseract variable: {}", entry.getKey());
                }
            }
        } catch (Throwable t) {
            arena.close();
            logger.error("Error during Tesseract initialization", t);
            if (t instanceof TesseraException te) throw te;
            throw new TesseraException("Error initializing Tesseract engine", t);
        }
    }

    /**
     * Recognizes text in the given image or PDF file.
     * If it's a multi-page file (like PDF), only the first page is returned.
     * Use {@link #recognizePages(Path)} for multi-page support.
     *
     * @param imagePath path to the image or PDF file
     * @return the OCR result for the first page
     * @throws IOException if the file cannot be read
     */
    public OcrResult recognize(Path imagePath) throws IOException {
        logger.info("Recognizing file: {}", imagePath);
        String fileName = imagePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            List<OcrResult> results = recognizePdf(imagePath);
            return results.isEmpty() ? new OcrResult("", 0, "") : results.get(0);
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("Failed to decode image: " + imagePath);
        }
        return recognize(image);
    }

    /**
     * Recognizes all pages in the given file (supports PDF).
     *
     * @param path path to the file
     * @return a list of OCR results, one per page
     * @throws IOException if the file cannot be read
     */
    public List<OcrResult> recognizePages(Path path) throws IOException {
        logger.info("Recognizing all pages in file: {}", path);
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return recognizePdf(path);
        } else {
            return List.of(recognize(path));
        }
    }

    private List<OcrResult> recognizePdf(Path pdfPath) throws IOException {
        logger.info("Processing PDF: {}", pdfPath);
        List<OcrResult> results = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            logger.debug("PDF has {} pages", pageCount);
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < pageCount; i++) {
                logger.debug("Rendering PDF page {}", i + 1);
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                results.add(recognize(image));
            }
        }
        return results;
    }

    /**
     * Recognizes text in the given BufferedImage.
     *
     * @param image the image to process
     * @return the OCR result
     */
    public OcrResult recognize(BufferedImage image) {
        int type = image.getType();
        BufferedImage processed = image;
        int bytesPerPixel;
        if (type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_BYTE_GRAY) {
            logger.debug("Converting BufferedImage from type {} to TYPE_3BYTE_BGR", type);
            processed = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            var g = processed.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            bytesPerPixel = 3;
        } else {
            bytesPerPixel = (type == BufferedImage.TYPE_BYTE_GRAY) ? 1 : 3;
        }

        byte[] pixels = ((DataBufferByte) processed.getRaster().getDataBuffer()).getData();
        return recognize(pixels, processed.getWidth(), processed.getHeight(), bytesPerPixel, bytesPerPixel * processed.getWidth());
    }

    /**
     * Recognizes text from raw pixel data.
     *
     * @param pixels         raw pixel bytes
     * @param width          image width
     * @param height         image height
     * @param bytesPerPixel  bytes per pixel (e.g. 1 for gray, 3 for BGR)
     * @param bytesPerLine   bytes per scanline (usually width * bytesPerPixel)
     * @return the OCR result
     */
    public OcrResult recognize(byte[] pixels, int width, int height, int bytesPerPixel, int bytesPerLine) {
        logger.debug("Recognizing raw pixels: {}x{}, {} bpp", width, height, bytesPerPixel);
        try (Arena sessionArena = Arena.ofConfined()) {
            MemorySegment pixelSegment = sessionArena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, pixels);
            TesseractBindings.TessBaseAPISetImage.invokeExact(handle, pixelSegment, width, height, bytesPerPixel, bytesPerLine);

            int res = (int) TesseractBindings.TessBaseAPIRecognize.invokeExact(handle, MemorySegment.NULL);
            if (res != 0) {
                throw new TesseraException("Error during recognition");
            }

            MemorySegment textPtr = (MemorySegment) TesseractBindings.TessBaseAPIGetUTF8Text.invokeExact(handle);
            String text = textPtr.reinterpret(Long.MAX_VALUE).getString(0);
            TesseractBindings.TessDeleteText.invokeExact(textPtr);

            int confidence = (int) TesseractBindings.TessBaseAPIMeanTextConf.invokeExact(handle);
            logger.debug("Recognition complete, confidence: {}", confidence);

            // Get hOCR optionally (let's just get it)
            MemorySegment hocrPtr = (MemorySegment) TesseractBindings.TessBaseAPIGetHOCRText.invokeExact(handle, 0);
            String hocr = hocrPtr.reinterpret(Long.MAX_VALUE).getString(0);
            TesseractBindings.TessDeleteText.invokeExact(hocrPtr);

            return new OcrResult(text, confidence, hocr);
        } catch (Throwable t) {
            logger.error("Error during OCR recognition process", t);
            if (t instanceof TesseraException te) throw te;
            throw new TesseraException("Error during OCR process", t);
        }
    }

    @Override
    public void close() {
        logger.info("Closing Tesseract engine");
        try {
            TesseractBindings.TessBaseAPIDelete.invokeExact(handle);
        } catch (Throwable t) {
            logger.error("Error deleting Tesseract API handle", t);
        } finally {
            arena.close();
        }
    }

    /**
     * @return a new builder for TesseractEngine.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataPath;
        private String language = "eng";
        private PageSegMode pageSegMode;
        private final Map<String, String> variables = new HashMap<>();

        public Builder dataPath(String dataPath) {
            this.dataPath = dataPath;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder pageSegMode(PageSegMode mode) {
            this.pageSegMode = mode;
            return this;
        }

        public Builder variable(String name, String value) {
            variables.put(name, value);
            return this;
        }

        public TesseractEngine build() {
            return new TesseractEngine(this);
        }
    }
}
