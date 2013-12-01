package cy.nicosia.zenont.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import cy.nicosia.zenont.base.ConfigManager;
import cy.nicosia.zenont.base.Logger;
import cy.nicosia.zenont.base.ConfigManager.Config;
import cy.nicosia.zenont.net.protocol.IProtocol;

/**
 * <b>TcpSocketD</b> extends the <b>SocketD</b> class.
 * <p/>
 * Implementation which starts a multithreaded TCP listener and is assigned
 * an <b>IProtocol</b> implementation for communication over any
 * established connections. 
 */
public class TcpSocketD implements ISocketD {

	private static final String TAG = "TcpSocketD";

	private String _host;
	private int _port;
	private ServerSocket _serverSocket;
	private AsyncAcceptThread _asyncAcceptThread;
	private TcpSocketDConfig _cfg;
	
	protected ConfigManager _cfgMgr;
	
	public TcpSocketD(ConfigManager configManager) {
		this(configManager, 0);
	}
	
	public TcpSocketD(ConfigManager configManager, int port) {
		this(configManager, null, port);
	}
	
	public TcpSocketD(ConfigManager configManager, String host, int port) {
		_cfgMgr = configManager;
		_cfg = (TcpSocketDConfig) _cfgMgr.getConfig(TcpSocketDConfig.class);
		setHost(host);
		setPort(port);
		
	}
	
	@Override
	public boolean start() {
		try {
			_serverSocket = new ServerSocket();
			InetSocketAddress iSockAddr =
					(getHost() == null ? new InetSocketAddress(getPort()) : new InetSocketAddress(getHost(), getPort()));		
			_serverSocket.bind(iSockAddr);
			//In case of port 0 passed as argument assign real port number since it is assigned by system
			setPort(_serverSocket.getLocalPort());
			Logger.debug(TAG, "Server started on port: " + getPort());
			_asyncAcceptThread = new AsyncAcceptThread();
			_asyncAcceptThread.setDaemon(true);
			_asyncAcceptThread.setName("Accept_Worker_Thread_" + getPort());
			_asyncAcceptThread.start();
		} catch (IOException e) {
			Logger.error(TAG, e);
			return false;
		}
		return true;
	}
	
	@Override
	public void stop() {
		_asyncAcceptThread.cancel();
		try {
			_asyncAcceptThread.join(_cfg.getConfigWaitForSocketClose());
		} catch (InterruptedException e) {
			Logger.error(TAG, e);
		}
	}	
	/**
	 * @return Host that socket is currently binded to.
	 */
	public String getHost() {
		return _host;
	}
	/**
	 * @param host Set host IP address that socket should bind to.
	 */
	public void setHost(String host) {
		this._host = host;
	}
	/**
	 * @return Port that socket is currently binded to.
	 */
	public int getPort() {
		return _port;
	}
	/**
	 * @param host Set port that socket should bind to.
	 */
	public void setPort(int port) {
		this._port = port;
	}
	/**
	 * @return The number of total connections accepted by listener.
	 */
	public long getTotalConnections() {
		return _asyncAcceptThread.getTotalConnects();
	}
	/**
	 * Thread which binds socket port and accepts connections.
	 * Starts Runnable to handle each connection.
	 */
	class AsyncAcceptThread extends Thread {

		private static final String TAG = "AsyncAcceptThread";

		private long _totalConnects;

		@Override
		public void run() {
			Logger.debug(TAG, "Async Accept Thread started");
			_totalConnects = 0;

			while (!Thread.currentThread().isInterrupted()) {
				Socket client;
				try {
					client = _serverSocket.accept();
					_totalConnects++;
					Logger.debug(TAG, "Connection accepted from: " + client.getInetAddress().getHostAddress());
					Thread connection = new Thread(new ConnectionWorker(client));
					connection.setName("Connection-" + _totalConnects);
					connection.setDaemon(true);
					connection.start();
				} catch (IOException e) {
					//If we close the socket to stop the thread do not log an error 
					if (!Thread.currentThread().isInterrupted())
						Logger.error(TAG, e);
				} 
			}
			Logger.debug(TAG, "Async Accept Thread stopping");
		}

		void cancel() {
			//Set interrupt flag
			_asyncAcceptThread.interrupt();
			try {
				//Cause an IOException to stop blocking
				if (!_serverSocket.isClosed())
					_serverSocket.close();
			} catch (IOException e) {
				assert(false); 
				Logger.error(TAG, e);
			}
		}

		synchronized long getTotalConnects() {
			return _totalConnects;
		}
	}
	/**
	 * Runnable that hands each connection to protocol handler.
	 */
	class ConnectionWorker implements Runnable {

		private static final String TAG = "ConnectionWorker";

		private Socket _clientSocket;

		ConnectionWorker(Socket client) {
			_clientSocket = client;
		}

		@Override
		public void run() {
			//An instance of our protocol handler
			IProtocol protocolHandlerInstance = null;

			try {
				Logger.debug(TAG, "ConnectionWorker started for client: " + _clientSocket.getInetAddress().getHostAddress());

				protocolHandlerInstance = _cfg.getProtocolHandler().newInstance();

				Logger.debug(TAG, "Starting protocol handler instance: " + protocolHandlerInstance.toString());
				protocolHandlerInstance.exec(_cfgMgr, _clientSocket);	
			} catch (Exception e) {
				Logger.error(TAG, e);
			} finally {
				protocolHandlerInstance.dispose();
				_clientSocket = null;
			}
		}
	}
	
	public static class TcpSocketDConfig extends Config {
		
		@SuppressWarnings("unused")
		private static final String TAG = "TcpSocketDConfig";
		
		//Configuration settings
		private volatile Class<? extends IProtocol> _protocolHandler;
		private volatile int _configWaitForSocketClose;
		
		//defaults
		{
			setConfigWaitForSocketClose(5);
		}
		
		public Class<? extends IProtocol> getProtocolHandler() {
			return _protocolHandler;
		}
		
		public void setProtocolHandler(
				Class<? extends IProtocol> protocol) {
			this._protocolHandler = protocol;
		}
		
		public int getConfigWaitForSocketClose() {
			return _configWaitForSocketClose;
		}
		public void setConfigWaitForSocketClose(int configWaitForSocketClose) {
			this._configWaitForSocketClose = configWaitForSocketClose * 1000;
		}
		
	}
}
