package br.com.estagio.anonymizer.file;

import br.com.estagio.anonymizer.core.HtmlAnonymizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class FolderProcessor {
    private final HtmlFileProcessor htmlFileProcessor;

    public FolderProcessor() {
        this(new HtmlFileProcessor());
    }

    FolderProcessor(HtmlFileProcessor htmlFileProcessor) {
        this.htmlFileProcessor = Objects.requireNonNull(htmlFileProcessor);
    }

    public FolderProcessingResult processFolder(Path inputFolder, Path outputFolder) throws IOException {
        Objects.requireNonNull(inputFolder, "inputFolder must not be null");
        Objects.requireNonNull(outputFolder, "outputFolder must not be null");

        validateFolders(inputFolder, outputFolder);

        List<Path> htmlFiles;
        try (Stream<Path> paths = Files.walk(inputFolder)) {
            htmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(this::hasHtmlExtension)
                    .sorted()
                    .toList();
        }

        HtmlAnonymizer anonymizer = new HtmlAnonymizer();
        for (Path htmlFile : htmlFiles) {
            Path relativePath = inputFolder.relativize(htmlFile);
            Path outputFile = outputFolder.resolve(relativePath);
            htmlFileProcessor.processFile(htmlFile, outputFile, anonymizer);
        }

        List<Path> processedFiles = htmlFiles.stream()
                .map(inputFolder::relativize)
                .toList();

        return new FolderProcessingResult(htmlFiles.size(), htmlFiles.size(), processedFiles);
    }

    private void validateFolders(Path inputFolder, Path outputFolder) throws IOException {
        if (!Files.exists(inputFolder)) {
            throw new IllegalArgumentException("Input folder does not exist: " + inputFolder);
        }

        if (!Files.isDirectory(inputFolder)) {
            throw new IllegalArgumentException("Input path is not a directory: " + inputFolder);
        }

        Path inputPath = inputFolder.toRealPath();
        Path outputPath = outputFolder.toAbsolutePath().normalize();

        if (outputPath.equals(inputPath)) {
            throw new IllegalArgumentException("Output folder cannot be the same as input folder: " + outputFolder);
        }

        if (outputPath.startsWith(inputPath)) {
            throw new IllegalArgumentException("Output folder cannot be inside input folder: " + outputFolder);
        }
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
