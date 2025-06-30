package common.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class MD5Hash {
    public static String HashFile(String filePath) {
        try {
            // Step 1: Create MD5 digest instance
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Step 2: Open file stream
            InputStream is = new FileInputStream(filePath);
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Step 3: Read file in chunks and update digest
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            is.close();

            // Step 4: Convert byte[] digest to hex string
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // you might also throw the exception or return empty string
        }
    }
}
