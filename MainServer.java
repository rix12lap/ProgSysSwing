import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.HashSet;

public class MainServer {
    private static Properties config;
    private static final int MAIN_SERVER_PORT;
    private static final String[] SUBSERVER_HOSTS;
    private static final int[] SUBSERVER_PORTS;
    private static final String STORAGE_PATH;

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MAIN_SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
        SUBSERVER_HOSTS = config.getProperty("subservers.hosts", "localhost:5001,localhost:5002,localhost:5003").split(",");
        SUBSERVER_PORTS = new int[SUBSERVER_HOSTS.length];
        for (int i = 0; i < SUBSERVER_HOSTS.length; i++) {
            SUBSERVER_PORTS[i] = Integer.parseInt(SUBSERVER_HOSTS[i].split(":")[1]);
        }
        STORAGE_PATH = config.getProperty("server.storage.path", 
            System.getProperty("user.home") + File.separator + "ServerFile" + File.separator);
    }

    public static void main(String[] args) {
        try {
            // Créer les répertoires pour chaque sous-serveur
            for (int port : SUBSERVER_PORTS) {
                Path subServerPath = Paths.get(STORAGE_PATH + "SubServer" + port);
                Files.createDirectories(subServerPath);
                System.out.println("Répertoire créé : " + subServerPath);
            }

            // Créer le répertoire principal
            Path mainStoragePath = Paths.get(STORAGE_PATH);
            Files.createDirectories(mainStoragePath);
            System.out.println("Répertoire principal créé : " + mainStoragePath);

            ServerSocket serverSocket = new ServerSocket(MAIN_SERVER_PORT);
            System.out.println("Main Server en attente de connexions sur le port " + MAIN_SERVER_PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client depuis : " + 
                    clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                String command = dis.readUTF();
                System.out.println("Commande reçue du client : " + command);

                switch (command) {
                    case "SEND_FILE":
                        receiveFile(dis);
                        break;
                    case "RETRIEVE_FILE":
                        String fileName = dis.readUTF();
                        sendFileToClient(fileName, dos);
                        break;
                    case "LIST_FILES":
                        listFiles(dos);
                        break;
                    case "DELETE_FILE":
                        deleteFile(dis, dos);
                        break;
                    default:
                        System.err.println("Commande inconnue reçue : " + command);
                }
            } catch (IOException e) {
                System.err.println("Erreur avec le client : " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void listFiles(DataOutputStream dos) throws IOException {
            HashSet<String> uniqueFiles = new HashSet<>();
            
            // Parcourir tous les sous-serveurs
            for (int port : SUBSERVER_PORTS) {
                File subServerDir = new File(STORAGE_PATH + "SubServer" + port);
                if (subServerDir.exists() && subServerDir.isDirectory()) {
                    File[] files = subServerDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().contains("_part1")) {
                                // Extraire le nom original du fichier
                                String originalName = file.getName().substring(0, file.getName().indexOf("_part1"));
                                uniqueFiles.add(originalName);
                            }
                        }
                    }
                }
            }

            // Envoyer le nombre de fichiers
            dos.writeInt(uniqueFiles.size());
            System.out.println("Envoi de la liste des fichiers (" + uniqueFiles.size() + " fichiers)");
            
            // Envoyer chaque nom de fichier
            for (String fileName : uniqueFiles) {
                dos.writeUTF(fileName);
                System.out.println("- " + fileName);
            }
        }

        private void deleteFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            System.out.println("Demande de suppression du fichier : " + fileName);
            boolean success = true;

            // Supprimer toutes les parties du fichier sur tous les sous-serveurs
            for (int port : SUBSERVER_PORTS) {
                String subServerPath = STORAGE_PATH + "SubServer" + port + File.separator;
                for (int i = 1; i <= 3; i++) {
                    File partFile = new File(subServerPath + fileName + "_part" + i);
                    if (partFile.exists()) {
                        if (partFile.delete()) {
                            System.out.println("Partie " + i + " supprimée sur le sous-serveur " + port);
                        } else {
                            success = false;
                            System.err.println("Échec de la suppression de la partie " + i + " sur le sous-serveur " + port);
                        }
                    }
                }
            }

            dos.writeBoolean(success);
            System.out.println("Suppression " + (success ? "réussie" : "échouée") + " pour " + fileName);
        }

        private void receiveFile(DataInputStream dis) throws IOException {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            System.out.println("Réception du fichier : " + fileName + " (" + fileSize + " bytes)");

            byte[] buffer = new byte[1024];
            int partSize = (int) Math.ceil(fileSize / 3.0);

            for (int i = 0; i < 3; i++) {
                String partFileName = fileName + "_part" + (i + 1);
                String subServerStoragePath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;

                // Créer le répertoire du sous-serveur s'il n'existe pas
                Files.createDirectories(Paths.get(subServerStoragePath));

                try (FileOutputStream fos = new FileOutputStream(subServerStoragePath + partFileName)) {
                    int bytesRead;
                    int totalBytesRead = 0;
                    int remainingBytes = (i == 2) ?
                            (int) (fileSize - (partSize * 2)) :
                            partSize;

                    while (totalBytesRead < remainingBytes &&
                            (bytesRead = dis.read(buffer, 0, Math.min(buffer.length, remainingBytes - totalBytesRead))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    System.out.println("Partie " + (i + 1) + " (" + totalBytesRead + " bytes) sauvegardée dans " + subServerStoragePath);
                }
            }

            System.out.println("Fichier " + fileName + " complètement reçu et distribué");
        }

        private void sendFileToClient(String fileName, DataOutputStream dos) throws IOException {
            System.out.println("Demande de récupération du fichier : " + fileName);
            File tempFile = new File(STORAGE_PATH + fileName);

            // Vérifier si toutes les parties existent
            boolean allPartsExist = true;
            for (int i = 0; i < SUBSERVER_PORTS.length; i++) {
                String partFileName = fileName + "_part" + (i + 1);
                String subServerPath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;
                File partFile = new File(subServerPath + partFileName);
                if (!partFile.exists()) {
                    allPartsExist = false;
                    System.err.println("Partie manquante : " + partFileName);
                    break;
                }
            }

            dos.writeBoolean(allPartsExist);
            if (!allPartsExist) {
                System.err.println("Impossible de récupérer le fichier : parties manquantes");
                return;
            }

            // Réassembler le fichier
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                for (int i = 0; i < SUBSERVER_PORTS.length; i++) {
                    String partFileName = fileName + "_part" + (i + 1);
                    String subServerPath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;
                    File partFile = new File(subServerPath + partFileName);

                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            // Envoyer le fichier reconstitué au client
            dos.writeLong(tempFile.length());
            try (FileInputStream fis = new FileInputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            // Supprimer le fichier temporaire
            tempFile.delete();
            System.out.println("Fichier " + fileName + " envoyé au client avec succès");
        }
    }
}