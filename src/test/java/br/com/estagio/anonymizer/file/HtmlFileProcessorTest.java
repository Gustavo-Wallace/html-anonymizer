package br.com.estagio.anonymizer.file;

import br.com.estagio.anonymizer.core.HtmlAnonymizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlFileProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAnonymizedHtmlOutput() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.html", "<html><body>550000000000 Internal Ticket Number 0000001</body></html>");
        Path output = tempDir.resolve("output.html");

        processor.processFile(input, output, new HtmlAnonymizer());

        String result = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(result.startsWith("<html><body>"));
        assertTrue(result.endsWith("</body></html>"));
        assertFalse(result.contains("550000000000"));
        assertFalse(result.contains("Internal Ticket Number 0000001"));
        assertTrue(result.contains("Internal Ticket Number "));
    }

    @Test
    void shouldNotModifyOriginalFile() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        String original = "<td>550000000000</td>";
        Path input = writeInputFile("input.html", original);
        Path output = tempDir.resolve("output.html");

        processor.processFile(input, output, new HtmlAnonymizer());

        assertEquals(original, Files.readString(input, StandardCharsets.UTF_8));
    }

    @Test
    void shouldCreateOutputDirectoryWhenNeeded() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.html", "<td>550000000000</td>");
        Path output = tempDir.resolve("nested").resolve("folder").resolve("output.html");

        processor.processFile(input, output, new HtmlAnonymizer());

        assertTrue(Files.exists(output));
        assertTrue(Files.isRegularFile(output));
    }

    @Test
    void shouldRejectMissingInputFile() {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = tempDir.resolve("missing.html");
        Path output = tempDir.resolve("output.html");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.processFile(input, output, new HtmlAnonymizer())
        );

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void shouldRejectInvalidExtension() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.txt", "<td>550000000000</td>");
        Path output = tempDir.resolve("output.html");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.processFile(input, output, new HtmlAnonymizer())
        );

        assertTrue(exception.getMessage().contains(".html or .htm"));
    }

    @Test
    void shouldProcessHtmlExtension() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.html", "<td>550000000000</td>");
        Path output = tempDir.resolve("output.html");

        processor.processFile(input, output, new HtmlAnonymizer());

        assertTrue(Files.exists(output));
    }

    @Test
    void shouldProcessHtmExtension() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.htm", "<td>550000000000</td>");
        Path output = tempDir.resolve("output.htm");

        processor.processFile(input, output, new HtmlAnonymizer());

        assertTrue(Files.exists(output));
    }

    @Test
    void shouldProcessHtmlExtensionIgnoringCase() throws IOException {
        HtmlFileProcessor processor = new HtmlFileProcessor();
        Path input = writeInputFile("input.HTML", "<td>550000000000</td>");
        Path output = tempDir.resolve("output.html");

        processor.processFile(input, output, new HtmlAnonymizer());

        assertTrue(Files.exists(output));
    }

    private Path writeInputFile(String fileName, String content) throws IOException {
        Path input = tempDir.resolve(fileName);
        Files.writeString(input, content, StandardCharsets.UTF_8);
        return input;
    }
}
