package br.com.estagio.anonymizer.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldProcessSingleHtmlFileIntoOutputFolder() throws IOException {
        Path inputFile = writeFile(tempDir.resolve("single.html"), "<td>550000000000</td>");
        Path outputFolder = tempDir.resolve("output");

        FolderProcessingResult result = new InputProcessor().processInput(inputFile, outputFolder);

        Path outputFile = outputFolder.resolve("single.html");
        assertEquals(1, result.getHtmlFilesFound());
        assertEquals(1, result.getFilesProcessed());
        assertEquals(List.of(Path.of("single.html")), result.getProcessedFiles());
        assertTrue(Files.exists(outputFile));
        assertFalse(readFile(outputFile).contains("550000000000"));
        assertEquals("<td>550000000000</td>", readFile(inputFile));
    }

    @Test
    void shouldProcessFolderInsideAnonymizedRootFolderWithInputName() throws IOException {
        Path inputFolder = createDirectory("empresa_a");
        Path outputFolder = tempDir.resolve("anonimizados");
        writeFile(inputFolder.resolve("index.html"), "<td>550000000000</td>");

        new InputProcessor().processInput(inputFolder, outputFolder);

        Path outputFile = outputFolder.resolve("empresa_a_anonimizado").resolve("index.html");
        assertTrue(Files.exists(outputFile));
        assertFalse(Files.exists(outputFolder.resolve("index.html")));
        assertFalse(Files.exists(outputFolder.resolve("empresa_a").resolve("index.html")));
        assertFalse(readFile(outputFile).contains("550000000000"));
    }

    @Test
    void shouldAllowOutputFolderToBeInputParentFolder() throws IOException {
        Path inputFolder = createDirectory("clientes");
        Path outputFolder = tempDir;
        writeFile(inputFolder.resolve("a.html"), "<td>550000000000</td>");

        new InputProcessor().processInput(inputFolder, outputFolder);

        assertTrue(Files.exists(tempDir.resolve("clientes_anonimizado").resolve("a.html")));
        assertFalse(Files.exists(tempDir.resolve("a.html")));
        assertEquals("<td>550000000000</td>", readFile(inputFolder.resolve("a.html")));
    }

    @Test
    void shouldPreserveSubfoldersInsideOutputRootFolder() throws IOException {
        Path inputFolder = createDirectory("clientes");
        Path outputFolder = tempDir.resolve("anonimizados");
        writeFile(inputFolder.resolve("empresa1").resolve("b.html"), "<td>550000000000</td>");
        writeFile(inputFolder.resolve("empresa2").resolve("subpasta").resolve("c.html"), "<td>551111111111</td>");

        new InputProcessor().processInput(inputFolder, outputFolder);

        assertTrue(Files.exists(outputFolder.resolve("clientes_anonimizado").resolve("empresa1").resolve("b.html")));
        assertTrue(Files.exists(outputFolder.resolve("clientes_anonimizado").resolve("empresa2").resolve("subpasta").resolve("c.html")));
        assertFalse(Files.exists(outputFolder.resolve("clientes_anonimizado").resolve("empresa1_anonimizado")));
        assertFalse(Files.exists(outputFolder.resolve("clientes_anonimizado").resolve("empresa2_anonimizado")));
    }

    @Test
    void shouldRejectNonHtmlInputFile() throws IOException {
        Path inputFile = writeFile(tempDir.resolve("notes.txt"), "550000000000");
        Path outputFolder = tempDir.resolve("output");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new InputProcessor().processInput(inputFile, outputFolder)
        );

        assertTrue(exception.getMessage().contains(".html or .htm"));
    }

    @Test
    void shouldNotCopyNonHtmlFilesWhenProcessingFolder() throws IOException {
        Path inputFolder = createDirectory("empresa_a");
        Path outputFolder = tempDir.resolve("anonimizados");
        writeFile(inputFolder.resolve("index.html"), "<td>550000000000</td>");
        writeFile(inputFolder.resolve("notes.txt"), "550000000000");

        new InputProcessor().processInput(inputFolder, outputFolder);

        assertTrue(Files.exists(outputFolder.resolve("empresa_a_anonimizado").resolve("index.html")));
        assertFalse(Files.exists(outputFolder.resolve("empresa_a_anonimizado").resolve("notes.txt")));
    }

    @Test
    void shouldRejectOutputFolderInsideInputFolder() throws IOException {
        Path inputFolder = createDirectory("clientes");
        Path outputFolder = inputFolder.resolve("saida");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new InputProcessor().processInput(inputFolder, outputFolder)
        );

        assertTrue(exception.getMessage().contains("inside input"));
    }

    @Test
    void shouldKeepSameReplacementAcrossFilesWhenProcessingFolder() throws IOException {
        Path inputFolder = createDirectory("empresa_a");
        Path outputFolder = tempDir.resolve("anonimizados");
        writeFile(inputFolder.resolve("first.html"), "<td>Internal Ticket Number 0000001</td>");
        writeFile(inputFolder.resolve("second.html"), "<td>Internal Ticket Number: 0000001</td>");

        new InputProcessor().processInput(inputFolder, outputFolder);

        String first = readFile(outputFolder.resolve("empresa_a_anonimizado").resolve("first.html"));
        String second = readFile(outputFolder.resolve("empresa_a_anonimizado").resolve("second.html"));
        String firstTicket = first.replaceAll(".*Internal Ticket Number\\s+(\\d+).*", "$1");
        String secondTicket = second.replaceAll(".*Internal Ticket Number:\\s+(\\d+).*", "$1");
        assertEquals(firstTicket, secondTicket);
        assertNotEquals("0000001", firstTicket);
    }

    private Path createDirectory(String directoryName) throws IOException {
        Path directory = tempDir.resolve(directoryName);
        Files.createDirectories(directory);
        return directory;
    }

    private Path writeFile(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
