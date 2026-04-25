package io.github.tessera;

/**
 * Unchecked exception thrown by Tessera for OCR errors.
 */
public class TesseraException extends RuntimeException {
    public TesseraException(String message) {
        super(message);
    }

    public TesseraException(String message, Throwable cause) {
        super(message, cause);
    }
}
