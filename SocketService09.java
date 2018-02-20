package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketService09 {
	private int port = 9836;

	public static void main(String[] args) {
		new SocketService09().run();
	}

	public void run() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		int count = 1;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("服务器已启动...");
			while (true) {
				socket = serverSocket.accept();
				System.out.println("第" + count + "个线程已启动...");
				System.out.println("线程地址：" + socket.getInetAddress().getHostAddress() + " 端口：" + socket.getPort());
				new Thread(new SocketThread09(socket, count++)).start();
			}
		} catch (IOException e) {
			System.out.println("服务器异常！");
		}
	}
}
