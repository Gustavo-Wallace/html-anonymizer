package br.com.estagio.anonymizer.ui;

import br.com.estagio.anonymizer.file.FolderProcessingResult;
import br.com.estagio.anonymizer.file.FolderProcessor;

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
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class MainWindow extends JFrame {
    private final JTextField inputFolderField = new JTextField();
    private final JTextField outputFolderField = new JTextField();
    private final JButton startButton = new JButton("Iniciar anonimizacao");
    private final JTextArea logArea = new JTextArea();
    private final FolderProcessor folderProcessor;

    public MainWindow() {
        this(new FolderProcessor());
    }

    MainWindow(FolderProcessor folderProcessor) {
        super("HTML Anonymizer");
        this.folderProcessor = folderProcessor;
        configureWindow();
        configureActions();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(createFormPanel(), BorderLayout.NORTH);
        root.add(createLogPanel(), BorderLayout.CENTER);
        root.add(startButton, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, constraints, 0, "Pasta de entrada:", inputFolderField, "Selecionar...", true);
        addRow(panel, constraints, 1, "Pasta de saida:", outputFolderField, "Selecionar...", false);

        return panel;
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            JTextField textField,
            String buttonText,
            boolean inputFolder
    ) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(textField, constraints);

        JButton browseButton = new JButton(buttonText);
        browseButton.addActionListener(event -> chooseDirectory(textField, inputFolder ? "Selecionar pasta de entrada" : "Selecionar pasta de saida"));

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
        startButton.addActionListener(event -> startProcessing());
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

    private void startProcessing() {
        logArea.setText("");

        Path inputFolder;
        Path outputFolder;
        try {
            inputFolder = readPath(inputFolderField, "Informe a pasta de entrada.");
            outputFolder = readPath(outputFolderField, "Informe a pasta de saida.");
        } catch (IllegalArgumentException exception) {
            appendLog("Erro: " + exception.getMessage());
            return;
        }

        startButton.setEnabled(false);

        SwingWorker<FolderProcessingResult, Void> worker = new SwingWorker<>() {
            @Override
            protected FolderProcessingResult doInBackground() throws Exception {
                appendLog("Inicio do processamento.");
                appendLog("Pasta de entrada: " + inputFolder);
                appendLog("Pasta de saida: " + outputFolder);
                return folderProcessor.processFolder(inputFolder, outputFolder);
            }

            @Override
            protected void done() {
                try {
                    FolderProcessingResult result = get();
                    appendLog("Arquivos HTML encontrados: " + result.getHtmlFilesFound());
                    appendLog("Arquivos processados: " + result.getFilesProcessed());
                    appendLog("Processamento concluido.");
                    JOptionPane.showMessageDialog(MainWindow.this, "Processamento concluido.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    appendLog("Erro: processamento interrompido.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    appendLog("Erro: " + cause.getMessage());
                } finally {
                    startButton.setEnabled(true);
                }
            }
        };

        worker.execute();
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

    private void appendLog(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendLog(message));
            return;
        }

        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
