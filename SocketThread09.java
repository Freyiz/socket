package socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SocketThread09 implements Runnable {
	public static final String OVERCODE = "---the-end-of-response-of-SocketService09---";
	public static final String CONTINUECODE = "---the-next-step-of-SocketService09---";
	public static final String CLIENTNAME = "客户端";
	private Socket socket;
	private int threadCount;

	public SocketThread09(Socket socket, int threadCount) {
		this.socket = socket;
		this.threadCount = threadCount;
	}

	public void run() {
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		DataInputStream dis = null;
		OutputStream os = null;
		PrintWriter pw = null;
		DataOutputStream dos = null;
		Scanner sc = new Scanner(System.in);
		try {
			// 发送线程排名信息
			os = socket.getOutputStream();
			pw = new PrintWriter(os);
			pw.println(threadCount);
			pw.flush();
			dos = new DataOutputStream(os);

			is = socket.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			dis = new DataInputStream(is);
			// 循环通信
			SocketThread09Loop(br, dis, pw, dos, sc, CLIENTNAME);

		} catch (IOException e) {
			// System.out.println("连接异常！");
		} finally {
			System.out.println("已断开连接，谢谢使用！");
			try {
				sc.close();
				if (br != null) {
					br.close();
				}
				if (dis != null) {
					dis.close();
				}
				if (pw != null) {
					pw.close();
				}
				if (dos != null) {
					dos.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				System.out.println("关闭异常！");
				e.printStackTrace();
			}
		}
	}

	public static void SocketThread09Loop(BufferedReader br, DataInputStream dis, PrintWriter pw, DataOutputStream dos,
			Scanner sc, String name) throws IOException {
		// 显示帮助信息
		System.out.println("请等待" + name + "的指令...");
		String inputMessage;

		while ((inputMessage = br.readLine()) != null) {
			String[] inputMessageArray = inputMessage.split(" ", 2);
			String command = inputMessageArray[0];

			if (inputMessageArray.length > 1) {
				String args = inputMessageArray[1];
				if (command.equals("-s") || command.equals("-send")) {
					// 响应客户端消息
					System.out.println(name + "消息：" + args);
					pw.println(sc.nextLine());
					pw.flush();
				} else if (command.equals("-f") || command.equals("-find")) {
					// 发送目录和文件信息至客户端
					System.out.println(name + "：" + inputMessage);
					SocketThread09.sendFilePath(args, pw, name);
				} else if (command.equals("-u") || command.equals("-upload")) {
					// 获取客户端文件
					ClientSocket09.downloadFiles(args, pw, dis, name);
				} else if (command.equals("-d") || command.equals("-download")) {
					// 发送文件至客户端
					System.out.println(name + "：" + inputMessage);
					SocketThread09.sendFiles(args, br, dos, name);
				}
			} else if (command.equals("-r") || command.equals("-reverse")) {
				// 切换输入输出方
				System.out.println("切换成功！");
				ClientSocket09.clientSocketLoop(br, dis, pw, dos, sc, name);
			} else if (command.equals("-q") || command.equals("-quit")) {
				System.out.println(name + "正在断开连接...");
				throw new IOException();
			} else if (command.equals("-h") || command.equals("-help")) {
				System.out.println(name + "消息：" + command);
			}
		}
	}

	public static void sendFilePath(String args, PrintWriter pw, String name) throws IOException {
		String[] argsArray = args.split(" ");
		int length = argsArray.length;
		// 获取文件路径
		String path = args;

		// 判断是否递归子目录
		boolean multiLevel = false;
		if (argsArray[length - 1].equals("-mul")) {
			// 重新获取文件路径
			path = args.substring(0, args.lastIndexOf("-mul") - 1);
			multiLevel = true;
		}

		File file = new File(path);
		if (file.isDirectory() || file.isFile()) {
			// 计数，计时
			int[] counts = { 1, 0 };
			long before, after;
			before = System.currentTimeMillis();

			if (file.isDirectory()) {
				counts = SocketThread09.sendListFiles(file, pw, multiLevel, counts);
			} else if (file != null) {
				counts[1]++;
				pw.println("文件：" + file);
			}
			after = System.currentTimeMillis();
			pw.println("查询结束：一共 " + counts[0] + " 个目录，" + counts[1] + " 个文件，用时 " + (after - before) + " ms。");
			pw.flush();
			System.out.println("查询结果已发送至" + name + "，用时 " + (after - before) + " ms。");
		} else {
			pw.println("未找到源目录或文件！");
		}
		// 发送结束暗号终止循环
		pw.println(SocketThread09.OVERCODE);
		pw.flush();
	}

	public static int[] sendListFiles(File file, PrintWriter pw, boolean multiLevel, int[] counts) throws IOException {
		File[] fileArray = file.listFiles();
		if (fileArray != null) {
			for (int i = 0; i < fileArray.length; i++) {
				File f = fileArray[i];
				String prefix = "文件：";
				if (f.isDirectory()) {
					counts[0]++;
					prefix = "目录：";
					// 递归子目录
					if (multiLevel) {
						SocketThread09.sendListFiles(f, pw, multiLevel, counts);
					}
				} else {
					counts[1]++;
				}
				pw.println(prefix + f);
				pw.flush();
			}
		}
		return counts;
	}

	public static void sendFiles(String args, BufferedReader br, DataOutputStream dos, String name) throws IOException {
		// 未通过客户端验证
		String inputMessage;
		if (!(inputMessage = br.readLine()).equals(SocketThread09.CONTINUECODE)) {
			if (name == ClientSocket09.SERVERNAME) {
				System.out.println(inputMessage);
			}
			return;
		}
		String[] argsArray = args.split(" ");
		// 获取文件路径
		String path = args.substring(0, args.lastIndexOf(argsArray[argsArray.length - 1]) - 1);
		File file = new File(path);

		// 发送路径名的起始截取位置索引
		dos.writeInt(Math.max(path.lastIndexOf(File.separator), path.lastIndexOf("/")));
		if (file.isDirectory() || file.isFile()) {
			if (name == SocketThread09.CLIENTNAME) {
				System.out.println("正在发送文件至" + name + "...");
			}
			long before, after;
			before = System.currentTimeMillis();

			if (file.isDirectory()) {
				// 发送目录信息
				SocketThread09.sendDirector(file, dos, name);
			} else if (file.isFile()) {
				// 发送文件内容
				SocketThread09.sendFile(file, dos, name);
			}
			// 发送结束暗号终止循环
			after = System.currentTimeMillis();
			dos.writeUTF(SocketThread09.OVERCODE);
			dos.flush();
			if (name == ClientSocket09.SERVERNAME) {
				inputMessage = br.readLine();
				System.out.println(inputMessage);
			} else {
				System.out.println(name + "下载结束，用时 " + (after - before) + " ms。");
			}
		} else {
			if (name == ClientSocket09.SERVERNAME) {
				System.out.println("未找到源目录或文件！");
			}
			return;
		}
	}

	public static void sendDirector(File director, DataOutputStream dos, String name) throws IOException {
		// 发送目录路径
		dos.writeUTF("正在下载目录 " + director + "...");
		dos.flush();
		if (name == ClientSocket09.SERVERNAME) {
			System.out.println("正在上传目录 " + director + "...");
		}

		File[] fileArray = director.listFiles();
		if (fileArray != null) {
			for (int i = 0; i < fileArray.length; i++) {
				File file = fileArray[i];
				if (file.isDirectory()) {
					// 递归
					SocketThread09.sendDirector(file, dos, name);
				} else {
					SocketThread09.sendFile(file, dos, name);
				}
			}
		}
		// 发送结束暗号终止单层目录循环
		dos.writeUTF(SocketThread09.OVERCODE);
		dos.flush();
	}

	public static void sendFile(File file, DataOutputStream dos, String name) throws IOException {
		// 发送文件路径
		dos.writeUTF("正在下载文件 " + file + "...");
		dos.flush();
		if (name == ClientSocket09.SERVERNAME) {
			System.out.println("正在上传文件 " + file + "...");
		}

		// 发送文件字节长度
		long lenght = file.length();
		dos.writeLong(file.length());

		int len = 1024;
		byte[] fileContent = new byte[len];
		InputStream is = new FileInputStream(file);

		while (lenght > 0) {
			// 需要严格控制数组的读写长度，否则可能导致数据丢失或误差
			if (lenght < len) {
				len = (int) lenght;
			}
			lenght -= is.read(fileContent, 0, len);
			dos.write(fileContent, 0, len);
			dos.flush();
		}
		is.close();
	}

	public static void printInstructions() {
		System.out.println("==================================================================");
		System.out.println("欢迎！常用指令如下：");
		System.out.println("-s/-send message              发送消息");
		System.out.println("-f/-find 目标目录/文件 [-mul]      查询目录/文件，参数 -mul 为可选，表示递归查看子目录");
		System.out.println("-u/-upload 本地目录/文件 目标目录             上传目录/文件");
		System.out.println("-d/-download 目标目录/文件 本地目录       下载目录/文件");
		System.out.println("-r/-reverse                   切换输入输出方");
		System.out.println("-q/-quit                      断开连接");
		System.out.println("-h/-help                      显示此帮助");
		System.out.println("==================================================================");
	}
}
