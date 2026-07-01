package br.com.estagio.anonymizer.file;

import java.nio.file.Path;
import java.time.Duration;

public interface ProcessingListener {
    ProcessingListener NONE = new ProcessingListener() {
    };

    default void fileStarted(Path inputFile, long sizeBytes) {
    }

    default void stageStarted(Path inputFile, String stageName) {
    }

    default void stageFinished(Path inputFile, String stageName, Duration duration) {
    }

    default void fileFinished(Path inputFile, Path outputFile, long sizeBytes, Duration duration) {
    }
}
