package cy.nicosia.zenont.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MultiValueMap<T1, T2> {
	
	public static final String TAG = "MultiValueMap";
	
	protected Map<T1, ArrayList<T2>> _collection;
	
	public MultiValueMap() {
		this._collection = new HashMap<T1, ArrayList<T2>>();
	}
	/**
	 * Use this method when you want to append to the previous value.
	 * @param key
	 * @param value
	 */
	public void appendValueToKey(T1 key, T2 value) {
		if (!_collection.containsKey(key)) 
			_collection.put(key, new ArrayList<T2>());

		_collection.get(key).add(value);
	}
	/**
	 * Use this method when you want to overwrite any previous value.
	 * @param key
	 * @param value
	 */
	public void overwriteKeyValue(T1 key, T2 value) {
		if (!_collection.containsKey(key)) 
			_collection.put(key, new ArrayList<T2>());

		_collection.get(key).clear();
		_collection.get(key).add(value);
	}
	/**
	 * @return a Map object containing all headers.
	 */
	protected Map<T1, ArrayList<T2>> getCollection() {
		return _collection;
	}

	public boolean hasKey(T1 s) {
		for(T1 k:_collection.keySet()) {
			if (s == k)
				return true; 
			}
		return false;
	}

	public boolean hasKeyValue(T1 s, T2 t) {
		for(T1 k:_collection.keySet())
			if(s == k) {
				ArrayList<T2> temp = _collection.get(k);
				for(T2 v:temp)
					if (t == v)
						return true;
			}

		return false;
	}

	public ArrayList<T2> getKeyValues(T1 s) {
		for (T1 k:_collection.keySet())
			if (s == k)
				return _collection.get(k);

		return null;
	}
}
