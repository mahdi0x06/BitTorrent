package tracker.controllers;

import common.models.Message;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.*;

public class TrackerConnectionController {

    public static Message handleCommand(Message message) {
        if (message.getType() != Message.Type.file_request) {
            return null;
        }

        String requestedName = message.getFromBody("name");
        Map<String, String> matchingPeers = new HashMap<>();
        for (PeerConnectionThread conn : TrackerApp.getConnections()) {
            Map<String, String> files = conn.getFileAndHashes();
            if (files.containsKey(requestedName)) {
                matchingPeers.put(conn.getOtherSideIP() + ":" + conn.getOtherSidePort(), files.get(requestedName));
            }
        }

        if (matchingPeers.isEmpty()) {
            return errorResponse("not_found");
        }

        Set<String> hashes = new HashSet<>(matchingPeers.values());
        if (hashes.size() > 1) {
            return errorResponse("multiple_hash");
        }

        List<String> peerAddresses = new ArrayList<>(matchingPeers.keySet());
        Collections.shuffle(peerAddresses);
        String selectedPeer = peerAddresses.get(0);
        String[] parts = selectedPeer.split(":");

        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        String md5 = matchingPeers.get(selectedPeer);
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "peer_found");
        body.put("peer_have", ip);
        body.put("peer_port", port);
        body.put("md5", md5);

        return new Message(body, Message.Type.response);
    }

    private static Message errorResponse(String errorType) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "error");
        body.put("error", errorType);
        return new Message(body, Message.Type.response);
    }

    public static Map<String, List<String>> getSends(PeerConnectionThread connection) {
        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("command", "get_sends");
            Message request = new Message(body, Message.Type.command);
            Message response = connection.sendAndWaitForResponse(request, TrackerApp.TIMEOUT_MILLIS);

            if (response != null && "ok".equals(response.getFromBody("response"))) {
                return response.getFromBody("sent_files");
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<String>> getReceives(PeerConnectionThread connection) {
        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("command", "get_receives");
            Message request = new Message(body, Message.Type.command);
            Message response = connection.sendAndWaitForResponse(request, TrackerApp.TIMEOUT_MILLIS);

            if (response != null && "ok".equals(response.getFromBody("response"))) {
                return response.getFromBody("received_files");
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyMap();
    }
}
