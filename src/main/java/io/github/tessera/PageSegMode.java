package io.github.tessera;

/**
 * Possible modes for page segmentation.
 */
public enum PageSegMode {
    /** Orientation and script detection (OSD) only. */
    OSD_ONLY(0),
    /** Automatic page segmentation with OSD. */
    AUTO_OSD(1),
    /** Automatic page segmentation, but no OSD, or OCR. */
    AUTO_ONLY(2),
    /** Fully automatic page segmentation, but no OSD. */
    AUTO(3),
    /** Assume a single column of text of variable sizes. */
    SINGLE_COLUMN(4),
    /** Assume a single uniform block of vertically aligned text. */
    SINGLE_BLOCK_VERT_TEXT(5),
    /** Assume a single uniform block of text. */
    SINGLE_BLOCK(6),
    /** Treat the image as a single text line. */
    SINGLE_LINE(7),
    /** Treat the image as a single word. */
    SINGLE_WORD(8),
    /** Treat the image as a single word in a circle. */
    CIRCLE_WORD(9),
    /** Treat the image as a single character. */
    SINGLE_CHAR(10),
    /** Find as much text as possible in no particular order. */
    SPARSE_TEXT(11),
    /** Sparse text with orientation and script detection. */
    SPARSE_TEXT_OSD(12),
    /** Treat the image as a single text line, bypassing hacks that are Tesseract-specific. */
    RAW_LINE(13);

    private final int value;

    PageSegMode(int value) {
        this.value = value;
    }

    /**
     * @return the native Tesseract constant value.
     */
    public int getValue() {
        return value;
    }
}
