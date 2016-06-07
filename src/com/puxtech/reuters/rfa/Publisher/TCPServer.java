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
	
	// ���ؼ����˿�
	private int port = 9901;

	// TCP�¼�������
	private TCPIoProcessorImpl tcpProcessor = null;

	// �����׽���
	private ServerSocketChannel listenerChannel = null;

	// �¼�selectģ��
	private Selector selector = null;

	// Ĭ��0��ʾ����������select
	private long timeout = 0;

	// �߳����б�־
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
		// �򿪼����ŵ�
		listenerChannel = ServerSocketChannel.open();

		// ����Ϊ������ģʽ
		listenerChannel.configureBlocking(false);

		// ���ö˿�(��֤��һ�λ�û��ȫ�رյ�����±����ٴ򿪶˿ڲ���ʧ��)
		listenerChannel.socket().setReuseAddress(true);

		// �뱾�ض˿ڰ�
		listenerChannel.socket().bind(new InetSocketAddress(port));

		// ��ѡ�����󶨵������ŵ�,ֻ�з������ŵ��ſ���ע��ѡ����.����ע�������ָ�����ŵ����Խ���Accept����
		listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	private int init() {
		if (0 == port || null == tcpProcessor) { // û�����ö˿ں�TCP�¼�������
			return -1;
		}

		try {
			// ����ѡ����
			selector = Selector.open();

			// �����׽���
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
		// ����ѭ��,�ȴ�IO
		while (runFlag) {
			try {
				// �ȴ�ĳ�ŵ�����(��ʱ)
				if (0 == selector.select(timeout)) {
					continue;
				}

				// �̼߳���
			} catch (IOException e) {
				continue;
			} catch (Exception e) {
				continue;
			}

			// ȡ�õ�����.selectedKeys()�а�����ÿ��׼����ĳһI/O�������ŵ���SelectionKey
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				try {
					if (key.isAcceptable()) {
						// �пͻ�����������ʱ
						tcpProcessor.handleAccept(key);
					}

					if (key.isValid() && key.isWritable()) {
						// �ͻ��˿�дʱ
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

	public int start() { // ��ʹ���µ��߳�������ѭ��
		int ret = init();
		if (0 == ret) {
			select();
		}

		return ret;
	}

	public int startWithThread() { // ʹ���µ��߳�������ѭ��
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
			log.error("TCPServer��ʼ��ʧ�ܣ�");
		}

		return ret;
	}

	public void close() {
		try {
			runFlag = false;

			// �ر�selector
			if (null != selector) {
				selector.close();
			}

			// �رռ����׽���
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
