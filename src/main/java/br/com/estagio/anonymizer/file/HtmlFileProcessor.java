package br.com.estagio.anonymizer.file;

import br.com.estagio.anonymizer.core.AnonymizationListener;
import br.com.estagio.anonymizer.core.HtmlAnonymizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class HtmlFileProcessor {
    public void processFile(Path inputFile, Path outputFile, HtmlAnonymizer anonymizer) throws IOException {
        processFile(inputFile, outputFile, anonymizer, ProcessingListener.NONE);
    }

    public void processFile(
            Path inputFile,
            Path outputFile,
            HtmlAnonymizer anonymizer,
            ProcessingListener listener
    ) throws IOException {
        Objects.requireNonNull(inputFile, "inputFile must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(anonymizer, "anonymizer must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        validateInputFile(inputFile);

        Instant readStart = Instant.now();
        listener.stageStarted(inputFile, "Leitura");
        String content = Files.readString(inputFile, StandardCharsets.UTF_8);
        listener.stageFinished(inputFile, "Leitura", Duration.between(readStart, Instant.now()));

        String anonymizedContent = anonymizer.anonymize(content, new AnonymizationListener() {
            @Override
            public void stageStarted(String stageName) {
                listener.stageStarted(inputFile, stageName);
            }

            @Override
            public void stageFinished(String stageName, Duration duration) {
                listener.stageFinished(inputFile, stageName, duration);
            }
        });

        Path outputParent = outputFile.getParent();
        if (outputParent != null) {
            Files.createDirectories(outputParent);
        }

        Instant writeStart = Instant.now();
        listener.stageStarted(inputFile, "Escrita");
        Files.writeString(outputFile, anonymizedContent, StandardCharsets.UTF_8);
        listener.stageFinished(inputFile, "Escrita", Duration.between(writeStart, Instant.now()));
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
