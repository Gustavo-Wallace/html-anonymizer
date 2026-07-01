package br.com.estagio.anonymizer.core;

import java.time.Duration;

public interface AnonymizationListener {
    AnonymizationListener NONE = new AnonymizationListener() {
    };

    default void stageStarted(String stageName) {
    }

    default void stageFinished(String stageName, Duration duration) {
    }
}
