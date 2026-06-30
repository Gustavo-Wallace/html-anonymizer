package br.com.estagio.anonymizer.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderProcessorTest {
    private static final Pattern PLAIN_PHONE_PATTERN = Pattern.compile("\\b\\d{12}\\b");
    private static final Pattern TICKET_PATTERN = Pattern.compile("Internal Ticket Number:?\\s+(\\d+)");

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessMultipleHtmlFilesInRootFolder() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("first.html"), "<td>550000000000</td>");
        writeFile(input.resolve("second.htm"), "<td>551111111111</td>");

        FolderProcessingResult result = new FolderProcessor().processFolder(input, output);

        assertEquals(2, result.getHtmlFilesFound());
        assertEquals(2, result.getFilesProcessed());
        assertTrue(result.getProcessedFiles().contains(Path.of("first_anonimizado.html")));
        assertTrue(result.getProcessedFiles().contains(Path.of("second_anonimizado.htm")));
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("first_anonimizado.html")));
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("second_anonimizado.htm")));
        assertFalse(Files.exists(output.resolve("first.html")));
        assertFalse(readFile(output.resolve("input_anonimizado").resolve("first_anonimizado.html")).contains("550000000000"));
        assertFalse(readFile(output.resolve("input_anonimizado").resolve("second_anonimizado.htm")).contains("551111111111"));
    }

    @Test
    void shouldProcessHtmlFilesInsideSubfolders() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("sub").resolve("deep").resolve("page.HTML"), "<td>550000000000</td>");

        FolderProcessingResult result = new FolderProcessor().processFolder(input, output);

        assertEquals(1, result.getHtmlFilesFound());
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("sub").resolve("deep").resolve("page_anonimizado.HTML")));
    }

    @Test
    void shouldPreserveRelativeFolderStructure() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        Path relativePath = Path.of("one", "two", "page.htm");
        writeFile(input.resolve(relativePath), "<td>550000000000</td>");

        FolderProcessingResult result = new FolderProcessor().processFolder(input, output);

        assertEquals(List.of(Path.of("one", "two", "page_anonimizado.htm")), result.getProcessedFiles());
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("one").resolve("two").resolve("page_anonimizado.htm")));
        assertFalse(Files.exists(output.resolve("input_anonimizado").resolve("one_anonimizado")));
    }

    @Test
    void shouldNotCopyNonHtmlFiles() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("page.html"), "<td>550000000000</td>");
        writeFile(input.resolve("notes.txt"), "550000000000");

        FolderProcessingResult result = new FolderProcessor().processFolder(input, output);

        assertEquals(1, result.getHtmlFilesFound());
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("page_anonimizado.html")));
        assertFalse(Files.exists(output.resolve("input_anonimizado").resolve("notes.txt")));
    }

    @Test
    void shouldNotDuplicateAnonymizedSuffixWhenProcessingFolder() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("page_anonimizado.html"), "<td>550000000000</td>");

        FolderProcessingResult result = new FolderProcessor().processFolder(input, output);

        assertEquals(List.of(Path.of("page_anonimizado.html")), result.getProcessedFiles());
        assertTrue(Files.exists(output.resolve("input_anonimizado").resolve("page_anonimizado.html")));
        assertFalse(Files.exists(output.resolve("input_anonimizado").resolve("page_anonimizado_anonimizado.html")));
    }

    @Test
    void shouldNotModifyOriginalFiles() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        Path originalFile = input.resolve("page.html");
        String originalContent = "<td>550000000000</td>";
        writeFile(originalFile, originalContent);

        new FolderProcessor().processFolder(input, output);

        assertEquals(originalContent, readFile(originalFile));
    }

    @Test
    void shouldKeepSamePhoneReplacementAcrossFiles() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("first.html"), "<td>550000000000</td>");
        writeFile(input.resolve("second.html"), "<td>550000000000</td>");

        new FolderProcessor().processFolder(input, output);

        String firstReplacement = firstPhone(readFile(output.resolve("input_anonimizado").resolve("first_anonimizado.html")));
        String secondReplacement = firstPhone(readFile(output.resolve("input_anonimizado").resolve("second_anonimizado.html")));
        assertEquals(firstReplacement, secondReplacement);
        assertNotEquals("550000000000", firstReplacement);
    }

    @Test
    void shouldKeepSameTicketReplacementAcrossFiles() throws IOException {
        Path input = createDirectory("input");
        Path output = tempDir.resolve("output");
        writeFile(input.resolve("first.html"), "<td>Internal Ticket Number 0000001</td>");
        writeFile(input.resolve("second.html"), "<td>Internal Ticket Number: 0000001</td>");

        new FolderProcessor().processFolder(input, output);

        String firstReplacement = firstTicket(readFile(output.resolve("input_anonimizado").resolve("first_anonimizado.html")));
        String secondReplacement = firstTicket(readFile(output.resolve("input_anonimizado").resolve("second_anonimizado.html")));
        assertEquals(firstReplacement, secondReplacement);
        assertNotEquals("0000001", firstReplacement);
    }

    @Test
    void shouldRejectMissingInputFolder() {
        Path input = tempDir.resolve("missing");
        Path output = tempDir.resolve("output");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FolderProcessor().processFolder(input, output)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void shouldRejectFileAsInputFolder() throws IOException {
        Path input = tempDir.resolve("input.html");
        Path output = tempDir.resolve("output");
        writeFile(input, "<td>550000000000</td>");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FolderProcessor().processFolder(input, output)
        );

        assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    void shouldAllowOutputFolderToBeInputParentFolder() throws IOException {
        Path input = createDirectory("clientes");
        Path output = tempDir;
        writeFile(input.resolve("a.html"), "<td>550000000000</td>");

        new FolderProcessor().processFolder(input, output);

        assertTrue(Files.exists(tempDir.resolve("clientes_anonimizado").resolve("a_anonimizado.html")));
        assertFalse(Files.exists(tempDir.resolve("a.html")));
        assertEquals("<td>550000000000</td>", readFile(input.resolve("a.html")));
    }

    @Test
    void shouldRejectOutputFolderEqualToInputFolder() throws IOException {
        Path input = createDirectory("input");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FolderProcessor().processFolder(input, input)
        );

        assertTrue(exception.getMessage().contains("same as input"));
    }

    @Test
    void shouldRejectOutputFolderInsideInputFolder() throws IOException {
        Path input = createDirectory("input");
        Path output = input.resolve("output");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FolderProcessor().processFolder(input, output)
        );

        assertTrue(exception.getMessage().contains("inside input"));
    }

    private Path createDirectory(String directoryName) throws IOException {
        Path directory = tempDir.resolve(directoryName);
        Files.createDirectories(directory);
        return directory;
    }

    private void writeFile(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private String firstPhone(String content) {
        Matcher matcher = PLAIN_PHONE_PATTERN.matcher(content);
        assertTrue(matcher.find());
        return matcher.group();
    }

    private String firstTicket(String content) {
        Matcher matcher = TICKET_PATTERN.matcher(content);
        assertTrue(matcher.find());
        return matcher.group(1);
    }
}
