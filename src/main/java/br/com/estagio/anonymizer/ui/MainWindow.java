package br.com.estagio.anonymizer.ui;

import br.com.estagio.anonymizer.file.FolderProcessingResult;
import br.com.estagio.anonymizer.file.InputProcessor;
import br.com.estagio.anonymizer.file.ProcessingListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainWindow extends JFrame {
    private static final String START_BUTTON_TEXT = "Iniciar anonimizacao";
    private static final String PROCESSING_BUTTON_TEXT = "Processando...";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JTextField inputFolderField = new JTextField();
    private final JTextField outputFolderField = new JTextField();
    private final JButton inputBrowseButton = new JButton("Selecionar...");
    private final JButton outputBrowseButton = new JButton("Selecionar...");
    private final JButton startButton = new JButton(START_BUTTON_TEXT);
    private final JTextArea logArea = new JTextArea();
    private final InputProcessor inputProcessor;

    public MainWindow() {
        this(new InputProcessor());
    }

    MainWindow(InputProcessor inputProcessor) {
        super("HTML Anonymizer");
        this.inputProcessor = inputProcessor;
        configureWindow();
        configureActions();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 480);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(createTopPanel(), BorderLayout.NORTH);
        root.add(createLogPanel(), BorderLayout.CENTER);
        root.add(startButton, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Selecione ou arraste um arquivo HTML ou pasta de entrada, e uma pasta de saida."), BorderLayout.NORTH);
        panel.add(createFormPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, constraints, 0, "Arquivo HTML ou pasta de entrada:", inputFolderField, inputBrowseButton);
        addRow(panel, constraints, 1, "Pasta de saida:", outputFolderField, outputBrowseButton);

        return panel;
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            JTextField textField,
            JButton browseButton
    ) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(textField, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        panel.add(browseButton, constraints);
    }

    private JScrollPane createLogPanel() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        return scrollPane;
    }

    private void configureActions() {
        inputBrowseButton.addActionListener(event -> chooseInputPath());
        outputBrowseButton.addActionListener(event -> chooseDirectory(outputFolderField, "Selecionar pasta de saida"));
        startButton.addActionListener(event -> startProcessing());
        configureDropTarget(inputFolderField, "entrada", true);
        configureDropTarget(outputFolderField, "saida", false);
    }

    private void chooseInputPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar arquivo HTML ou pasta de entrada");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedPath = chooser.getSelectedFile().toPath();
            if (Files.isRegularFile(selectedPath) && !hasHtmlExtension(selectedPath)) {
                showValidationError("Selecione um arquivo .html/.htm ou uma pasta: " + selectedPath);
                return;
            }

            inputFolderField.setText(selectedPath.toString());
        }
    }

    private void chooseDirectory(JTextField targetField, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void configureDropTarget(JTextField targetField, String fieldName, boolean acceptsHtmlFile) {
        targetField.setToolTipText(acceptsHtmlFile ? "Arraste uma pasta ou arquivo HTML para preencher este campo." : "Arraste uma pasta para preencher este campo.");
        targetField.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return targetField.isEnabled() && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    List<File> files = readDroppedFiles(support.getTransferable());
                    if (files.size() != 1) {
                        String expected = acceptsHtmlFile ? "um arquivo HTML ou uma pasta" : "uma pasta";
                        showDropError("Arraste apenas " + expected + " para o campo de " + fieldName + ".");
                        return false;
                    }

                    File droppedFile = files.get(0);
                    if (!isValidDroppedInput(droppedFile, acceptsHtmlFile)) {
                        String expected = acceptsHtmlFile ? "uma pasta ou arquivo .html/.htm" : "uma pasta";
                        showDropError("O item arrastado para " + fieldName + " deve ser " + expected + ": " + droppedFile);
                        return false;
                    }

                    targetField.setText(droppedFile.toPath().toString());
                    String logPrefix = acceptsHtmlFile ? "Entrada selecionada" : "Pasta de saida selecionada";
                    appendLog(logPrefix + " por arrastar e soltar: " + droppedFile.toPath());
                    return true;
                } catch (IOException | UnsupportedFlavorException exception) {
                    showDropError("Nao foi possivel ler o item arrastado: " + exception.getMessage());
                    return false;
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<File> readDroppedFiles(Transferable transferable) throws IOException, UnsupportedFlavorException {
        return (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
    }

    private boolean isValidDroppedInput(File droppedFile, boolean acceptsHtmlFile) {
        if (droppedFile.isDirectory()) {
            return true;
        }

        return acceptsHtmlFile && droppedFile.isFile() && hasHtmlExtension(droppedFile.toPath());
    }

    private void startProcessing() {
        logArea.setText("");

        Path inputPath;
        Path outputFolder;
        try {
            inputPath = readPath(inputFolderField, "A entrada nao pode estar vazia.");
            outputFolder = readPath(outputFolderField, "A pasta de saida nao pode estar vazia.");
            validateBeforeProcessing(inputPath, outputFolder);
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
            return;
        }

        setProcessingState(true);
        LocalDateTime startTime = LocalDateTime.now();

        SwingWorker<FolderProcessingResult, Void> worker = new SwingWorker<>() {
            @Override
            protected FolderProcessingResult doInBackground() throws Exception {
                appendLog("Inicio do processamento: " + formatDateTime(startTime));
                appendLog("Entrada: " + inputPath);
                appendLog("Pasta de saida: " + outputFolder);
                return inputProcessor.processInput(inputPath, outputFolder, processingListener());
            }

            @Override
            protected void done() {
                LocalDateTime endTime = LocalDateTime.now();
                try {
                    FolderProcessingResult result = get();
                    appendLog("Termino do processamento: " + formatDateTime(endTime));
                    appendLog("Duracao aproximada: " + formatDuration(Duration.between(startTime, endTime)));
                    appendLog("Arquivos HTML encontrados: " + result.getHtmlFilesFound());
                    appendLog("Arquivos processados: " + result.getFilesProcessed());
                    result.getLargestFile().ifPresent(largestFile -> appendLog(
                            "Maior arquivo: " + displayName(largestFile)
                                    + " (" + formatMegabytes(result.getLargestFileSizeBytes()) + ")"
                                    + " em " + formatDuration(result.getLargestFileDuration())
                    ));
                    if (result.getHtmlFilesFound() == 0) {
                        appendLog("Nenhum arquivo HTML foi encontrado.");
                    }
                    appendLog("Processamento concluido.");
                    JOptionPane.showMessageDialog(MainWindow.this, "Processamento concluido.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    appendLog("Erro: processamento interrompido.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    appendLog("Erro: " + cause.getMessage());
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setProcessingState(false);
                }
            }
        };

        worker.execute();
    }

    private ProcessingListener processingListener() {
        return new ProcessingListener() {
            @Override
            public void fileStarted(Path inputFile, long sizeBytes) {
                appendLog("Processando: " + displayName(inputFile) + " (" + formatMegabytes(sizeBytes) + ")");
            }

            @Override
            public void stageStarted(Path inputFile, String stageName) {
                appendLog(stageName + " iniciado.");
            }

            @Override
            public void stageFinished(Path inputFile, String stageName, Duration duration) {
                appendLog(stageName + " concluido em " + formatDuration(duration));
            }

            @Override
            public void fileFinished(Path inputFile, Path outputFile, long sizeBytes, Duration duration) {
                appendLog("Concluido: " + displayName(inputFile) + " em " + formatDuration(duration));
            }
        };
    }

    private Path readPath(JTextField field, String emptyMessage) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }

        try {
            return Path.of(value);
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Caminho invalido: " + value);
        }
    }

    private void validateBeforeProcessing(Path inputPath, Path outputFolder) {
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("A entrada nao existe: " + inputPath);
        }

        if (Files.isRegularFile(inputPath) && !hasHtmlExtension(inputPath)) {
            throw new IllegalArgumentException("O arquivo de entrada deve ter extensao .html ou .htm: " + inputPath);
        }

        if (!Files.isRegularFile(inputPath) && !Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("A entrada deve ser um arquivo HTML ou uma pasta: " + inputPath);
        }

        if (Files.exists(outputFolder) && !Files.isDirectory(outputFolder)) {
            throw new IllegalArgumentException("A saida deve ser uma pasta: " + outputFolder);
        }

        Path normalizedInput = inputPath.toAbsolutePath().normalize();
        Path normalizedOutput = outputFolder.toAbsolutePath().normalize();

        if (normalizedOutput.equals(normalizedInput)) {
            throw new IllegalArgumentException("A pasta de saida nao pode ser igual a entrada.");
        }

        if (Files.isDirectory(inputPath) && normalizedOutput.startsWith(normalizedInput)) {
            throw new IllegalArgumentException("A pasta de saida nao pode estar dentro da pasta de entrada.");
        }

        if (Files.isRegularFile(inputPath) && normalizedOutput.resolve(inputPath.getFileName()).equals(normalizedInput)) {
            throw new IllegalArgumentException("A saida nao pode sobrescrever o arquivo original.");
        }
    }

    private void showValidationError(String message) {
        appendLog("Erro de validacao: " + message);
        JOptionPane.showMessageDialog(this, message, "Validacao", JOptionPane.WARNING_MESSAGE);
    }

    private void showDropError(String message) {
        appendLog("Erro ao arrastar e soltar: " + message);
        JOptionPane.showMessageDialog(this, message, "Arrastar e soltar", JOptionPane.WARNING_MESSAGE);
    }

    private void setProcessingState(boolean processing) {
        inputFolderField.setEnabled(!processing);
        outputFolderField.setEnabled(!processing);
        inputBrowseButton.setEnabled(!processing);
        outputBrowseButton.setEnabled(!processing);
        startButton.setEnabled(!processing);
        startButton.setText(processing ? PROCESSING_BUTTON_TEXT : START_BUTTON_TEXT);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatDuration(Duration duration) {
        double totalSeconds = Math.max(0, duration.toMillis()) / 1000.0;
        if (totalSeconds < 60) {
            return String.format(Locale.ROOT, "%.1fs", totalSeconds);
        }

        long seconds = Math.max(0, duration.toSeconds());
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes + "min " + remainingSeconds + "s";
    }

    private String formatMegabytes(long sizeBytes) {
        return String.format(Locale.ROOT, "%.1f MB", sizeBytes / 1024.0 / 1024.0);
    }

    private String displayName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    private boolean hasHtmlExtension(Path file) {
        Path fileName = file.getFileName();
        if (fileName == null) {
            return false;
        }

        String lowerCaseName = fileName.toString().toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".html") || lowerCaseName.endsWith(".htm");
    }

    private void appendLog(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendLog(message));
            return;
        }

        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
