package br.com.estagio.anonymizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldShowUsageWhenArgumentCountIsInvalid() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[] {"only-input"}, emptyPrintStream(), printStream(err));

        assertEquals(1, exitCode);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Uso:"));
    }

    @Test
    void shouldProcessFolderWhenArgumentsAreValid() throws IOException {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.writeString(input.resolve("page.html"), "<td>550000000000</td>", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[] {input.toString(), output.toString()},
                printStream(out),
                printStream(err)
        );

        assertEquals(0, exitCode);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Arquivos processados: 1"));
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("page.html")));
        assertFalse(Files.readString(output.resolve("input_anonimizado").resolve("page.html"), StandardCharsets.UTF_8).contains("550000000000"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldProcessSingleHtmlFileWhenArgumentsAreValid() throws IOException {
        Path input = tempDir.resolve("page.html");
        Path output = tempDir.resolve("output");
        Files.writeString(input, "<td>550000000000</td>", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[] {input.toString(), output.toString()},
                printStream(out),
                emptyPrintStream()
        );

        assertEquals(0, exitCode);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Arquivos processados: 1"));
        assertTrue(Files.exists(output.resolve("page.html")));
        assertFalse(Files.readString(output.resolve("page.html"), StandardCharsets.UTF_8).contains("550000000000"));
    }

    @Test
    void shouldReturnErrorWhenProcessingFails() {
        Path input = tempDir.resolve("missing");
        Path output = tempDir.resolve("output");
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[] {input.toString(), output.toString()},
                emptyPrintStream(),
                printStream(err)
        );

        assertEquals(2, exitCode);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Erro ao processar entrada:"));
    }

    private static PrintStream printStream(ByteArrayOutputStream outputStream) {
        return new PrintStream(outputStream, true, StandardCharsets.UTF_8);
    }

    private static PrintStream emptyPrintStream() {
        return printStream(new ByteArrayOutputStream());
    }
}
