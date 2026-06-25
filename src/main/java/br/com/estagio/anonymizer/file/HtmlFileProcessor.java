package br.com.estagio.anonymizer.file;

import br.com.estagio.anonymizer.core.HtmlAnonymizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class HtmlFileProcessor {
    public void processFile(Path inputFile, Path outputFile, HtmlAnonymizer anonymizer) throws IOException {
        Objects.requireNonNull(inputFile, "inputFile must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(anonymizer, "anonymizer must not be null");

        validateInputFile(inputFile);

        String content = Files.readString(inputFile, StandardCharsets.UTF_8);
        String anonymizedContent = anonymizer.anonymize(content);

        Path outputParent = outputFile.getParent();
        if (outputParent != null) {
            Files.createDirectories(outputParent);
        }

        Files.writeString(outputFile, anonymizedContent, StandardCharsets.UTF_8);
    }

    private void validateInputFile(Path inputFile) {
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile);
        }

        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputFile);
        }

        if (!hasHtmlExtension(inputFile)) {
            throw new IllegalArgumentException("Input file must have .html or .htm extension: " + inputFile);
        }
    }

    private boolean hasHtmlExtension(Path inputFile) {
        Path fileName = inputFile.getFileName();
        if (fileName == null) {
            return false;
        }

        String lowerCaseName = fileName.toString().toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".html") || lowerCaseName.endsWith(".htm");
    }
}
