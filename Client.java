import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class Client {
    private static Properties config;

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String SERVER_HOST = config.getProperty("main.server.host", "localhost");
    private static final int SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
    private static final String[] SUBSERVER_HOSTS = config.getProperty("subservers.hosts", "localhost,localhost,localhost").split(",");
    private static final int[] SUBSERVER_PORTS = {
            Integer.parseInt(SUBSERVER_HOSTS[0].split(":")[1]),
            Integer.parseInt(SUBSERVER_HOSTS[1].split(":")[1]),
            Integer.parseInt(SUBSERVER_HOSTS[2].split(":")[1])
    };

    private static final String BASE_PATH = config.getProperty("server.storage.path", System.getProperty("user.home") + File.separator + "ServerFile" + File.separator);
    private static final String DOWNLOAD_PATH = config.getProperty("download.path", System.getProperty("user.home") + File.separator + "Downloads" + File.separator);

    public boolean sendFile(String filePath) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(filePath)) {

            File file = new File(filePath);
            String fileName = file.getName();
            long fileSize = file.length();

            dos.writeUTF("SEND_FILE");
            dos.writeUTF(fileName);
            dos.writeLong(fileSize);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            System.out.println("Fichier envoyé avec succès");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void retrieveFile(String fileName) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Demander la récupération du fichier au serveur principal
            dos.writeUTF("RETRIEVE_FILE");
            dos.writeUTF(fileName);
            dos.flush();

            // Vérifier si le fichier existe et peut être récupéré
            boolean fileExists = dis.readBoolean();
            if (!fileExists) {
                System.out.println("Le fichier " + fileName + " n'existe pas sur le serveur.");
                return;
            }

            // Lire la taille du fichier
            long fileSize = dis.readLong();

            // Créer le chemin complet de téléchargement
            File downloadFile = new File(DOWNLOAD_PATH + fileName);
            try (FileOutputStream fos = new FileOutputStream(downloadFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize &&
                        (bytesRead = dis.read(buffer, 0,
                                (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                System.out.println("Fichier " + fileName + " téléchargé avec succès dans " + downloadFile.getAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la récupération du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        // Exemple d'utilisation
        // client.sendFile("/chemin/vers/votre/fichier");
        // client.retrieveFile("nom_du_fichier");
    }
}