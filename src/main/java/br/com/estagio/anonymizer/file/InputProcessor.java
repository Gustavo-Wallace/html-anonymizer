package br.com.estagio.anonymizer.file;

import br.com.estagio.anonymizer.core.HtmlAnonymizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InputProcessor {
    private final HtmlFileProcessor htmlFileProcessor;
    private final FolderProcessor folderProcessor;

    public InputProcessor() {
        this(new HtmlFileProcessor(), new FolderProcessor());
    }

    InputProcessor(HtmlFileProcessor htmlFileProcessor, FolderProcessor folderProcessor) {
        this.htmlFileProcessor = Objects.requireNonNull(htmlFileProcessor);
        this.folderProcessor = Objects.requireNonNull(folderProcessor);
    }

    public FolderProcessingResult processInput(Path inputPath, Path outputFolder) throws IOException {
        return processInput(inputPath, outputFolder, ProcessingListener.NONE);
    }

    public FolderProcessingResult processInput(
            Path inputPath,
            Path outputFolder,
            ProcessingListener listener
    ) throws IOException {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        Objects.requireNonNull(outputFolder, "outputFolder must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("Input path does not exist: " + inputPath);
        }

        if (Files.exists(outputFolder) && !Files.isDirectory(outputFolder)) {
            throw new IllegalArgumentException("Output path must be a directory: " + outputFolder);
        }

        if (Files.isRegularFile(inputPath)) {
            return processFile(inputPath, outputFolder, listener);
        }

        if (Files.isDirectory(inputPath)) {
            return processFolder(inputPath, outputFolder, listener);
        }

        throw new IllegalArgumentException("Input path must be an HTML file or a directory: " + inputPath);
    }

    private FolderProcessingResult processFile(
            Path inputFile,
            Path outputFolder,
            ProcessingListener listener
    ) throws IOException {
        if (!hasHtmlExtension(inputFile)) {
            throw new IllegalArgumentException("Input file must have .html or .htm extension: " + inputFile);
        }

        Path fileName = inputFile.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Input file name is invalid: " + inputFile);
        }

        Path outputFile = outputFolder.resolve(OutputFileName.anonymized(fileName));
        if (outputFile.toAbsolutePath().normalize().equals(inputFile.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Output file cannot be the same as input file: " + outputFile);
        }

        Instant totalStart = Instant.now();
        long sizeBytes = Files.size(inputFile);
        Instant fileStart = Instant.now();
        listener.fileStarted(inputFile, sizeBytes);
        htmlFileProcessor.processFile(inputFile, outputFile, new HtmlAnonymizer(), listener);
        Duration fileDuration = Duration.between(fileStart, Instant.now());
        listener.fileFinished(inputFile, outputFile, sizeBytes, fileDuration);
        return new FolderProcessingResult(
                1,
                1,
                List.of(OutputFileName.anonymized(fileName)),
                Duration.between(totalStart, Instant.now()),
                inputFile,
                sizeBytes,
                fileDuration
        );
    }

    private FolderProcessingResult processFolder(
            Path inputFolder,
            Path outputFolder,
            ProcessingListener listener
    ) throws IOException {
        return folderProcessor.processFolder(inputFolder, outputFolder, listener);
    }

    private boolean hasHtmlExtension(Path file) {
        Path fileName = file.getFileName();
        if (fileName == null) {
            return false;
        }

        String lowerCaseName = fileName.toString().toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".html") || lowerCaseName.endsWith(".htm");
    }
}
