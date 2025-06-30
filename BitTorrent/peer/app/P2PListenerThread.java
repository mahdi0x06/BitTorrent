package peer.app;

import common.models.Message;
import common.utils.FileUtils;
import common.utils.JSONUtils;
import common.utils.MD5Hash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2PListenerThread extends Thread {
    private final ServerSocket serverSocket;

    public P2PListenerThread(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    private void handleConnection(Socket socket) throws Exception {
        try {//TODO watch out the jsons
            socket.setSoTimeout(TIMEOUT_MILLIS);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            String json = reader.readLine();
            if (json == null) {
                socket.close();
                return;
            }

            Message message = JSONUtils.fromJson(json);
            if (message == null || !message.getType().equals(Message.Type.download_request)) {
                socket.close();
                return;
            }

            String filename = message.getFromBody("name");
            String md5 = message.getFromBody("md5");
            String receiverIp = message.getFromBody("receiver_ip");
            int receiverPort = message.getIntFromBody("receiver_port");

            File file = new File(PeerApp.getSharedFolderPath(), filename);
            if (!file.exists() || !file.isFile()) {
                socket.close();
                return;
            }

            String fileMd5 = MD5Hash.HashFile(file.getAbsolutePath());
            if (!md5.equals(fileMd5)) {
                socket.close();
                return;
            }

            String receiver = receiverIp + ":" + receiverPort;
            TorrentP2PThread torrentThread = new TorrentP2PThread(socket, file, receiver);
            PeerApp.addTorrentP2PThread(torrentThread);
            torrentThread.start();

        } catch (Exception e) {
            socket.close();
        }
    }

    @Override
    public void run() {
        while (!PeerApp.isEnded()) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (Exception e) {
                break;
            }
        }

        try {
            serverSocket.close();
        } catch (Exception ignored) {
        }
    }
}
