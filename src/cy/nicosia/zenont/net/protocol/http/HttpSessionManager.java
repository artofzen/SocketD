package cy.nicosia.zenont.net.protocol.http;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import cy.nicosia.zenont.base.ConfigManager;
import cy.nicosia.zenont.base.Logger;
import cy.nicosia.zenont.base.Pile;
import cy.nicosia.zenont.base.ConfigManager.Config;

public class HttpSessionManager implements Runnable {

	private static final String TAG = "Session Manager";
	
	private static final Map<String, HttpSession> _sessionValues;
	private static HttpSessionManager _httpSessionManager;
	
	private HttpSessionManagerConfig _cfg;

	static {	
		_sessionValues = new HashMap<String, HttpSession>();
	}
	
	private HttpSessionManager(Config cfg) {
		_cfg = (HttpSessionManagerConfig) cfg;
	}

	public synchronized static HttpSessionManager getInstance(ConfigManager conMgr) {
		if (_httpSessionManager == null){
			_httpSessionManager = new HttpSessionManager(conMgr.getConfig(HttpSessionManagerConfig.class));
			Thread maintenance = new Thread(_httpSessionManager);
			maintenance.setName(TAG);
			maintenance.start();
		}
			
		return _httpSessionManager;
	}

	public synchronized HttpSession getSession(String key) {
		try {
			HttpSession tempSession = null;
			if (key != null && _sessionValues.containsKey(key)) {	
					tempSession = _sessionValues.get(key);
					if (!tempSession.isExpired())
						tempSession.updateLastAccessTime();
					else
						tempSession = getSession(null);
			} else {
				String newKey = null;
				do {
					newKey = generateKey();
				} while(_sessionValues.containsKey(key));
				tempSession = new HttpSession(newKey, _cfg.getConfigSessionTimeoutMinutes());
				_sessionValues.put(newKey, tempSession);
			}

			return tempSession;
		} catch (Exception ex) {
			return null;
		}
	}

	private synchronized void maintainSessions() {
		Logger.debug(TAG, "Running _session maintenance");
		Iterator<Entry<String, HttpSession>> it = _sessionValues.entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<String, HttpSession> pair = it.next();
			String key = pair.getKey();
			Logger.debug(TAG, "Checking _session: " + key);
			if (pair.getValue().isExpired()) {
				Logger.debug(TAG, "Removing _session: " + key);
				it.remove();
			}
		}
	}

	@Override
	public void run() {
		Logger.debug(TAG, "Started");

		try {
			while(true) {
				Thread.sleep(_cfg.getConfigSessionTimerMaintenance());
				maintainSessions();
			}

		} catch (InterruptedException e) {
			Logger.error(TAG, e);
		}	
	}

	private String generateKey() {
		String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
		int len = 30;
		Random rnd = new Random(Calendar.getInstance().getTimeInMillis());

		StringBuilder sb = new StringBuilder(len);
		for(int i = 0; i < len; i++) 
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public static class HttpSession extends Pile {

		@SuppressWarnings("unused")
		private static final String TAG = "Http Session";

		private final String _sessionKey;
		private long _lastAccess;
		private long _sessionTimeout;

		public HttpSession(String key, int sessionTimeout) {
			_sessionKey = key;
			_sessionTimeout = (long) sessionTimeout;
			_lastAccess = Calendar.getInstance().getTimeInMillis();
		}

		private void updateLastAccessTime() {
			_lastAccess = Calendar.getInstance().getTimeInMillis();
		}

		private boolean isExpired() {
			if (Calendar.getInstance().getTimeInMillis() - _lastAccess > _sessionTimeout)
				return true;
			else
				return false;
		}

		public String getSessionKey() {
			return _sessionKey;
		}
	}
	
	public static class HttpSessionManagerConfig extends Config {

		@SuppressWarnings("unused")
		private static final String TAG = "Http Session Manager Config";

		//Configuration settings
		private volatile int _configSessionTimeoutMinutes;
		private volatile long _configSessionTimerMaintenance;

		//defaults
		{
			setConfigSessionTimeoutMinutes(10);
			setConfigSessionTimerMaintenance(1);
		}

		public int getConfigSessionTimeoutMinutes() {
			return _configSessionTimeoutMinutes;
		}

		public void setConfigSessionTimeoutMinutes(int configSessionTimeoutMinutes) {
			this._configSessionTimeoutMinutes = configSessionTimeoutMinutes * 60 * 1000;
		}

		public long getConfigSessionTimerMaintenance() {
			return _configSessionTimerMaintenance;
		}

		public void setConfigSessionTimerMaintenance(
				long configSessionTimerMaintenance) {
			this._configSessionTimerMaintenance = configSessionTimerMaintenance * 60 * 1000;
		}
	}
}
