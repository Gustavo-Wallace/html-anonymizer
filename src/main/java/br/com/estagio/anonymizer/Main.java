package br.com.estagio.anonymizer;

import br.com.estagio.anonymizer.file.FolderProcessingResult;
import br.com.estagio.anonymizer.file.InputProcessor;
import br.com.estagio.anonymizer.file.ProcessingListener;
import br.com.estagio.anonymizer.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
            return;
        }

        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 2) {
            printUsage(err);
            return 1;
        }

        Path inputPath = Path.of(args[0]);
        Path outputFolder = Path.of(args[1]);

        try {
            FolderProcessingResult result = new InputProcessor().processInput(
                    inputPath,
                    outputFolder,
                    consoleProcessingListener(out)
            );
            printSummary(out, inputPath, outputFolder, result);
            return 0;
        } catch (IllegalArgumentException | IOException exception) {
            err.println("Erro ao processar entrada: " + exception.getMessage());
            return 2;
        }
    }

    private static void printUsage(PrintStream err) {
        err.println("Uso: java -jar target/html-anonymizer-1.0.0.jar <arquivo-html-ou-pasta-entrada> <pasta-saida>");
        err.println("Exemplo: java -jar target/html-anonymizer-1.0.0.jar \"C:/entrada\" \"C:/saida\"");
    }

    private static void printSummary(
            PrintStream out,
            Path inputPath,
            Path outputFolder,
            FolderProcessingResult result
    ) {
        out.println("Processamento concluido.");
        out.println("Arquivos HTML encontrados: " + result.getHtmlFilesFound());
        out.println("Arquivos processados: " + result.getFilesProcessed());
        out.println("Tempo total: " + formatDuration(result.getTotalDuration()));
        result.getLargestFile().ifPresent(largestFile -> out.println(
                "Maior arquivo: " + displayName(largestFile)
                        + " (" + formatMegabytes(result.getLargestFileSizeBytes()) + ")"
                        + " em " + formatDuration(result.getLargestFileDuration())
        ));
        out.println("Entrada: " + inputPath);
        out.println("Pasta de saida: " + outputFolder);
    }

    private static ProcessingListener consoleProcessingListener(PrintStream out) {
        return new ProcessingListener() {
            @Override
            public void fileStarted(Path inputFile, long sizeBytes) {
                out.println("Processando: " + displayName(inputFile) + " (" + formatMegabytes(sizeBytes) + ")");
                out.flush();
            }

            @Override
            public void fileFinished(Path inputFile, Path outputFile, long sizeBytes, Duration duration) {
                out.println("Concluido: " + displayName(inputFile) + " em " + formatDuration(duration));
                out.flush();
            }
        };
    }

    private static String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "min " + remainingSeconds + "s";
        }

        return remainingSeconds + "s";
    }

    private static String formatMegabytes(long sizeBytes) {
        return String.format(Locale.ROOT, "%.1f MB", sizeBytes / 1024.0 / 1024.0);
    }

    private static String displayName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
