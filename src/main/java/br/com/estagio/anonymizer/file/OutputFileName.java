package br.com.estagio.anonymizer.file;

import java.nio.file.Path;
import java.util.Locale;

final class OutputFileName {
    private static final String SUFFIX = "_anonimizado";

    private OutputFileName() {
    }

    static Path anonymized(Path fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("Input file name is invalid.");
        }

        String name = fileName.toString();
        String lowerCaseName = name.toLowerCase(Locale.ROOT);
        String extension;

        if (lowerCaseName.endsWith(".html")) {
            extension = name.substring(name.length() - ".html".length());
        } else if (lowerCaseName.endsWith(".htm")) {
            extension = name.substring(name.length() - ".htm".length());
        } else {
            throw new IllegalArgumentException("Input file must have .html or .htm extension: " + fileName);
        }

        String baseName = name.substring(0, name.length() - extension.length());
        if (baseName.toLowerCase(Locale.ROOT).endsWith(SUFFIX)) {
            return fileName;
        }

        return Path.of(baseName + SUFFIX + extension);
    }
}
