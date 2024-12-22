import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashSet;
import java.util.Properties;

public class FileTransferHistory {
    private List<FileTransfer> transfers;
    private Client client;
    private static Properties config;
    private static final String SERVER_HOST;
    private static final int SERVER_PORT;

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        SERVER_HOST = config.getProperty("main.server.host", "localhost");
        SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
    }

    public FileTransferHistory(Client client) {
        this.transfers = new ArrayList<>();
        this.client = client;
    }

    public void addFileTransfer(String fileName, String filePath) {
        FileTransfer transfer = new FileTransfer(fileName, filePath, new Date());
        transfers.add(transfer);
    }

    // Add the missing showHistoryDialog method
    public void showHistoryDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "File Transfer History", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(parent);

        // Create table model with columns
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("File Name");
        model.addColumn("Path");
        model.addColumn("Transfer Date");
        model.addColumn("Available on Server");

        // Create table
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Style the table header
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(240, 240, 250));
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Center align the "Available on Server" column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton refreshButton = new JButton("Refresh");
        JButton downloadButton = new JButton("Download Selected");
        JButton deleteButton = new JButton("Delete from Server");
        JButton closeButton = new JButton("Close");

        // Style buttons
        for (JButton button : new JButton[]{refreshButton, downloadButton, deleteButton, closeButton}) {
            button.setFont(button.getFont().deriveFont(Font.BOLD));
            button.setBackground(new Color(240, 240, 250));
            button.setFocusPainted(false);
        }

        buttonsPanel.add(refreshButton);
        buttonsPanel.add(downloadButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(closeButton);

        // Add components to dialog
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Add button listeners
        refreshButton.addActionListener(e -> updateTableData(model));
        
        downloadButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String fileName = (String) model.getValueAt(selectedRow, 0);
                client.retrieveFile(fileName);
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Please select a file to download",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String fileName = (String) model.getValueAt(selectedRow, 0);
                if (deleteFileFromServer(fileName)) {
                    updateTableData(model);
                    JOptionPane.showMessageDialog(dialog,
                        "File deleted successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog,
                        "Failed to delete file",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Please select a file to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        // Initial table update
        updateTableData(model);

        // Show dialog
        dialog.setVisible(true);
    }

    private HashSet<String> getAvailableFiles() {
        HashSet<String> availableFiles = new HashSet<>();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST_FILES");
            int fileCount = dis.readInt();
            
            for (int i = 0; i < fileCount; i++) {
                String fileName = dis.readUTF();
                availableFiles.add(fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error connecting to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
        }
        return availableFiles;
    }

    private boolean deleteFileFromServer(String fileName) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DELETE_FILE");
            dos.writeUTF(fileName);

            return dis.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateTableData(DefaultTableModel model) {
        model.setRowCount(0);
        HashSet<String> availableFiles = getAvailableFiles();

        for (FileTransfer transfer : transfers) {
            Object[] row = {
                transfer.getFileName(),
                transfer.getFilePath(),
                transfer.getTransferDate().toString(),
                availableFiles.contains(transfer.getFileName()) ? "Yes" : "No"
            };
            model.addRow(row);
        }

        for (String fileName : availableFiles) {
            boolean exists = false;
            for (FileTransfer transfer : transfers) {
                if (transfer.getFileName().equals(fileName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Object[] row = {
                    fileName,
                    "Server Storage",
                    "Unknown",
                    "Yes"
                };
                model.addRow(row);
            }
        }
    }

    private static class FileTransfer {
        private String fileName;
        private String filePath;
        private Date transferDate;

        public FileTransfer(String fileName, String filePath, Date transferDate) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.transferDate = transferDate;
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public Date getTransferDate() { return transferDate; }
    }
}