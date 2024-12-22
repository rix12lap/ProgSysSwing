//import java.io.*;
//import java.net.Socket;
//
//public class Client {
//    private static final String SERVER_HOST = "localhost";
//    private static final int SERVER_PORT = 5000;
//    private static final String[] SUBSERVER_HOSTS = {"localhost", "localhost", "localhost"};
//    private static final int[] SUBSERVER_PORTS = {5001, 5002, 5003};
//    private static final String BASE_PATH = "C:\\Users\\squilacci\\Documents\\ServerFile\\";
//
//    // Méthode pour envoyer un fichier au serveur principal
//    public void sendFile(String filePath) {
//        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
//             FileInputStream fis = new FileInputStream(filePath);
//             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
//
//            File file = new File(filePath);
//            String fileName = file.getName();
//            long fileSize = file.length();
//
//            // Envoyer le nom et la taille du fichier
//            dos.writeUTF(fileName);
//            dos.writeLong(fileSize);
//
//            // Envoyer le contenu du fichier
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                dos.write(buffer, 0, bytesRead);
//            }
//
//            System.out.println("Fichier envoyé avec succès");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Méthode pour récupérer un fichier perdu
//    public void retrieveFile(String fileName) {
//        try {
//            // Créer un fichier de sortie dans le répertoire de base
//            try (FileOutputStream fos = new FileOutputStream(BASE_PATH + fileName)) {
//                // Récupérer les parties depuis les sous-serveurs
//                for (int i = 0; i < 3; i++) {
//                    String partFileName = fileName + "_part" + (i+1);
//                    String subServerStoragePath = "C:\\Users\\squilacci\\Documents\\ServerFile\\SubServer" + SUBSERVER_PORTS[i] + "\\";
//
//                    try (Socket subServerSocket = new Socket(SUBSERVER_HOSTS[i], SUBSERVER_PORTS[i]);
//                         DataOutputStream dos = new DataOutputStream(subServerSocket.getOutputStream());
//                         DataInputStream dis = new DataInputStream(subServerSocket.getInputStream())) {
//
//                        // Demander explicitement la récupération de la partie du fichier
//                        dos.writeUTF("RETRIEVE_PART");
//                        dos.writeUTF(partFileName);
//                        dos.flush();
//
//                        // Vérifier si la partie existe
//                        boolean partExists = dis.readBoolean();
//                        if (!partExists) {
//                            System.out.println("Partie de fichier manquante : " + partFileName);
//                            return;
//                        }
//
//                        // Recevoir et écrire la partie
//                        byte[] buffer = new byte[1024];
//                        int bytesRead;
//                        while ((bytesRead = dis.read(buffer)) != -1) {
//                            fos.write(buffer, 0, bytesRead);
//                        }
//                    }
//                }
//
//                System.out.println("Fichier reconstitué avec succès");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void main(String[] args) {
//        ClientBack client = new ClientBack();
//
//        // Exemple d'utilisation
////         client.sendFile("C:\\Users\\squilacci\\Documents\\Teste.txt");
//        client.retrieveFile("Teste.txt");
//    }
//}