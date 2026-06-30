package br.com.estagio.anonymizer.file;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class FolderProcessingResult {
    private final int htmlFilesFound;
    private final int filesProcessed;
    private final List<Path> processedFiles;
    private final Duration totalDuration;
    private final Path largestFile;
    private final long largestFileSizeBytes;
    private final Duration largestFileDuration;

    public FolderProcessingResult(int htmlFilesFound, int filesProcessed, List<Path> processedFiles) {
        this(htmlFilesFound, filesProcessed, processedFiles, Duration.ZERO, null, 0, Duration.ZERO);
    }

    public FolderProcessingResult(
            int htmlFilesFound,
            int filesProcessed,
            List<Path> processedFiles,
            Duration totalDuration,
            Path largestFile,
            long largestFileSizeBytes,
            Duration largestFileDuration
    ) {
        this.htmlFilesFound = htmlFilesFound;
        this.filesProcessed = filesProcessed;
        this.processedFiles = List.copyOf(processedFiles);
        this.totalDuration = totalDuration;
        this.largestFile = largestFile;
        this.largestFileSizeBytes = largestFileSizeBytes;
        this.largestFileDuration = largestFileDuration;
    }

    public int getHtmlFilesFound() {
        return htmlFilesFound;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public List<Path> getProcessedFiles() {
        return processedFiles;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public Optional<Path> getLargestFile() {
        return Optional.ofNullable(largestFile);
    }

    public long getLargestFileSizeBytes() {
        return largestFileSizeBytes;
    }

    public Duration getLargestFileDuration() {
        return largestFileDuration;
    }
}
