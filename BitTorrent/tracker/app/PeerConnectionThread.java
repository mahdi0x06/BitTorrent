package tracker.app;

import common.models.ConnectionThread;
import common.models.Message;
import common.utils.JSONUtils;
import tracker.controllers.TrackerConnectionController;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class PeerConnectionThread extends ConnectionThread {
    private String peerKey;
    private BufferedReader in;
    private BufferedWriter out;
    private HashMap<String, String> fileAndHashes = new HashMap<>();

    public PeerConnectionThread(Socket socket) throws IOException {//TODO the exception
        super(socket);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.socket.setSoTimeout(TIMEOUT_MILLIS);
    }

    @Override
    public boolean initialHandshake() {
        try {
            refreshStatus();
            refreshFileList();
            TrackerApp.addPeerConnection(this);
            return true;
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception ignored) {}
            return false;
        }
    }


    public void refreshStatus() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        Message statusMsg = new Message(body, Message.Type.command);
        sendMessage(statusMsg);

        String response = in.readLine();
        Message msg = JSONUtils.fromJson(response);
        if (msg == null) throw new RuntimeException("Invalid status response");

        String ip = msg.getFromBody("ip");
        int port = msg.getIntFromBody("port");
        this.peerKey = ip + ":" + port;
    }


    public void refreshFileList() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        Message getFilesMsg = new Message(body, Message.Type.command);
        sendMessage(getFilesMsg);

        String response = in.readLine();
        Message msg = JSONUtils.fromJson(response);

        if (msg == null) throw new RuntimeException("Null message from peer");

        fileAndHashes = new HashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) msg.getFromBody("files")).entrySet()) {//TODO watch out
            fileAndHashes.put(entry.getKey(), entry.getValue().toString());
        }
    }



    @Override
    protected boolean handleMessage(Message message) {
        if (message.getType() == Message.Type.file_request) {
            sendMessage(TrackerConnectionController.handleCommand(message));
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        super.run();
        TrackerApp.removePeerConnection(this);
    }

    public Map<String, String> getFileAndHashes() {
        return Map.copyOf(fileAndHashes);
    }
}
