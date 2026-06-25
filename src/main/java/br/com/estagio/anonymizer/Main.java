package br.com.estagio.anonymizer;

import br.com.estagio.anonymizer.file.FolderProcessingResult;
import br.com.estagio.anonymizer.file.FolderProcessor;
import br.com.estagio.anonymizer.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

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

        Path inputFolder = Path.of(args[0]);
        Path outputFolder = Path.of(args[1]);

        try {
            FolderProcessingResult result = new FolderProcessor().processFolder(inputFolder, outputFolder);
            printSummary(out, inputFolder, outputFolder, result);
            return 0;
        } catch (IllegalArgumentException | IOException exception) {
            err.println("Erro ao processar pasta: " + exception.getMessage());
            return 2;
        }
    }

    private static void printUsage(PrintStream err) {
        err.println("Uso: java -jar target/html-anonymizer-1.0.0.jar <pasta-entrada> <pasta-saida>");
        err.println("Exemplo: java -jar target/html-anonymizer-1.0.0.jar \"C:/entrada\" \"C:/saida\"");
    }

    private static void printSummary(
            PrintStream out,
            Path inputFolder,
            Path outputFolder,
            FolderProcessingResult result
    ) {
        out.println("Processamento concluido.");
        out.println("Arquivos HTML encontrados: " + result.getHtmlFilesFound());
        out.println("Arquivos processados: " + result.getFilesProcessed());
        out.println("Pasta de entrada: " + inputFolder);
        out.println("Pasta de saida: " + outputFolder);
    }
}
