import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class SubServer {
    private static Properties config;

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int port;
    private String storagePath;

    public SubServer(int port) {
        this.port = port;
        this.storagePath = config.getProperty("server.storage.path",
                System.getProperty("user.home") + File.separator + "ServerFile" +
                        File.separator + "SubServer" + port + File.separator);
    }

    public void start() {
        try {
            Files.createDirectories(Paths.get(storagePath));

            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Sous-serveur sur le port " + port + " en attente...");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new FileReceiver(socket, storagePath, port)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Veuillez spécifier un port pour le sous-serveur");
            return;
        }
        int port = Integer.parseInt(args[0]);
        new SubServer(port).start();
    }

    // La classe FileReceiver reste inchangée
    private static class FileReceiver implements Runnable {
        private Socket socket;
        private String storagePath;
        private int port;

        public FileReceiver(Socket socket, String storagePath, int port) {
            this.socket = socket;
            this.storagePath = storagePath;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                String command = dis.readUTF();

                if ("RETRIEVE_PART".equals(command)) {
                    String fileName = dis.readUTF();
                    File partFile = new File(storagePath + fileName);

                    // Indiquer si le fichier existe
                    dos.writeBoolean(partFile.exists());

                    if (partFile.exists()) {
                        System.out.println("Envoi de la partie : " + fileName + " du port " + port);
                        try (FileInputStream fis = new FileInputStream(partFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                dos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}