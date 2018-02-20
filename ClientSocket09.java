package socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientSocket09 {
	public static final String SERVERNAME = "服务端";
	private String address = "127.0.0.1";
	private int port = 9836;

	public static void main(String[] args) {
		new ClientSocket09().run();
	}

	public void run() {
		Socket socket = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		DataInputStream dis = null;
		OutputStream os = null;
		PrintWriter pw = null;
		DataOutputStream dos = null;
		Scanner sc = new Scanner(System.in);
		try {
			socket = new Socket(address, port);
			System.out.println("已与" + SERVERNAME + "建立连接！");
			InetAddress addr = InetAddress.getByName(address);
			System.out.println(SERVERNAME + "名称：" + addr.getHostName() + " 地址：" + addr.getHostAddress());
			// 获取线程排名信息
			is = socket.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			// 使用 DataInputStream 获取文件数据
			dis = new DataInputStream(is);
			String threadCount = br.readLine();
			System.out.println("这是当前第 " + threadCount + " 个线程。");

			os = socket.getOutputStream();
			pw = new PrintWriter(os);
			dos = new DataOutputStream(os);

			// 循环通信
			clientSocketLoop(br, dis, pw, dos, sc, SERVERNAME);

		} catch (UnknownHostException e) {
			System.out.println("未知主机地址！");
			// e.printStackTrace();
		} catch (IOException e) {
			// System.out.println("出错了，已断开连接！");
		} finally {
			System.out.println("已断开连接，谢谢使用！");
			sc.close();
		}
	}

	public static void clientSocketLoop(BufferedReader br, DataInputStream dis, PrintWriter pw, DataOutputStream dos,
			Scanner sc, String name) throws IOException {
		String inputMessage, outputMessage;
		// 显示帮助信息
		SocketThread09.printInstructions();
		System.out.println("请输入指令：");

		while (true) {
			outputMessage = sc.nextLine();
			pw.println(outputMessage);
			pw.flush();
			String[] outputMessageArray = outputMessage.split(" ", 2);
			String command = outputMessageArray[0];

			if (outputMessageArray.length > 1) {
				String args = outputMessageArray[1];
				if (command.equals("-s") || command.equals("-send")) {
					System.out.println("已发送，请等待" + name + "响应...");
					// 此处会读取到服务端之前的无关输入，待解决...
					inputMessage = br.readLine();
					System.out.println(name + "消息：" + inputMessage);
				} else if (command.equals("-f") || command.equals("-find")) {
					// 循环输出来自服务端的响应至控制台
					while (!(inputMessage = br.readLine()).equals(SocketThread09.OVERCODE)) {
						System.out.println(inputMessage);
					}
				} else if (command.equals("-u") || command.equals("-upload")) {
					// 上传文件至服务端
					SocketThread09.sendFiles(args, br, dos, name);
				} else if (command.equals("-d") || command.equals("-download")) {
					// 下载服务端文件到本地
					ClientSocket09.downloadFiles(args, pw, dis, name);
				}
				// 切换输入输出方
			} else if (command.equals("-r") || command.equals("-reverse")) {
				System.out.println("切换成功！");
				SocketThread09.SocketThread09Loop(br, dis, pw, dos, sc, name);
			} else if (command.equals("-q") || command.equals("-quit")) {
				System.out.println("正在与" + name + "断开连接...");
				throw new IOException();
			} else if (command.equals("-h") || command.equals("-help")) {
				SocketThread09.printInstructions();
			}
		}
	}

	public static void downloadFiles(String args, PrintWriter pw, DataInputStream dis, String name) throws IOException {
		String[] argsArray = args.split(" ");
		// 获取本地文件路径
		String path = argsArray[argsArray.length - 1];
		File file = new File(path);

		if (file.isDirectory()) {
			pw.println(SocketThread09.CONTINUECODE);
			pw.flush();
		} else {
			pw.println("请指定一个有效目录！");
			pw.flush();
			if (name == ClientSocket09.SERVERNAME) {
				System.out.println("请指定一个有效目录！");
			}
			return;
		}

		// 获取顶级路径
		String fatherPath = path;
		// 获取路径名的起始截取位置索引
		int childPathIndex = dis.readInt();
		if ((childPathIndex) == -1) {
			if (name == ClientSocket09.SERVERNAME) {
				System.out.println("未找到源目录或文件！");
			}
			return;
		}

		if (name == SocketThread09.CLIENTNAME) {
			System.out.println("正在获取" + name + "文件...");
		}
		// 计数，计时
		int[] counts = { 0, 0 };
		long before, after;
		before = System.currentTimeMillis();
		counts = ClientSocket09.downloadDirector(dis, fatherPath, childPathIndex, counts, name);
		after = System.currentTimeMillis();
		if (name == ClientSocket09.SERVERNAME) {
			System.out.println("下载结束：一共 " + counts[0] + " 个目录，" + counts[1] + " 个文件，用时 " + (after - before) + " ms。");
		} else {
			pw.println("上传结束：一共 " + counts[0] + " 个目录，" + counts[1] + " 个文件，用时 " + (after - before) + " ms。");
			pw.flush();
			System.out.println(name + "上传结束，用时 " + (after - before) + " ms。");
		}
	}

	public static int[] downloadDirector(DataInputStream dis, String fatherPath, int childPathIndex, int[] counts,
			String name) throws IOException {
		String UTFoutputMessage;
		// 循环获取数据直到碰到终止暗号
		while (!(UTFoutputMessage = dis.readUTF()).equals(SocketThread09.OVERCODE)) {
			if (name == ClientSocket09.SERVERNAME) {
				// 输出内容至控制台
				System.out.println(UTFoutputMessage);
			}
			// 获取目录路径
			String path = fatherPath
					+ UTFoutputMessage.substring(childPathIndex + 7, UTFoutputMessage.lastIndexOf("..."));
			if (UTFoutputMessage.startsWith("正在下载目录")) {
				counts[0]++;
				File director = new File(path);
				director.mkdir();
				// 递归
				ClientSocket09.downloadDirector(dis, fatherPath, childPathIndex, counts, name);
			} else {
				counts[1]++;
				// 保存文件到本地
				ClientSocket09.downloadFile(path, dis);
			}
		}
		return counts;
	}

	public static void downloadFile(String filePath, DataInputStream dis) throws IOException {
		// 获取文件字节长度
		long lenght = dis.readLong();

		int len = 1024;
		byte[] fileContent = new byte[len];
		OutputStream os = new FileOutputStream(filePath);

		while (lenght > 0) {
			// 需要严格控制数组的读写长度，否则可能导致数据丢失或误差
			if (lenght < len) {
				len = (int) lenght;
			}
			lenght -= dis.read(fileContent, 0, len);
			os.write(fileContent, 0, len);
			os.flush();
		}
		os.close();
	}
}
