package cy.nicosia.zenont.net.protocol.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cy.nicosia.zenont.base.Logger;
import cy.nicosia.zenont.base.MultiValueMap;

public abstract class HttpMessage {

	public static abstract class MimeTypes {

		public static final String JSON = "application/json";
		public static final String JAVASCRIPT = "application/javascript";
		public static final String BINARY = "application/octet-stream";
		public static final String PDF = "application/pdf";
		public static final String XML = "application/xml";
		public static final String ZIP = "application/zip";
		public static final String GZIP = "application/gzip";
		public static final String TEXT = "text/plain";
		public static final String HTML = "text/html";
		public static final String URLENCODED = "application/x-www-form-urlencoded";
		public static final String MULTIPART_FORM = "multipart/form-data";
		public static final String MULTIPART_MIXED = "multipart/mixed";

	}
	/**
	 * Basic implementation for pairs used in HTTP requests & responses.
	 * HttpMultiValueCollections are defined as a list of <code>Map&lt;String, ArrayList&lt;String&gt;&gt;</code>.<p/>
	 *
	 * This class provides formatting for header and other pair keys which might have multiple values.<br/>
	 * To add to a key which might have multiple values use the <code>appendValueToKey</code> otherwise
	 * use <code>overwriteKeyValue method</code>.
	 */
	public static class HttpMultiValueCollection extends MultiValueMap<String, String> {

		@SuppressWarnings("unused")
		private static final String TAG = "HttpMultiValueCollection";

		@Override
		public boolean hasKey(String s) {
			for(String k:getCollection().keySet())
				if (s.equalsIgnoreCase(k))
					return true;
			return false;
		}
		
		@Override
		public boolean hasKeyValue(String s, String t) {
			for(String k:getCollection().keySet())
				if(s.equalsIgnoreCase(k)) {
					ArrayList<String> temp = getCollection().get(k);
					for(String v:temp)
						if (t.equalsIgnoreCase(v))
							return true;
				}

			return false;
		}
		
		@Override
		public ArrayList<String> getKeyValues(String s) {
			for (String k:getCollection().keySet())
				if (s.equalsIgnoreCase(k))
					return getCollection().get(k);

			return null;
		}
		/**
		 * @return a string object formatted for header output in a HTTP request or response.
		 */
		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();
			for (String key:getCollection().keySet()) {
				sb.append(key); sb.append(':');
				for (String value:getCollection().get(key)) {
					sb.append(value); sb.append(',');
				}
				//Delete last comma
				sb.deleteCharAt(sb.length() - 1);
				sb.append("\r\n");
			}

			return sb.toString();
		}
		
		public boolean hasKeyValueStartsWith(String s, String t) {
			for(String k:getCollection().keySet())
				if(s.equalsIgnoreCase(k)) {
					ArrayList<String> temp = getCollection().get(k);
					for(String v:temp)
						if (v.toLowerCase().startsWith(t.toLowerCase()))
							return true;
				}

			return false;
		}
		
		public String getFirstKeyValueStartsWith(String s, String t) {
			try {
				return getKeyValueStartsWith(s, t).get(0);
			} catch (NullPointerException ex) {
				return null;
			}
		}

		public ArrayList<String> getKeyValueStartsWith(String s, String t) {
			ArrayList<String> tempList = new ArrayList<String>();
			for(String k:getCollection().keySet())
				if(s.equalsIgnoreCase(k)) {
					ArrayList<String> temp = getCollection().get(k);
					for(String v:temp)
						if (v.toLowerCase().startsWith(t.toLowerCase()))
							tempList.add(v);
					return tempList;
				}

			return null;
		}
	}	
	/**
	 * This class stores the input streams (body) created from either an HttpRequest or an HttpResponse.<br/>
	 * It can be used to create a body from:
	 * <ul>
	 * <li><b>a byte array</b>
	 * <li><b>a string</b>
	 * <li><b>a file</b>
	 * </ul>
	 * 
	 * The addBody method adds multiple bodies to the HttpBody and is used by HttpRequest.<br/>
	 * to account for multipart requests.
	 * <p/>
	 * The setBody method ensures that only one body exists <br/>
	 * and is used by HttpResponse.
	 */
	public static class HttpBody {

		private static final String TAG = "HttpBody";

		private ArrayList<HttpBodyEntity> _httpBodyEntities;
		private ArrayList<File> _tmpFiles;

		public HttpBody() {
			_httpBodyEntities = new ArrayList<HttpBodyEntity>();
			_tmpFiles = new ArrayList<File>();
		}
		/**
		 * @param body A string element to add to the body list.
		 */
		public HttpBody(String body) {
			this();
			addBody(body);
		}
		/**
		 * @param body A byte array element to add to the body list.
		 */
		public HttpBody(byte[] body) {
			this();
			addBody(body);
		}
		/**
		 * @param body A file element to add to the body list. 
		 * @param isTempFile The boolean determines if the file is deleted when resource is collected.<br/>
		 * <b>DO NOT</b> set to true if returning static content in a response.
		 */
		public HttpBody(File body, boolean isTempFile) throws FileNotFoundException {
			this();
			addBody(body, isTempFile);
		}
		/**
		 * @param body A string element to set as the body.
		 */
		public void setBody(String body) {
			dispose();
			_httpBodyEntities = new ArrayList<HttpBodyEntity>();
			_tmpFiles = new ArrayList<File>();
			addBody(body);
		}
		/**
		 * @param body A byte array element to set as the body.
		 */
		public void setBody(byte[] body) {
			dispose();
			_httpBodyEntities = new ArrayList<HttpBodyEntity>();
			addBody(body);
		}
		/**
		 * @param body A file element to set as the body. 
		 * @param isTempFile The boolean determines if the file is deleted when resource is collected.<br/>
		 * <b>DO NOT</b> set to true if returning static content in an HttpResponse.
		 */
		public void setBody(File body, boolean isTempFile) throws FileNotFoundException {
			dispose();
			_httpBodyEntities = new ArrayList<HttpBodyEntity>();
			addBody(body, isTempFile);
		}
		/**
		 * @param body A string element to add to the body list.
		 */
		public void addBody(String body) {
			addBody(body, new HttpMultiValueCollection());
		}
		/**
		 * @param body A string element to add to the body list.
		 * @param bodyMetadata Used by HttpRequest when parsing multipart. Stores body headers.
		 */
		public void addBody(String body, HttpMultiValueCollection bodyMetadata) {
			byte[] tempBuffer = null;
			try {
				tempBuffer = body.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				Logger.error(TAG, "UnsupportedEncodingException");
			}

			InputStream bodyData = new ByteArrayInputStream(tempBuffer);
			long bodyLength = tempBuffer.length;

			_httpBodyEntities.add(new HttpBodyEntity(bodyData, bodyLength, bodyMetadata));
		}
		/**
		 * @param body A byte array element to add to the body list.
		 */
		public void addBody(byte[] body) {
			addBody(body, new HttpMultiValueCollection());
		}
		/**
		 * @param body A byte array element to add to the body list.
		 * @param bodyMetadata Used by HttpRequest when parsing multipart. Stores body headers.
		 */
		public void addBody(byte[] body, HttpMultiValueCollection bodyMetadata) {
			InputStream bodyData = new ByteArrayInputStream(body);
			long bodyLength = body.length;

			_httpBodyEntities.add(new HttpBodyEntity(bodyData, bodyLength, new HttpMultiValueCollection()));
		}
		/**
		 * @param body A file element to add to the body list.
		 * @param isTempFile If declared true, file will be deleted when object is disposed.
		 * <b>DO NOT</b> set to true if returning static content in an HttpResponse.
		 */
		public void addBody(File body, boolean isTempFile) throws FileNotFoundException {
			addBody(body, new HttpMultiValueCollection(), isTempFile);
		}
		/**
		 * @param body A file element to add to the body list.
		 * @param bodyMetadata Used by HttpRequest when parsing multipart. Stores body headers.
		 * @param isTempFile If declared true, file will be deleted when object is disposed.
		 * <b>DO NOT</b> set to true if returning static content in an HttpResponse.
		 */
		public void addBody(File body, HttpMultiValueCollection bodyMetadata, boolean isTempFile) throws FileNotFoundException {
			InputStream bodyData = null;

			try {
				if (isTempFile)
					_tmpFiles.add(body);
				bodyData = new FileInputStream(body);
			} catch (FileNotFoundException e) {
				Logger.error(TAG, "Exception:" + (e.getMessage() != null ? e.getMessage() : "Empty Exception"));
				throw e;
			}

			long bodyLength = body.length();

			_httpBodyEntities.add(new HttpBodyEntity(bodyData, bodyLength, new HttpMultiValueCollection()));
		}	
		/**
		 * @return Total number of bodies.
		 */
		public int getBodyCount() {
			return _httpBodyEntities.size();
		}	
		/**
		 * @param index Index in array to get.
		 * @return an InputStream for a body.
		 */
		public InputStream getBody(int index) {		
			return _httpBodyEntities.get(index)._bodyData;
		}	
		/**
		 * @return an ArrayList of body InputStreams
		 */
		public ArrayList<InputStream> getBodies() {
			ArrayList<InputStream> tmp = new ArrayList<InputStream>();
			for (HttpBodyEntity hbe : _httpBodyEntities)
				tmp.add(hbe.getBodyData());
			return tmp;
		}
		/**
		 * @param body Takes an InputStream of a body.
		 * @return The size of the InputStream.
		 */
		public long getBodyLength(InputStream body) {
			for (HttpBodyEntity hbe : _httpBodyEntities)
				if (hbe.getBodyData() == body)
					return hbe.getBodyLength();

			return -1;
		}
		/**
		 * @param body Takes an InputStream of a body.
		 * @return the body headers collected during an HttpRequest.
		 */
		public HttpMultiValueCollection getBodyMetadata(InputStream body) {
			for (HttpBodyEntity hbe : _httpBodyEntities)
				if (hbe.getBodyData() == body)
					return hbe.getBodyMetadata();

			return null;
		}
		/**
		 * Close all body InputStreams and delete temp files used as bodies.
		 */
		public void dispose() {
			try {
				for (HttpBodyEntity hbe:_httpBodyEntities)
					hbe.getBodyData().close();
				_httpBodyEntities.clear();

				for (File f:_tmpFiles)
					f.delete();
				_tmpFiles.clear();
			} catch (Exception e) {
				//Ignore errors during cleanup
			}
		}

		@Override
		protected void finalize() throws Throwable {
			this.dispose();
		}

		private class HttpBodyEntity {

			@SuppressWarnings("unused")
			private static final String TAG = "HttpBodyEntity";

			private InputStream _bodyData;
			private long _bodyLength;
			private HttpMultiValueCollection _bodyMetadata;

			private HttpBodyEntity(InputStream bodyData, long bodyLength, HttpMultiValueCollection bodyMetadata) {
				_bodyData = bodyData;
				_bodyLength = bodyLength;
				_bodyMetadata = bodyMetadata;
			}

			private InputStream getBodyData() {
				return _bodyData;
			}

			@SuppressWarnings("unused")
			private void setBodyData(InputStream bodyData) {
				this._bodyData = bodyData;
			}

			private long getBodyLength() {
				return _bodyLength;
			}

			@SuppressWarnings("unused")
			private void setBodyLength(long bodyLength) {
				this._bodyLength = bodyLength;
			}

			private HttpMultiValueCollection getBodyMetadata() {
				return _bodyMetadata;
			}

			@SuppressWarnings("unused")
			private void setBodyMetadata(HttpMultiValueCollection bodyMetadata) {
				this._bodyMetadata = bodyMetadata;
			}
		}
	}


}
