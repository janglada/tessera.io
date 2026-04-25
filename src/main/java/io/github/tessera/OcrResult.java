package io.github.tessera;

/**
 * Immutable record holding recognized text and metadata.
 *
 * @param text       the recognized UTF-8 text
 * @param confidence the mean confidence score (0-100)
 * @param hocr       optionally the hOCR output
 */
public record OcrResult(String text, float confidence, String hocr) {
}
