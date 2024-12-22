//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class MainServer {
//    private static final int MAIN_SERVER_PORT = 5000;
//    private static final String[] SUBSERVER_HOSTS = {"localhost", "localhost", "localhost"};
//    private static final int[] SUBSERVER_PORTS = {5001, 5002, 5003};
//    private static final String STORAGE_PATH = "C:\\Users\\squilacci\\Documents\\ServerFile\\";
//
//    public static void main(String[] args) {
//        try {
//            // Créer les répertoires pour chaque sous-serveur
//            for (int port : SUBSERVER_PORTS) {
//                Files.createDirectories(Paths.get(STORAGE_PATH + "SubServer" + port));
//            }
//
//            // Créer le répertoire principal
//            Files.createDirectories(Paths.get(STORAGE_PATH));
//
//            ServerSocket serverSocket = new ServerSocket(MAIN_SERVER_PORT);
//            System.out.println("Main Server en attente de connexions...");
//
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                new Thread(new ClientHandler(clientSocket)).start();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    static class ClientHandler implements Runnable {
//        private Socket clientSocket;
//
//        public ClientHandler(Socket socket) {
//            this.clientSocket = socket;
//        }
//
//        @Override
//        public void run() {
//            try {
//                // Recevoir le fichier du client
//                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
//                String fileName = dis.readUTF();
//                long fileSize = dis.readLong();
//
//                byte[] buffer = new byte[1024];
//                int partSize = (int) Math.ceil(fileSize / 3.0);
//
//                for (int i = 0; i < 3; i++) {
//                    String subServerStoragePath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + "\\";
//
//                    try (FileOutputStream fos = new FileOutputStream(subServerStoragePath + fileName + "_part" + (i+1))) {
//                        int bytesRead;
//                        int totalBytesRead = 0;
//                        int remainingBytes = (i == 2) ?
//                                (int)(fileSize - (partSize * 2)) :
//                                partSize;
//
//                        while (totalBytesRead < remainingBytes &&
//                                (bytesRead = dis.read(buffer, 0,
//                                        Math.min(buffer.length, remainingBytes - totalBytesRead))) != -1) {
//                            fos.write(buffer, 0, bytesRead);
//                            totalBytesRead += bytesRead;
//                        }
//
//                        // Envoyer la partie au sous-serveur correspondant
//                        sendToSubServer(fileName + "_part" + (i+1), i);
//                    }
//                }
//
//                System.out.println("Fichier " + fileName + " divisé et distribué");
//                clientSocket.close();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // Méthode ajoutée pour envoyer aux sous-serveurs
//        private void sendToSubServer(String partFileName, int serverIndex) {
//            try {
//                String subServerStoragePath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[serverIndex] + "\\";
//
//                try (Socket subServerSocket = new Socket(SUBSERVER_HOSTS[serverIndex], SUBSERVER_PORTS[serverIndex]);
//                     FileInputStream fis = new FileInputStream(subServerStoragePath + partFileName);
//                     DataOutputStream dos = new DataOutputStream(subServerSocket.getOutputStream())) {
//
//                    dos.writeUTF(partFileName);
//                    dos.writeLong(new File(subServerStoragePath + partFileName).length());
//
//                    byte[] buffer = new byte[1024];
//                    int bytesRead;
//                    while ((bytesRead = fis.read(buffer)) != -1) {
//                        dos.write(buffer, 0, bytesRead);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}