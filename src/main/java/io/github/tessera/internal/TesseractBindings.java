package io.github.tessera.internal;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.*;

/**
 * Low-level FFM bindings for Tesseract C API.
 */
public final class TesseractBindings {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    static {
        SymbolLookup lookup;
        try {
            lookup = SymbolLookup.libraryLookup("tesseract", Arena.global());
        } catch (IllegalArgumentException e) {
            try {
                // Try with versioned name on linux
                lookup = SymbolLookup.libraryLookup("tesseract.so.5", Arena.global());
            } catch (IllegalArgumentException e2) {
                // Fallback to absolute path
                String libPath = "/usr/lib/x86_64-linux-gnu/libtesseract.so.5";
                lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
            }
        }
        LOOKUP = lookup;
    }

    public static final MethodHandle TessBaseAPICreate = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPICreate").get(),
            FunctionDescriptor.of(ADDRESS)
    );

    public static final MethodHandle TessBaseAPIDelete = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIDelete").get(),
            FunctionDescriptor.ofVoid(ADDRESS)
    );

    public static final MethodHandle TessBaseAPIInit3 = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIInit3").get(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
    );

    public static final MethodHandle TessBaseAPISetImage = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPISetImage").get(),
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    );

    public static final MethodHandle TessBaseAPISetVariable = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPISetVariable").get(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
    );

    public static final MethodHandle TessBaseAPISetPageSegMode = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPISetPageSegMode").get(),
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)
    );

    public static final MethodHandle TessBaseAPIRecognize = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIRecognize").get(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    );

    public static final MethodHandle TessBaseAPIGetUTF8Text = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIGetUTF8Text").get(),
            FunctionDescriptor.of(ADDRESS, ADDRESS)
    );

    public static final MethodHandle TessBaseAPIGetHOCRText = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIGetHOCRText").get(),
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT)
    );

    public static final MethodHandle TessBaseAPIMeanTextConf = LINKER.downcallHandle(
            LOOKUP.find("TessBaseAPIMeanTextConf").get(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );

    public static final MethodHandle TessDeleteText = LINKER.downcallHandle(
            LOOKUP.find("TessDeleteText").get(),
            FunctionDescriptor.ofVoid(ADDRESS)
    );

    private TesseractBindings() {
    }
}
