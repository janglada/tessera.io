package io.github.tessera;

import io.github.tessera.internal.TesseractBindings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * High-level API for Tesseract OCR.
 * This class is NOT thread-safe, but multiple instances can be used concurrently.
 */
public final class TesseractEngine implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment handle;

    private TesseractEngine(Builder builder) {
        this.arena = Arena.ofConfined();
        try {
            this.handle = (MemorySegment) TesseractBindings.TessBaseAPICreate.invokeExact();
            if (this.handle.equals(MemorySegment.NULL)) {
                throw new TesseraException("Failed to create Tesseract API handle");
            }

            MemorySegment dataPath = builder.dataPath != null ? arena.allocateFrom(builder.dataPath) : MemorySegment.NULL;
            MemorySegment language = builder.language != null ? arena.allocateFrom(builder.language) : MemorySegment.NULL;

            int res = (int) TesseractBindings.TessBaseAPIInit3.invokeExact(handle, dataPath, language, 3); // 3 = OEM_DEFAULT
            if (res != 0) {
                throw new TesseraException("Failed to initialize Tesseract with dataPath=" + builder.dataPath + ", language=" + builder.language);
            }

            if (builder.pageSegMode != null) {
                TesseractBindings.TessBaseAPISetPageSegMode.invokeExact(handle, builder.pageSegMode.getValue());
            }

            for (var entry : builder.variables.entrySet()) {
                MemorySegment name = arena.allocateFrom(entry.getKey());
                MemorySegment value = arena.allocateFrom(entry.getValue());
                TesseractBindings.TessBaseAPISetVariable.invokeExact(handle, name, value);
            }
        } catch (Throwable t) {
            arena.close();
            if (t instanceof TesseraException te) throw te;
            throw new TesseraException("Error initializing Tesseract engine", t);
        }
    }

    /**
     * Recognizes text in the given image file.
     *
     * @param imagePath path to the image file
     * @return the OCR result
     * @throws IOException if the file cannot be read
     */
    public OcrResult recognize(Path imagePath) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("Failed to decode image: " + imagePath);
        }
        return recognize(image);
    }

    /**
     * Recognizes text in the given BufferedImage.
     *
     * @param image the image to process
     * @return the OCR result
     */
    public OcrResult recognize(BufferedImage image) {
        // Convert to a format Tesseract likes: 3-byte BGR or 1-byte Gray
        int type = image.getType();
        BufferedImage processed = image;
        int bytesPerPixel;
        if (type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_BYTE_GRAY) {
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

            // Get hOCR optionally (let's just get it)
            MemorySegment hocrPtr = (MemorySegment) TesseractBindings.TessBaseAPIGetHOCRText.invokeExact(handle, MemorySegment.NULL, 0);
            String hocr = hocrPtr.reinterpret(Long.MAX_VALUE).getString(0);
            TesseractBindings.TessDeleteText.invokeExact(hocrPtr);

            return new OcrResult(text, confidence, hocr);
        } catch (Throwable t) {
            if (t instanceof TesseraException te) throw te;
            throw new TesseraException("Error during OCR process", t);
        }
    }

    @Override
    public void close() {
        try {
            TesseractBindings.TessBaseAPIDelete.invokeExact(handle);
        } catch (Throwable t) {
            // Log or ignore
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
