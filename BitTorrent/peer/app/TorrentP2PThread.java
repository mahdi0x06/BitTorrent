package peer.app;

import common.utils.MD5Hash;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class TorrentP2PThread extends Thread {
	private final Socket socket;
	private final File file;
	private final String receiver;
	private final BufferedOutputStream dataOutputStream;

	public TorrentP2PThread(Socket socket, File file, String receiver) throws IOException {
		this.socket = socket;
		this.file = file;
		this.receiver = receiver;
		this.dataOutputStream = new BufferedOutputStream(socket.getOutputStream());
		PeerApp.addTorrentP2PThread(this);
	}

    @Override
    public void run() {
        try {
            FileInputStream fileStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

            dataOutputStream.flush();
            fileStream.close();
            dataOutputStream.close();
            socket.close();

            String md5 = MD5Hash.HashFile(file.getAbsolutePath());
            String record = file.getName() + " " + md5;
            PeerApp.addSentFile(receiver, record);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (Exception ignore) {}
        }

        PeerApp.removeTorrentP2PThread(this);
    }

	public void end() {
		try {
			dataOutputStream.close();
			socket.close();
		} catch (Exception e) {}
	}
}
