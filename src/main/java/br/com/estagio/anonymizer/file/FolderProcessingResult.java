package br.com.estagio.anonymizer.file;

import java.nio.file.Path;
import java.util.List;

public class FolderProcessingResult {
    private final int htmlFilesFound;
    private final int filesProcessed;
    private final List<Path> processedFiles;

    public FolderProcessingResult(int htmlFilesFound, int filesProcessed, List<Path> processedFiles) {
        this.htmlFilesFound = htmlFilesFound;
        this.filesProcessed = filesProcessed;
        this.processedFiles = List.copyOf(processedFiles);
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
}
