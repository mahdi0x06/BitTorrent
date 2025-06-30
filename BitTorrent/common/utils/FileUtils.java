package common.utils;

import java.io.File;
import java.util.*;

public class FileUtils {

    public static Map<String, String> listFilesInFolder(String folderPath) {
        Map<String, String> fileMap = new HashMap<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return fileMap;
        }

        File[] files = folder.listFiles();
        if (files == null) return fileMap;

        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName();
                String md5 = MD5Hash.HashFile(file.getAbsolutePath());
                fileMap.put(filename, md5);
            }
        }

        return fileMap;
    }

    public static String getSortedFileList(Map<String, String> files) {
        if (files.isEmpty()) {
            return "Repository is empty.";
        }

        List<String> fileNames = new ArrayList<>(files.keySet());
        Collections.sort(fileNames);

        StringBuilder builder = new StringBuilder();
        for (String name : fileNames) {
            builder.append(name).append(" ").append(files.get(name)).append("\n");
        }

        return builder.toString().trim();
    }
}
