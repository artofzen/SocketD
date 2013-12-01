package cy.nicosia.zenont.base;

import java.util.HashMap;
import java.util.Map;

public class Pile {

	public static final String TAG = "Pile";
	
	private Map<String, String> _stringValues;
	private Map<String, Integer> _intValues;
	private Map<String, Long> _longValues;
	private Map<String, Double> _doubleValues;
	private Map<String, Float> _floatValues;
	private Map<String, Object> _objectValues;
	private Map<String, Boolean> _booleanValues;
	
	{
		_stringValues = null;
		_intValues = null;
		_longValues = null;
		_doubleValues = null;
		_floatValues = null;
		_objectValues = null;
		_booleanValues = null;
	}
	
	public synchronized void addString(String key, String value) {
		if (_stringValues == null)
			_stringValues = new HashMap<String, String>();
		_stringValues.put(key, value);
	}

	public synchronized String getString(String key) {
		return _stringValues.get(key);
	}

	public synchronized void addInteger(String key, Integer value) {
		if (_intValues == null)
			_intValues = new HashMap<String, Integer>();
		_intValues.put(key, value);
	}

	public synchronized Integer getInteger(String key) {
		return _intValues.get(key);
	}

	public synchronized void addLong(String key, Long value) {
		if (_longValues == null)
			_longValues = new HashMap<String, Long>();
		_longValues.put(key, value);
	}

	public synchronized Long getLong(String key) {
		return _longValues.get(key);
	}

	public synchronized void addDouble(String key, Double value) {
		if (_doubleValues == null)
			_doubleValues = new HashMap<String, Double>();
		_doubleValues.put(key, value);
	}

	public synchronized Double getDouble(String key) {
		return _doubleValues.get(key);
	}

	public synchronized void addFloat(String key, Float value) {
		if (_floatValues == null)
			_floatValues = new HashMap<String, Float>();
		_floatValues.put(key, value);
	}

	public synchronized Float getFloat(String key) {
		return _floatValues.get(key);
	}

	public synchronized void addObject(String key, Object value) {
		if (_objectValues == null)
			_objectValues = new HashMap<String, Object>();
		_objectValues.put(key, value);
	}

	public synchronized Object getObject(String key) {
		return _objectValues.get(key);
	}
	
	public synchronized void addBool(String key, boolean value) {
		if (_booleanValues == null)
			_booleanValues = new HashMap<String, Boolean>();
		_booleanValues.put(key, value);
	}

	public synchronized Object getBool(String key) {
		return _booleanValues.get(key);
	}
	
}
