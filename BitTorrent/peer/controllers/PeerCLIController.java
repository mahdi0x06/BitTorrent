package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import common.utils.MD5Hash;
import peer.app.PeerApp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static peer.app.PeerApp.TIMEOUT_MILLIS;
import static peer.app.PeerApp.getSharedFolderPath;

public class PeerCLIController {

    public static String processCommand(String command) {
        for (PeerCommands cmd : PeerCommands.values()) {
            Matcher matcher = cmd.getMatcher(command);
            if (matcher.matches()) {
                switch (cmd) {
                    case EXIT:
                        return endProgram();
                    case LIST:
                        return handleListFiles();
                    case DOWNLOAD:
                        return handleDownload(matcher.group(1));
                }
            }
        }
        return "Invalid command.";
    }

    private static String handleListFiles() {
        try {
            Map<String, String> files = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
            return FileUtils.getSortedFileList(files);
        } catch (Exception e) {
            return "Repository is empty.";
        }
    }

    //    private static String handleDownload(String filename) {
//        try {
//            File targetFile = new File(PeerApp.getSharedFolderPath(), filename);
//            if (targetFile.exists()) {
//                return "You already have the file!";
//            }
//
//            HashMap<String, Object> body = new HashMap<>();
//            body.put("name", filename);
//            Message request = new Message(body, Message.Type.file_request);
//            Message response = PeerApp.getP2TConnection().sendAndWaitForResponse(request, TIMEOUT_MILLIS);
//
//            if (response == null) {
//                return "No peer has the file!";
//            }
//
//            String responseType = response.getFromBody("response");
//
//            if ("error".equals(responseType)) {
//                String error = response.getFromBody("error");
//                switch (error) {
//                    case "not_found":
//                        return "No peer has the file!";
//                    case "multiple_hash":
//                        return "Multiple hashes found!";
//                }
//            }
//
//            if ("peer_found".equals(responseType)) {
//                String peerIp = response.getFromBody("peer_have");
//                int peerPort = response.getIntFromBody("peer_port");
//                String md5 = response.getFromBody("md5");
//
//                boolean downloadSuccess = PeerApp.requestDownload(peerIp, peerPort, filename, md5);
//                if (!downloadSuccess) {
//                    return "File downloaded successfully: " + filename;
//                }
//
//                File downloadedFile = new File(PeerApp.getSharedFolderPath(), filename);
//
//                if (!downloadedFile.exists() || downloadedFile.length() == 0) {
//                    downloadedFile.delete();
//                    return "The file has been downloaded from peer but is corrupted!";
//                }
//
//                String actualMd5 = MD5Hash.HashFile(downloadedFile.getAbsolutePath());
//                if (!md5.equals(actualMd5)) {
//                    downloadedFile.delete();
//                    return "The file has been downloaded from peer but is corrupted!";
//                }
//
//                PeerApp.addReceivedFile(peerIp + ":" + peerPort, filename + " " + md5);
//                return "File downloaded successfully: " + filename;
//            }
//
//            return "Unknown response type from tracker.";
//        } catch (Exception e) {
//            return "File downloaded successfully: " + filename;
//        }
//    }
    private static String handleDownload(String filename) {
        try {
            File targetFile = new File(PeerApp.getSharedFolderPath(), filename);
            if (targetFile.exists()) {
                return "You already have the file!";
            }

            HashMap<String, Object> body = new HashMap<>();
            body.put("name", filename);
            Message request = new Message(body, Message.Type.file_request);
            Message response = PeerApp.getP2TConnection().sendAndWaitForResponse(request, TIMEOUT_MILLIS);

            if (response == null) {
                return "No peer has the file!";
            }

            String responseType = response.getFromBody("response");
            if ("error".equals(responseType)) {
                String error = response.getFromBody("error");
                return switch (error) {
                    case "not_found" -> "No peer has the file!";
                    case "multiple_hash" -> "Multiple hashes found!";
                    default -> "Unknown error.";
                };
            }

            if ("peer_found".equals(responseType)) {
                String peerIp = response.getFromBody("peer_have");
                int peerPort = response.getIntFromBody("peer_port");
                String md5 = response.getFromBody("md5");

                boolean downloadSuccess = PeerApp.requestDownload(peerIp, peerPort, filename, md5);
                File downloadedFile = new File(PeerApp.getSharedFolderPath(), filename);

                if (!downloadSuccess || !downloadedFile.exists() || downloadedFile.length() == 0) {
                    System.out.println("downloadSuccess " + downloadSuccess);
                    System.out.println("downloadedFile exist " + downloadedFile.exists());
                    System.out.println("downloadedFile length " + downloadedFile.length());
                    downloadedFile.delete();
                    return "The file has been downloaded from peer but is corrupted!";
                }

                String actualMd5 = MD5Hash.HashFile(downloadedFile.getAbsolutePath());
                if (!md5.equals(actualMd5)) {
                    System.out.println("md5 " + md5);
                    System.out.println("actualMd5 " + actualMd5);
                    downloadedFile.delete();
                    return "The file has been downloaded from peer but is corrupted!";
                }

                PeerApp.addReceivedFile(peerIp + ":" + peerPort, filename + " " + md5);
                return "File downloaded successfully: " + filename;
            }

            return "Unknown response type from tracker.";
        } catch (Exception e) {
            return "Download failed due to unexpected error.";
        }
    }

    public static String endProgram() {
        PeerApp.endAll();
        return "";
    }
}
