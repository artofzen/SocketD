package cy.nicosia.zenont.base;

import java.util.HashMap;
import java.util.Map;


public class ConfigManager {

	private static final String TAG = "Configuration Manager";

	private final Map<Class<? extends Config>, Config> _configSettings;
	
	private ConfigManager() {
		_configSettings = new HashMap<Class<? extends Config>, Config>();
	}

	public static ConfigManager getNewInstance() {
		return new ConfigManager();
	}

	public synchronized Config getConfig(Class<? extends Config> key) {
		if (_configSettings.containsKey(key)) {
			return _configSettings.get(key);
		} else {
			try {
				_configSettings.put(key, key.newInstance());
			}  catch (Exception e) {
				Logger.error(TAG, e);
			}
			return _configSettings.get(key);
		}
	}

	public abstract static class Config {
		@SuppressWarnings("unused")
		private static final String TAG = "Config";
	}
}
