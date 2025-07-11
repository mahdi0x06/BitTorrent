package peer.app;

import common.models.ConnectionThread;
import common.models.Message;
import common.utils.JSONUtils;
import common.utils.MD5Hash;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerApp {
    public static final int TIMEOUT_MILLIS = 500;
    public static String peerIp;
    public static int peerPort;
    public static String trackerIp;
    public static int trackerPort;
    public static String repoPath;
    public static Map<String, List<String>> sentFiles = new ConcurrentHashMap<>();
    public static Map<String, List<String>> receivedFiles = new ConcurrentHashMap<>();
    private static boolean exitFlag = false;
    public static P2TConnectionThread trackerConnectionThread;
    public static P2PListenerThread peerListenerThread;
    public static final List<TorrentP2PThread> torrentThreads = Collections.synchronizedList(new ArrayList<>());

    public static boolean isEnded() {
        return exitFlag;
    }

    public static void initFromArgs(String[] args) throws Exception {
        String peerIpPort = args[0];
        String trackerIpPort = args[1];
        repoPath = args[2];
        String[] peerParts = peerIpPort.split(":");
        String[] trackerParts = trackerIpPort.split(":");
        peerIp = peerParts[0];
        peerPort = Integer.parseInt(peerParts[1]);
        trackerIp = trackerParts[0];
        trackerPort = Integer.parseInt(trackerParts[1]);

    }

    public static void endAll() {
        exitFlag = true;

        try {
            if (trackerConnectionThread != null) {
                trackerConnectionThread.end();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        synchronized (torrentThreads) {
            for (TorrentP2PThread thread : torrentThreads) {
                try {
                    thread.end();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            torrentThreads.clear();
        }

        sentFiles.clear();
        receivedFiles.clear();

        try {
            if (peerListenerThread != null && peerListenerThread.isAlive()) {
                peerListenerThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void connectTracker() {
        try {
            if (trackerConnectionThread == null || !trackerConnectionThread.isAlive()) {
                Socket socket = new Socket(trackerIp, trackerPort);
                trackerConnectionThread = new P2TConnectionThread(socket);
                trackerConnectionThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startListening() {
        try {
            if (peerListenerThread == null) {
                peerListenerThread = new P2PListenerThread(peerPort);
                new Thread(peerListenerThread).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        if (torrentP2PThread == null) return;

        synchronized (torrentThreads) {
            torrentThreads.remove(torrentP2PThread);
        }
    }

    public static void addTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        if (torrentP2PThread == null) return;

        synchronized (torrentThreads) {
            if (!torrentThreads.contains(torrentP2PThread)) {
                torrentThreads.add(torrentP2PThread);
            }
        }
    }

    public static String getSharedFolderPath() {
        return repoPath;
    }

    public static void addSentFile(String receiver, String fileNameAndHash) {
        sentFiles.computeIfAbsent(receiver, k -> new ArrayList<>());

        synchronized (sentFiles.get(receiver)) {
            if (!sentFiles.get(receiver).contains(fileNameAndHash)) {
                sentFiles.get(receiver).add(fileNameAndHash);
            }
        }
    }


    public static void addReceivedFile(String sender, String fileNameAndHash) {
        receivedFiles.computeIfAbsent(sender, k -> new ArrayList<>());

        synchronized (receivedFiles.get(sender)) {
            if (!receivedFiles.get(sender).contains(fileNameAndHash)) {
                receivedFiles.get(sender).add(fileNameAndHash);
            }
        }
    }


    public static String getPeerIP() {
        return peerIp;
    }

    public static int getPeerPort() {
        return peerPort;
    }

    public static Map<String, List<String>> getSentFiles() {
        return sentFiles;
    }

    public static Map<String, List<String>> getReceivedFiles() {
        return receivedFiles;
    }

    public static P2TConnectionThread getP2TConnection() {
        return trackerConnectionThread;
    }

    //public static boolean requestDownload(String ip, int port, String filename, String md5) {
//    try {
//        File file = new File(repoPath, filename);
//
//        HashMap<String, Object> body = new HashMap<>();
//        body.put("name", filename);
//        body.put("md5", md5);
//        body.put("receiver_ip", peerIp);
//        body.put("receiver_port", peerPort);
//
//        Message message = new Message(body, Message.Type.download_request);
//        String json = JSONUtils.toJson(message);
//
//        Socket socket = new Socket(ip, port);
//        socket.setSoTimeout(TIMEOUT_MILLIS);
//
//        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
//        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//
//        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
//        out.flush();
//
//        FileOutputStream fos = new FileOutputStream(file);
//        byte[] buffer = new byte[4096];
//        int bytesRead;
//        while ((bytesRead = in.read(buffer)) != -1) {
//            fos.write(buffer, 0, bytesRead);
//        }
//
//        fos.close();
//        in.close();
//        out.close();
//        socket.close();
//
//        return true;
//    } catch (Exception e) {
//        return false;
//    }
//}
    public static boolean requestDownload(String ip, int port, String filename, String md5) {
        File file = new File(repoPath, filename);
        try (
            Socket socket = new Socket(ip, port);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(file)
        ) {
            socket.setSoTimeout(TIMEOUT_MILLIS);

            HashMap<String, Object> body = new HashMap<>();
            body.put("name", filename);
            body.put("md5", md5);
            body.put("receiver_ip", peerIp);
            body.put("receiver_port", peerPort);
            Message message = new Message(body, Message.Type.download_request);
            String json = JSONUtils.toJson(message);

            out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            boolean receivedData = false;

            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                receivedData = true;
            }
            return receivedData;
        } catch (Exception e) {
            e.printStackTrace();
            file.delete();
            return false;
        }
    }


}
