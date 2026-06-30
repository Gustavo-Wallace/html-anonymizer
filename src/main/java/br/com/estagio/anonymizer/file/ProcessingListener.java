package br.com.estagio.anonymizer.file;

import java.nio.file.Path;
import java.time.Duration;

public interface ProcessingListener {
    ProcessingListener NONE = new ProcessingListener() {
    };

    default void fileStarted(Path inputFile, long sizeBytes) {
    }

    default void fileFinished(Path inputFile, Path outputFile, long sizeBytes, Duration duration) {
    }
}
