# Tessera Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a Java library for Tesseract OCR using Java's FFM API.

**Architecture:** A low-level `TesseractBindings` class uses `Linker` and `MethodHandle` to call C functions. A high-level `TesseractEngine` provides a fluent API, managing memory via `Arena`.

**Tech Stack:** Java 22, Maven, Tesseract C API.

---

### Task 1: Project Setup

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/module-info.java`

- [ ] **Step 1: Create pom.xml**
  - Use Java 22
  - Enable preview features if necessary (though FFM is final in 22)
  - Group ID: io.github.tessera
  - Artifact ID: tessera

- [ ] **Step 2: Create module-info.java**
  - Name: `io.github.tessera`
  - Exports: `io.github.tessera`
  - Requires: `java.desktop` (for BufferedImage)

### Task 2: Domain Models

**Files:**
- Create: `src/main/java/io/github/tessera/TesseraException.java`
- Create: `src/main/java/io/github/tessera/PageSegMode.java`
- Create: `src/main/java/io/github/tessera/OcrResult.java`

- [ ] **Step 1: Create TesseraException**
  - Unchecked exception

- [ ] **Step 2: Create PageSegMode**
  - Enum mapping to Tesseract constants (0 to 13)

- [ ] **Step 3: Create OcrResult**
  - Immutable record with text, confidence, and hOCR

### Task 3: Low-level Bindings

**Files:**
- Create: `src/main/java/io/github/tessera/internal/TesseractBindings.java`

- [ ] **Step 1: Implement TesseractBindings**
  - Use `Linker.nativeLinker()`
  - Use `SymbolLookup` to find tesseract library
  - Define `MethodHandle` for all required functions
  - Use `FunctionDescriptor` for signatures

### Task 4: High-level API

**Files:**
- Create: `src/main/java/io/github/tessera/TesseractEngine.java`

- [ ] **Step 1: Implement TesseractEngine**
  - Builder pattern
  - `AutoCloseable` implementation
  - `recognize(Path)`, `recognize(BufferedImage)`, `recognize(byte[])`
  - Manage `Arena` and native pointers

### Task 5: Documentation & Verification

**Files:**
- Create: `README.md`
- Create: `src/test/java/io/github/tessera/TesseractEngineTest.java` (Optional/Manual verification)

- [ ] **Step 1: Create README.md**
  - Usage examples
  - Requirements (Java 22, Tesseract)

- [ ] **Step 2: Basic compilation check**
  - `mvn compile`
