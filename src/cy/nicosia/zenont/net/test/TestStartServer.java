package cy.nicosia.zenont.net.test;

import java.io.IOException;

import cy.nicosia.zenont.base.ConfigManager;
import cy.nicosia.zenont.base.Logger;
import cy.nicosia.zenont.base.ConfigManager.Config;
import cy.nicosia.zenont.net.TcpSocketD;
import cy.nicosia.zenont.net.TcpSocketD.TcpSocketDConfig;
import cy.nicosia.zenont.net.protocol.HttpProtocol.HttpProtocolConfig;
import cy.nicosia.zenont.net.protocol.http.HttpSessionManager.HttpSessionManagerConfig;

public class TestStartServer {

	private static final String TAG = "TestStartServer";
	
	public static void main(String[] args) {
		new TestStartServer().exec();
	}
	
	public void exec() {
		ConfigManager cfgMgr = ConfigManager.getNewInstance();
		setConfigManagerSettings(cfgMgr);
		TcpSocketD httpTcpServer = new TcpSocketD(cfgMgr, 8080);
		
		if (!httpTcpServer.start()) {
			Logger.error(TAG, "Could not start server");
			return;
		}
		
		/*for (int i = 10; i < 10; i++) {
			String host = httpTcpServer.getHost();
			int port = httpTcpServer.getPort();
	
			try {
				Socket s = new Socket(host, port);
				s.close();
			} catch (UnknownHostException e) {
				Logger.error(TAG, e);
			} catch (IOException e) {
				Logger.error(TAG, e);
			}
		}*/
		
		try {
			System.in.read();
			System.out.println("Total Connections: " + httpTcpServer.getTotalConnections());
		} catch (IOException e) {
			Logger.error(TAG, e);
		}
		
		httpTcpServer.stop();
	}
	
	public void setConfigManagerSettings(ConfigManager cfgMgr) {
		Config cfg = null;
		
		cfg = cfgMgr.getConfig(TcpSocketDConfig.class);
		((TcpSocketDConfig)cfg).setProtocolHandler(TestHttpProtocolHandler.class);
		((TcpSocketDConfig)cfg).setConfigWaitForSocketClose(5);
		
		cfg = cfgMgr.getConfig(HttpProtocolConfig.class);
		((HttpProtocolConfig)cfg).setConfigBufferLength(4 * 1024);
		((HttpProtocolConfig)cfg).setConfigDefaultConnectionTimeoutSeconds(20);
		((HttpProtocolConfig)cfg).setConfigSessionEnabled(true);
		((HttpProtocolConfig)cfg).setConfigSessionCookieIdentifier("SocketDTest");
		
		cfg = cfgMgr.getConfig(HttpSessionManagerConfig.class);
		((HttpSessionManagerConfig)cfg).setConfigSessionTimeoutMinutes(1);
		((HttpSessionManagerConfig)cfg).setConfigSessionTimerMaintenance(2);
	}
}
