package tracker.controllers;

import common.models.Message;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.*;

public class TrackerCLIController {

    public static String processCommand(String command) {
        if (command.equals("exit")) {
            return endProgram();
        } else if (command.equals("refresh_files")) {
            return refreshFiles();
        } else if (command.equals("reset_connections")) {
            return resetConnections();
        } else if (command.equals("list_peers")) {
            return listPeers();
        } else if (command.startsWith("list_files ")) {
            return listFiles(command);
        } else if (command.startsWith("get_sends ")) {
            return getSends(command);
        } else if (command.startsWith("get_receives ")) {
            return getReceives(command);
        } else {
            return "Invalid command.";
        }
    }

    private static String getReceives(String command) {
        String target = command.substring("get_receives ".length());
        String[] parts = target.split(":");
        if (parts.length != 2) return "Peer not found.";

        PeerConnectionThread conn = TrackerApp.getConnectionByIpPort(parts[0], Integer.parseInt(parts[1]));
        if (conn == null) return "Peer not found.";

        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("command", "get_receives");
            Message request = new Message(body, Message.Type.command);
            Message response = conn.sendAndWaitForResponse(request, TrackerApp.TIMEOUT_MILLIS);

            if (response == null || !"ok".equals(response.getFromBody("response"))) {
                return "No files received by " + target;
            }

            Map<String, List<String>> received = response.getFromBody("received_files");
            if (received == null || received.isEmpty()) {
                return "No files received by " + target;
            }

            List<String> output = new ArrayList<>();
            for (var entry : received.entrySet()) {
                for (String file : entry.getValue()) {
                    output.add(file + " - " + entry.getKey());
                }
            }

            output.sort(Comparator.naturalOrder());
            return String.join("\n", output);
        } catch (Exception e) {
            return "Peer not found.";
        }
    }


    private static String getSends(String command) {
        String target = command.substring("get_sends ".length());
        String[] parts = target.split(":");
        if (parts.length != 2) return "Peer not found.";

        PeerConnectionThread conn = TrackerApp.getConnectionByIpPort(parts[0], Integer.parseInt(parts[1]));
        if (conn == null) return "Peer not found.";

        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("command", "get_sends");
            Message request = new Message(body, Message.Type.command);
            Message response = conn.sendAndWaitForResponse(request, TrackerApp.TIMEOUT_MILLIS);

            if (response == null || !"ok".equals(response.getFromBody("response"))) {
                return "No files sent by " + target;
            }

            Map<String, List<String>> sent = response.getFromBody("sent_files");
            if (sent == null || sent.isEmpty()) {
                return "No files sent by " + target;
            }

            List<String> output = new ArrayList<>();
            for (var entry : sent.entrySet()) {
                for (String file : entry.getValue()) {
                    output.add(file + " - " + entry.getKey());
                }
            }

            output.sort(Comparator.naturalOrder());
            return String.join("\n", output);
        } catch (Exception e) {
            return "Peer not found.";
        }
    }


    private static String listFiles(String command) {
        String target = command.substring("list_files ".length());
        String[] parts = target.split(":");
        if (parts.length != 2) return "Peer not found.";

        PeerConnectionThread conn = TrackerApp.getConnectionByIpPort(parts[0], Integer.parseInt(parts[1]));
        if (conn == null) return "Peer not found.";

        Map<String, String> files = conn.getFileAndHashes();
        if (files == null || files.isEmpty()) return "Repository is empty.";

        List<String> lines = new ArrayList<>();
        for (var entry : files.entrySet()) {
            lines.add(entry.getKey() + " " + entry.getValue());
        }

        lines.sort(Comparator.naturalOrder());
        return String.join("\n", lines);
    }

    private static String listPeers() {
        List<PeerConnectionThread> peers = TrackerApp.getConnections();
        if (peers.isEmpty()) return "No peers connected.";

        List<String> ips = new ArrayList<>();
        for (PeerConnectionThread peer : peers) {
            ips.add(peer.getOtherSideIP() + ":" + peer.getOtherSidePort());
        }

        ips.sort(Comparator.naturalOrder());
        return String.join("\n", ips);
    }

    private static String resetConnections() {
        List<PeerConnectionThread> peers = TrackerApp.getConnections();
        for (PeerConnectionThread peer : peers) {
            try {
                peer.refreshStatus();
                peer.refreshFileList();
            } catch (Exception e) {
                TrackerApp.removePeerConnection(peer);
            }
        }
        return "";
    }

    private static String refreshFiles() {
        for (PeerConnectionThread peer : TrackerApp.getConnections()) {
            try {
                peer.refreshFileList();
            } catch (Exception e) {
                TrackerApp.removePeerConnection(peer);
            }
        }
        return "";
    }

    private static String endProgram() {
        TrackerApp.endAll();
        return "";
    }
}
