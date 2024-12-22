//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class SubServer {
//    private int port;
//    private String storagePath;
//
//    public SubServer(int port) {
//        this.port = port;
//        // Utiliser le port dans le chemin de stockage
//        this.storagePath = "C:\\Users\\squilacci\\Documents\\ServerFile\\SubServer" + port + "\\";
//    }
//
//    public void start() {
//        try {
//            // Créer le répertoire du sous-serveur
//            Files.createDirectories(Paths.get(storagePath));
//
//            ServerSocket serverSocket = new ServerSocket(port);
//            System.out.println("Sous-serveur sur le port " + port + " en attente...");
//
//            while (true) {
//                Socket socket = serverSocket.accept();
//                new Thread(new FileReceiver(socket, storagePath)).start();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Déplacez la méthode main ici, à l'extérieur de FileReceiver
//    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Veuillez spécifier un port pour le sous-serveur");
//            return;
//        }
//        int port = Integer.parseInt(args[0]);
//        new SubServer(port).start();
//    }
//
//    private static class FileReceiver implements Runnable {
//        private Socket socket;
//        private String storagePath;
//
//        public FileReceiver(Socket socket, String storagePath) {
//            this.socket = socket;
//            this.storagePath = storagePath;
//        }
//
//        @Override
//        public void run() {
//            try {
//                // Le reste du code reste le même
//                DataInputStream dis = new DataInputStream(socket.getInputStream());
//                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//
//                String command = dis.readUTF();
//
//                if ("RETRIEVE_PART".equals(command)) {
//                    String fileName = dis.readUTF();
//                    File partFile = new File(storagePath + fileName);
//
//                    // Indiquer si le fichier existe
//                    dos.writeBoolean(partFile.exists());
//
//                    if (partFile.exists()) {
//                        try (FileInputStream fis = new FileInputStream(partFile)) {
//                            byte[] buffer = new byte[1024];
//                            int bytesRead;
//                            while ((bytesRead = fis.read(buffer)) != -1) {
//                                dos.write(buffer, 0, bytesRead);
//                            }
//                        }
//                    }
//                }
//
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}