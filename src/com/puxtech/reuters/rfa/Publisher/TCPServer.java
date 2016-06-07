package com.puxtech.reuters.rfa.Publisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TCPServer {
	private static final Log log = LogFactory.getLog(TCPServer.class);
	
	// 本地监听端口
	private int port = 9901;

	// TCP事件处理器
	private TCPIoProcessorImpl tcpProcessor = null;

	// 监听套接字
	private ServerSocketChannel listenerChannel = null;

	// 事件select模型
	private Selector selector = null;

	// 默认0表示无限期阻塞select
	private long timeout = 0;

	// 线程运行标志
	private volatile boolean runFlag = true;

	public TCPServer() {
		this(0, null);
	}

	public TCPServer(int port, TCPIoProcessorImpl tcpProtocol) {
		setPort(port);
		setTCPProcessor(tcpProtocol);
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setTCPProcessor(TCPIoProcessorImpl tcpProcessor) {
		this.tcpProcessor = tcpProcessor;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void createSocket() throws IOException {
		// 打开监听信道
		listenerChannel = ServerSocketChannel.open();

		// 设置为非阻塞模式
		listenerChannel.configureBlocking(false);

		// 重用端口(保证上一次还没完全关闭的情况下本次再打开端口不会失败)
		listenerChannel.socket().setReuseAddress(true);

		// 与本地端口绑定
		listenerChannel.socket().bind(new InetSocketAddress(port));

		// 将选择器绑定到监听信道,只有非阻塞信道才可以注册选择器.并在注册过程中指出该信道可以进行Accept操作
		listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	private int init() {
		if (0 == port || null == tcpProcessor) { // 没有设置端口和TCP事件处理器
			return -1;
		}

		try {
			// 创建选择器
			selector = Selector.open();

			// 创建套接字
			createSocket();
		} catch (IOException e) {
			log.error("", e);
			return -1;
		} catch (Exception e) {
			log.error("", e);
			return -1;
		}

		return 0;
	}

	private void select() {
		// 反复循环,等待IO
		while (runFlag) {
			try {
				// 等待某信道就绪(或超时)
				if (0 == selector.select(timeout)) {
					continue;
				}

				// 线程计数
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}

			// 取得迭代器.selectedKeys()中包含了每个准备好某一I/O操作的信道的SelectionKey
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				try {
					if (key.isAcceptable()) {
						// 有客户端连接请求时
						tcpProcessor.handleAccept(key);
					}

					if (key.isValid() && key.isWritable()) {
						// 客户端可写时
						tcpProcessor.handleWrite(key);
					}
				}
				catch (Exception e) {
					try {
						SocketChannel sockchannel = (SocketChannel)key.channel();
						sockchannel.socket().shutdownInput();
						sockchannel.socket().shutdownOutput();
						sockchannel.close();
					} catch (Exception e1) {
						log.error("close error channel", e1);
					}
					key.cancel();
					if(!(e instanceof ClosedChannelException) )
						log.error("select",e);
				} finally {
					keyIter.remove();
				}
			}
	    }
	}

	public int start() { // 不使用新的线程来启动循环
		int ret = init();
		if (0 == ret) {
			select();
		}

		return ret;
	}

	public int startWithThread() { // 使用新的线程来启动循环
		int ret = init();
		if (0 == ret) {
			new Thread(new Runnable() {
				public void run() {
					try {
						select();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}else{
			log.error("TCPServer初始化失败！");
		}

		return ret;
	}

	public void close() {
		try {
			runFlag = false;

			// 关闭selector
			if (null != selector) {
				selector.close();
			}

			// 关闭监听套接字
			if (null != listenerChannel) {
				listenerChannel.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void publish(Object obj) {
		//tcpProcessor.publish(selector.selectedKeys(),obj);
	}
}
