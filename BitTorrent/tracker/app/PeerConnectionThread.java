package tracker.app;

import common.models.ConnectionThread;
import common.models.Message;
import common.utils.JSONUtils;
import tracker.controllers.TrackerConnectionController;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class PeerConnectionThread extends ConnectionThread {
    private String peerKey;
    private Map<String, String> fileAndHashes = new HashMap<>();

    public PeerConnectionThread(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public boolean initialHandshake() {
        try {
            socket.setSoTimeout(TIMEOUT_MILLIS);

            HashMap<String, Object> statusBody = new HashMap<>();
            statusBody.put("command", "status");
            Message statusRequestMsg = new Message(statusBody, Message.Type.command);
            sendMessage(statusRequestMsg);

            String receivedPeerStatusResponseStr = dataInputStream.readUTF();
            Message peerStatusResponse = JSONUtils.fromJson(receivedPeerStatusResponseStr);
            if (peerStatusResponse == null || !"ok".equals(peerStatusResponse.getFromBody("response"))) {
                throw new RuntimeException();
            }

            String ip = peerStatusResponse.getFromBody("peer");
            int port = peerStatusResponse.getIntFromBody("listen_port");
            this.peerKey = ip + ":" + port;
            this.setOtherSideIP(ip);
            this.setOtherSidePort(port);

            HashMap<String, Object> fileListBody = new HashMap<>();
            fileListBody.put("command", "get_files_list");
            Message fileListRequest = new Message(fileListBody, Message.Type.command);
            sendMessage(fileListRequest);

            String receivedPeerFileListResponseStr = dataInputStream.readUTF();
            Message peerFileListResponse = JSONUtils.fromJson(receivedPeerFileListResponseStr);

            if (peerFileListResponse == null || !"ok".equals(peerFileListResponse.getFromBody("response"))) {
                throw new RuntimeException();
            }

            fileAndHashes.clear();
            Map<String, Object> files = peerFileListResponse.getFromBody("files");
            if (files != null) {
                for (Map.Entry<String, Object> entry : files.entrySet()) {
                    fileAndHashes.put(entry.getKey(), entry.getValue().toString());
                }
            }

            socket.setSoTimeout(0);

            TrackerApp.addPeerConnection(this);
            return true;
        } catch (Exception e) {
            System.err.println("Inital HandShake failed with remote device.");
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException ignored) {}
            return false;
        }
    }

    public void refreshStatus() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        Message statusMsg = new Message(body, Message.Type.command);
        Message response = sendAndWaitForResponse(statusMsg, TIMEOUT_MILLIS);
        if (response == null) {
            System.err.println("Request Timed out.");
            throw new RuntimeException("Invalid status response");
        }
        String ip = response.getFromBody("peer");
        int port = response.getIntFromBody("listen_port");
        this.peerKey = ip + ":" + port;
        this.setOtherSideIP(ip);
        this.setOtherSidePort(port);
    }

    public void refreshFileList() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        Message request = new Message(body, Message.Type.command);
        Message response = sendAndWaitForResponse(request, TIMEOUT_MILLIS);
        if (response == null) {
            System.err.println("Request Timed out.");
            throw new RuntimeException("Null file list response from peer");
        }

        fileAndHashes.clear();
        Map<String, Object> files = (Map<String, Object>) response.getFromBody("files");
        if (files != null) {
            for (Map.Entry<String, Object> entry : files.entrySet()) {
                fileAndHashes.put(entry.getKey(), entry.getValue().toString());
            }
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
