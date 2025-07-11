package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import peer.app.PeerApp;
import peer.app.P2TConnectionThread;

import java.util.*;

public class P2TConnectionController {

    public static Message handleCommand(Message message) {
        String command = message.getFromBody("command");

        switch (command) {
            case "status":
                return status();
            case "get_files_list":
                return getFilesList();
            case "get_sends":
                return getSends();
            case "get_receives":
                return getReceives();
            default:
                HashMap<String, Object> err = new HashMap<>();
                err.put("response", "error");
                err.put("error", "unknown_command");
                return new Message(err, Message.Type.response);
        }
    }

    public static Message status() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        body.put("response", "ok");
        body.put("peer", PeerApp.getPeerIP());
        body.put("listen_port", PeerApp.getPeerPort());
        return new Message(body, Message.Type.response);
    }

    public static Message getFilesList() {
        Map<String, String> fileMap = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        body.put("response", "ok");
        body.put("files", fileMap);
        return new Message(body, Message.Type.response);
    }

    private static Message getSends() {
        Map<String, List<String>> sends = PeerApp.getSentFiles();
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_sends");
        body.put("response", "ok");
        body.put("sent_files", sends);
        return new Message(body, Message.Type.response);
    }


    private static Message getReceives() {
        Map<String, List<String>> receives = PeerApp.getReceivedFiles();
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_receives");
        body.put("response", "ok");
        body.put("received_files", receives);
        return new Message(body, Message.Type.response);
    }


    public static Message sendFileRequest(P2TConnectionThread tracker, String fileName) throws Exception {
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", fileName);
        Message request = new Message(requestBody, Message.Type.file_request);

        Message response = tracker.sendAndWaitForResponse(request, PeerApp.TIMEOUT_MILLIS);
        if (response == null) {
            throw new Exception("No response received");
        }
        return response;
    }
}
