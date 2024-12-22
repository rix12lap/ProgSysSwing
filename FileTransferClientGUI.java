import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class FileTransferClientGUI extends JFrame {
    private JTextField filePathField;
    private JButton selectFileButton;
    private JButton sendFileButton;
    private JTextArea statusArea;
    private JButton viewHistoryButton;
    private Client client;
    private FileTransferHistory fileHistory;

    public FileTransferClientGUI() {
        // Initialisation du look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        client = new Client();
        fileHistory = new FileTransferHistory(client);

        // Configuration de la fenêtre
        setTitle("File Transfer Client");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Panneau principal avec fond dégradé
        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                Color color1 = new Color(240, 240, 255);
                Color color2 = new Color(220, 220, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(contentPane);

        // Panneau pour envoyer des fichiers
        JPanel sendPanel = createSendPanel();

        // Ajouter le panneau au contenu principal
        contentPane.add(sendPanel, BorderLayout.CENTER);
    }

    private JPanel createSendPanel() {
        JPanel sendPanel = new JPanel(new BorderLayout(10, 10));
        sendPanel.setOpaque(false);

        // Sélection de fichier
        JPanel fileSelectionPanel = new JPanel(new BorderLayout(10, 10));
        filePathField = new JTextField();
        filePathField.setFont(new Font("Arial", Font.PLAIN, 14));
        filePathField.setEditable(false);

        selectFileButton = new JButton("Select File");
        styleButton(selectFileButton);

        fileSelectionPanel.add(new JLabel("Selected File:", SwingConstants.LEFT), BorderLayout.WEST);
        fileSelectionPanel.add(filePathField, BorderLayout.CENTER);
        fileSelectionPanel.add(selectFileButton, BorderLayout.EAST);

        // Boutons d'envoi et d'historique
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setOpaque(false);

        sendFileButton = new JButton("Send File");
        viewHistoryButton = new JButton("View Transfer History");

        styleButton(sendFileButton);
        styleButton(viewHistoryButton);

        buttonsPanel.add(sendFileButton);
        buttonsPanel.add(viewHistoryButton);

        // Zone de statut
        statusArea = new JTextArea();
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        statusArea.setEditable(false);
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setPreferredSize(new Dimension(500, 150));

        // Assemblage du panneau d'envoi
        sendPanel.add(fileSelectionPanel, BorderLayout.NORTH);
        sendPanel.add(buttonsPanel, BorderLayout.CENTER);
        sendPanel.add(scrollPane, BorderLayout.SOUTH);

        // Écouteurs d'événements pour envoi de fichiers
        selectFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        sendFileButton.addActionListener(e -> {
            String filePath = filePathField.getText();
            if (filePath.isEmpty()) {
                showErrorDialog("Please select a file first");
                return;
            }

            try {
                boolean success = client.sendFile(filePath);
                File sentFile = new File(filePath);

                if (success) {
                    fileHistory.addFileTransfer(sentFile.getName(), filePath);
                    statusArea.append("File sent successfully: " + sentFile.getName() + "\n");
                    showSuccessDialog("File sent successfully!");
                } else {
                    showErrorDialog("Failed to send file.");
                }
            } catch (Exception ex) {
                statusArea.append("Error sending file: " + ex.getMessage() + "\n");
                showErrorDialog("Error sending file: " + ex.getMessage());
            }
        });

        viewHistoryButton.addActionListener(e ->
                fileHistory.showHistoryDialog(this)
        );

        return sendPanel;
    }

    // Méthodes de style et de dialogue précédentes
    private void styleButton(JButton button) {
        // Implémentation identique à la version précédente
    }

    private void showErrorDialog(String message) {
        // Implémentation identique à la version précédente
    }

    private void showSuccessDialog(String message) {
        // Implémentation identique à la version précédente
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileTransferClientGUI gui = new FileTransferClientGUI();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}