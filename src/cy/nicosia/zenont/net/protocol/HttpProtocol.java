package cy.nicosia.zenont.net.protocol;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import cy.nicosia.zenont.base.ConfigManager;
import cy.nicosia.zenont.base.ConfigManager.Config;
import cy.nicosia.zenont.base.DynamicByteArray;
import cy.nicosia.zenont.base.DynamicByteArray.ByteArrayUtils;
import cy.nicosia.zenont.base.Logger;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpMultiValueCollection;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.MimeTypes;
import cy.nicosia.zenont.net.protocol.http.HttpRequest;
import cy.nicosia.zenont.net.protocol.http.HttpRequest.Method;
import cy.nicosia.zenont.net.protocol.http.HttpRequest.ProtocolVersion;
import cy.nicosia.zenont.net.protocol.http.HttpResponse;
import cy.nicosia.zenont.net.protocol.http.HttpSessionManager;
import cy.nicosia.zenont.net.protocol.http.HttpSessionManager.HttpSession;

/**
 * Abstract implementation of the HTTP protocol 1.0<br/>
 * This class needs to be extended and method<p/>
 * <code>public HttpResponse executeRequest(HttpRequest httpRequest)</code><p/>
 * needs to be implemented in the subclass.
 * Implementations will receive a valid HTTP request to process and must return a response.
 */
public abstract class HttpProtocol implements IProtocol {

	private static final String TAG = "HttpProtocol";
	private static final byte[] DOUBLE_EOL = new byte[] {'\r', '\n', '\r', '\n'};

	private Socket _client;
	private InputStream _inputStream;
	private OutputStream _outputStream;
	private HttpProtocolConfig _cfg;

	protected HttpSession _session;
	protected ConfigManager _configManager;
	/**
	 * Override this method in your implementation to receive the request
	 * for processing.
	 * @param httpRequest is a request coming from the client.
	 * @return a response to be sent to client.
	 */
	public abstract HttpResponse executeRequest(HttpSession session, HttpRequest httpRequest);
	/* (non-Javadoc)
	 * @see cy.nicosia.zenont.net.protocol.TcpProtocolBase#exec(java.lang.Object[])
	 */
	@Override
	public final void exec(Object... transferable) throws Exception {
		Logger.debug(TAG,"Exec started");
		assert (transferable != null && transferable.length > 1) : "No transferables found";
		_configManager = (ConfigManager)transferable[0];
		Socket clientSocket = (Socket)transferable[1];
		_cfg = (HttpProtocolConfig) _configManager.getConfig(HttpProtocolConfig.class);
		setClient(clientSocket);

		//Used to hold data from input stream
		byte[] buffer = new byte[_cfg.getConfigBufferLength()];
		//Used to hold leftover bytes from buffer after it has been parsed
		DynamicByteArray holdingBuffer = new DynamicByteArray();
		//Default Timeout
		getClient().setSoTimeout(_cfg.getConfigDefaultConnectionTimeoutSeconds());
		do {

			HttpRequest request = new HttpRequest();
			HttpResponse response = null;

			//create HttpRequest
			try {
				receive(request, holdingBuffer, buffer);
			} catch (Exception e) {
				Logger.error(TAG, e);
				if (request != null) {
					request.dispose();
					request = null;
				}
				//close connection
				throw e;
			} 

			//Session write control variable
			boolean isNewSession = true;
			//establish HttpSession
			try {
				String previousKey = null;
				String sessionKey = null;
				if (_cfg.isConfigSessionEnabled()) {

					//Only get first Index
					if (request.getHeaders().hasKeyValueStartsWith(
							"Cookie", _cfg.getConfigSessionCookieIdentifier())) {

						sessionKey = request.getHeaders().getFirstKeyValueStartsWith(
								"Cookie", _cfg.getConfigSessionCookieIdentifier());

						sessionKey = sessionKey.replace(_cfg.getConfigSessionCookieIdentifier() + "=", "");
						previousKey = sessionKey;
					}

					_session = HttpSessionManager.getInstance(_configManager).getSession(sessionKey);

					//Check if it is a new _session
					if (previousKey != null && previousKey.equals(_session.getSessionKey()))
						isNewSession = false;
				}
			} catch (Exception e) {
				Logger.error(TAG, e);
				//close connection
				throw e;
			} 

			//create HttpResponse
			try {
				response = executeRequest(_session, request);

				//Set the cookie if it is a new _session
				if (_cfg.isConfigSessionEnabled() && isNewSession) {	
					String sessionKey = null;

					sessionKey = _cfg.getConfigSessionCookieIdentifier() + "=" + _session.getSessionKey() + ";path=/";

					if (sessionKey != null) {
						response.getHeaders().appendValueToKey("Set-Cookie", sessionKey);
					}
				}

				send(response);

			} catch (Exception e) {
				Logger.error(TAG, e);
				HttpResponse errorResponse = 
						new HttpResponse(HttpResponse.Status.INTERNAL_ERROR, 
								null, HttpResponse.Status.INTERNAL_ERROR);

				send(errorResponse);	
				//close connection
				throw e;
			} finally {
				//Cleanup (files and streams open)
				if (request != null) {
					request.dispose();
					request = null;
				}
				response = null;
			}
		} while (!getClient().isClosed());
	}

	@Override
	public void dispose() {
		try {
			Logger.debug(TAG, "Closing connection.");
			if (_client != null)
				_client.close();
		} catch (Exception e) {
			Logger.error(TAG, "Exception during cleanup");
		}
	}
	/**
	 * @return the client socket.
	 */
	public final Socket getClient() {
		return _client;
	}
	/**
	 * Set the client socket and get the InputStream and OutputStream.
	 * @param clientSocket
	 */
	private final void setClient(Socket clientSocket) {
		this._client = clientSocket;
		try {
			this._inputStream = ((Socket)clientSocket).getInputStream();
			this._outputStream = ((Socket)clientSocket).getOutputStream();
		} catch (Exception e) {
			Logger.error(TAG, e);
		}
	}
	/**
	 * @return the input stream for the client
	 */
	public final InputStream getInputStream() {
		return _inputStream;
	}
	/**
	 * @return the output stream for the client
	 */
	public final OutputStream getOutputStream() {
		return _outputStream;
	}

	private final void receive(HttpRequest request, DynamicByteArray holdingBuffer, byte[] buffer) throws Exception {

		assert holdingBuffer != null : "holdingBuffer is null";
		assert buffer != null : "buffer is null";

		Logger.debug(TAG,"Receive started");

		//Get headers
		parseHeaders(request, holdingBuffer, buffer);

		//Get Body
		if (request.getMethod().equals(Method.POST) || request.getMethod().equals(Method.PUT)) {
			//Parse a multipart form
			if (request.getHeaders().hasKeyValue("Content-Type", MimeTypes.MULTIPART_FORM)) 
				parseMultiPartBody(
						request, request.getHeaders(), holdingBuffer, buffer);
			else 
				parseBody(request, holdingBuffer, buffer);
		}

	}

	private final void send(HttpResponse response) throws Exception {
		PrintWriter pw = null;
		try {
			OutputStream output = getOutputStream();
			//Check for needed headers
			if (response.getHeaders() == null)
				response.setHeaders(new HttpMultiValueCollection());

			if (!response.getHeaders().hasKey("Date")) {
				SimpleDateFormat gmtFrmt = 
						new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
				gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
				response.getHeaders().overwriteKeyValue("Date", gmtFrmt.format(new Date()));
			}

			if (!response.getHeaders().hasKey("Content-Length") && 
					response.getBodyLength(response.getBody()) > 0L) {
				response.getHeaders().overwriteKeyValue(
						"Content-Length", Long.toString(response.getBodyLength(response.getBody())));
			}

			pw = new PrintWriter((output));
			Logger.debug(TAG, response.getHttpResponseLine());
			pw.print(response.getHttpResponseLine());
			Logger.debug(TAG, response.getHeadersResponseFormat());
			pw.print(response.getHeadersResponseFormat());
			pw.print("\r\n");
			pw.flush();

			if (response.getBody() != null) {
				int totalRead = 0;
				byte[] buffer = new byte[_cfg.getConfigBufferLength()];

				while (totalRead < response.getBodyLength(response.getBody())) {
					int read = 0;
					read = response.getBody().read(buffer, 0, _cfg.getConfigBufferLength());
					totalRead += read;
					output.write(buffer,0, read);
				}
				output.flush();
			}	
		} catch (Exception e) {
			//Do nothing - just cleanup below
		} finally {
			pw.close();
			response.dispose();
			response = null;
		}
	}

	private final void parseHeaders(HttpRequest request, 
			DynamicByteArray holdingBuffer, byte[] buffer) throws Exception {

		HttpResponse errorResponse = null;
		//Bytes read into buffer
		int read = 0;
		int splitIndex = -1;
		try {
			do {
				read = getInputStream().read(
						buffer, 0, _cfg.getConfigBufferLength()); //read exceptions caught by exec

				//append any previously unparsed data to newly received data
				holdingBuffer.concatenate(buffer, read);

				//Check to see if we have a double EOL in our buffer
				splitIndex = holdingBuffer.find(DOUBLE_EOL);
			}  while (read != -1 && splitIndex == -1);

			Logger.debug(TAG, "Parsing headers");

			BufferedReader bReader = 
					new BufferedReader(
							new InputStreamReader(
									new ByteArrayInputStream(holdingBuffer.getArray(), 0, splitIndex)));

			//Parse Request Line
			{
				String line = "";				
				//Ignore empty lines preceding request line
				do {line = bReader.readLine();} while (line.equals(""));

				Logger.debug(TAG, line);
				//Get request line
				String method = Method.validate(line.substring(0, line.indexOf(" ")));
				request.setMethod(method);
				line = line.substring(line.indexOf(" ") + 1);

				URI uri = URI.create(line.substring(0, line.indexOf(" ")));
				request.setUri(uri);
				line = line.substring(line.indexOf(" ") + 1);

				String protocolVersion = ProtocolVersion.validate(line);
				request.setProtocolVersion(protocolVersion);

				Map<String, String> parameters = null;
				//Parse any parameters if GET was requested
				if (method.equals(Method.GET) &&
						(line = uri.getQuery()) != null) {
					line = parsePercentEncoding(line);
					parameters = parseParameters(line);
				}
				request.setParameters(parameters);
			}//End Parse Request Line

			//Parse Headers
			{
				HttpMultiValueCollection headers = parseKeyValue(bReader);

				request.setHeaders(headers);
				//extract remaining data which might include body or next request
				holdingBuffer.shift(splitIndex);

				Logger.debug(TAG, headers.toString());

			}//End Parse Headers

			//finished parsing headers
			bReader.close();
		} catch (SocketTimeoutException ex) {
			if (read != -1 && errorResponse == null) {
				errorResponse = 
						new HttpResponse(
								HttpResponse.Status.REQUEST_TIMEOUT, null, HttpResponse.Status.REQUEST_TIMEOUT);
				send(errorResponse);
			}
			throw ex;
		} catch (Exception ex){
			//Catch BAD REQUESTS and send response
			//If the client connects and immediately disconnects read == -1
			if (read != -1 && errorResponse == null) {
				errorResponse = 
						new HttpResponse(
								HttpResponse.Status.BAD_REQUEST, 
								null, HttpResponse.Status.BAD_REQUEST + "-Invalid Headers");
				send(errorResponse);
			}
			throw ex;
		}
	}

	private final void parseBody(HttpRequest request, 
			DynamicByteArray holdingBuffer, byte[] buffer) throws Exception {

		//Bytes read into buffer
		int read = 0;
		try {
			int contentLength = 
					Integer.parseInt(request.getHeaders().getKeyValues("Content-Length").get(0));

			String parameterLine = null;

			//Get complete body according to content length
			while (read != -1 && holdingBuffer.length() < contentLength) {
				read = getInputStream().read(
						buffer, 0, _cfg.getConfigBufferLength()); //read exceptions caught by exec

				//append any previously unparsed data to newly received data
				holdingBuffer.concatenate(buffer, read);
			}

			Logger.debug(TAG, "Parsing body");

			ByteArrayInputStream bin = new ByteArrayInputStream(holdingBuffer.getArray(), 0, contentLength);
			byte[] body;
			//get application/x-www-form-urlencoded
			if (request.getHeaders().hasKeyValue("Content-Type", MimeTypes.URLENCODED)) {
				BufferedReader bReader = new BufferedReader(new InputStreamReader(bin));

				char[] params = new char[contentLength];
				bReader.read(params, 0, contentLength);
				parameterLine = parsePercentEncoding(new String(params));
				request.setParameters(parseParameters(parameterLine));

				body = new byte[0];
			} else { //get raw body
				body = new byte[contentLength];
				bin.read(body, 0, contentLength);
			}

			//extract remaining data to return which might include body or next request
			holdingBuffer.shift(contentLength);

			request.addBody(body);
		} catch (Exception ex){
			//Catch BAD REQUESTS and send response
			//If the client connects and immediately disconnects read == -1
			if (read != -1) {
				HttpResponse errorResponse = 
						new HttpResponse(
								HttpResponse.Status.BAD_REQUEST, 
								null, HttpResponse.Status.BAD_REQUEST + "-Invalid Body");
				send(errorResponse);
			}
			throw ex;
		}

	}

	private final void parseMultiPartBody(HttpRequest request, HttpMultiValueCollection metadata,
			DynamicByteArray holdingBuffer, byte[] buffer) throws Exception {

		int read = 0;

		try {
			//Get boundary
			String startBoundary = metadata.getKeyValueStartsWith("Content-Type", "boundary=").get(0);
			startBoundary = startBoundary.replace("boundary=", "").replace("\"", "").trim();
			startBoundary = "--" + startBoundary;
			String endBoundary = startBoundary + "--";
			byte[] startBoundaryBytes = startBoundary.getBytes("UTF-8");
			byte[] endBoundaryBytes = endBoundary.getBytes("UTF-8");

			int splitIndex = -1;

			Logger.debug(TAG, "Parsing multipart");

			do {
				//Find 2 EOL in holdingBuffer
				splitIndex = holdingBuffer.find(DOUBLE_EOL);

				while (read != -1 && splitIndex == -1) {

					read = getInputStream().read(buffer, 0, _cfg.getConfigBufferLength());

					//append any previously unparsed data to newly received data
					holdingBuffer.concatenate(buffer, read);

					splitIndex = holdingBuffer.find(DOUBLE_EOL);
				}

				Logger.debug(TAG, "Found boundary");

				BufferedReader bReader = 
						new BufferedReader(
								new InputStreamReader(
										new ByteArrayInputStream(holdingBuffer.getArray(), 0, splitIndex)));

				Logger.debug(TAG, "Parsing body metadata");
				//Dispose of boundary
				bReader.readLine();
				//Parse body metadata
				HttpMultiValueCollection bodyMetadata = parseKeyValue(bReader);

				//finished parsing headers
				bReader.close();
				//extract remaining data
				holdingBuffer.shift(splitIndex);

				//Inner boundary found
				if (bodyMetadata.hasKeyValue("Content-Type", MimeTypes.MULTIPART_MIXED))
					parseMultiPartBody(request, bodyMetadata, holdingBuffer, buffer);

				if (bodyMetadata.hasKeyValueStartsWith("Content-Disposition", "filename=")) {
					Logger.debug(TAG, "Found file body");

					String tmpFilename = bodyMetadata.getFirstKeyValueStartsWith(
							"Content-Disposition", "filename=");

					tmpFilename = tmpFilename.replace("filename=", "").replace("\"", "").trim();

					File tmpFile = File.createTempFile("$SocketD-" + tmpFilename, ".tmpsd");
					Logger.debug(TAG, "File absolute path: " + tmpFile.getAbsolutePath());

					FileOutputStream fos = new FileOutputStream(tmpFile);

					splitIndex = holdingBuffer.find(startBoundaryBytes);

					while (read != -1 && splitIndex == -1) {

						read = getInputStream().read(buffer, 0, _cfg.getConfigBufferLength());

						//append any previously unparsed data to newly received data
						holdingBuffer.concatenate(buffer, read);

						splitIndex = holdingBuffer.find(startBoundaryBytes);

					}

					splitIndex = splitIndex - startBoundaryBytes.length;

					fos.write(holdingBuffer.getArray(), 0, splitIndex - 2); //Remove newline
					fos.flush();
					fos.close();
					//extract remaining data
					holdingBuffer.shift(splitIndex);

					request.addBody(tmpFile, bodyMetadata, true);
				} else {
					Logger.debug(TAG, "Found body");
					splitIndex = holdingBuffer.find(startBoundaryBytes);

					while (read != -1 && splitIndex == -1) {

						read = getInputStream().read(buffer, 0, _cfg.getConfigBufferLength());

						//append any previously unparsed data to newly received data
						holdingBuffer.concatenate(buffer, read);

						splitIndex = holdingBuffer.find(startBoundaryBytes);

					}

					splitIndex = splitIndex - startBoundaryBytes.length;

					byte[] body = 
							ByteArrayUtils.extractBytesFromByteArray(
									holdingBuffer.getArray(), 
									0, 
									splitIndex - 2); //Remove newline after body

					//extract remaining data
					holdingBuffer.shift(splitIndex);

					request.addBody(body, bodyMetadata);
				}

				Logger.debug(TAG, "Finished parsing body");

				//Need to find next boundary or end boundary to know what to do
				do {

					//If we have the end boundary in buffer then we know if we can do the loop again
					//data in buffer needs to be bigger than end boundary to do loop again
					//controlled by while below
					splitIndex = holdingBuffer.find(endBoundaryBytes);

					//Found end boundary so break
					if (splitIndex != -1)
						break;
					//If no end boundary found check for a start boundary
					splitIndex = holdingBuffer.find(startBoundaryBytes);

					//If a start boundary is found read at least two more bytes to make sure
					//it is not a partial end boundary that looks like a start boundary
					if (splitIndex != -1) {
						int readCount = 0;
						//Read at least two more bytes
						while (read != -1 && readCount < 2) {
							read = getInputStream().read(buffer, 0, _cfg.getConfigBufferLength());
							//append any previously unparsed data to newly received data
							holdingBuffer.concatenate(buffer, read);
							readCount += read;
						}
						//Check again to see if it is indeed an end boundary
						splitIndex = holdingBuffer.find(endBoundaryBytes);
						break;
					}

					//Keep reading until we find either an end or start boundary
					read = getInputStream().read(buffer, 0, _cfg.getConfigBufferLength());
					//append any previously unparsed data to newly received data
					holdingBuffer.concatenate(buffer, read);

				} while (true && read != -1);

			} while (splitIndex == -1 || splitIndex != endBoundaryBytes.length);

			Logger.debug(TAG, "Found boundary end");

			//extract remaining data
			holdingBuffer.shift(splitIndex);

		} catch (Exception ex) {
			//Catch BAD REQUESTS and send response
			//If the client connects and immediately disconnects read == -1
			if (read != -1) {
				HttpResponse errorResponse = 
						new HttpResponse(
								HttpResponse.Status.BAD_REQUEST, 
								null, HttpResponse.Status.BAD_REQUEST + "-Invalid Body");

				send(errorResponse);
			}
			throw ex;
		}
	}

	private final Map<String, String> parseParameters(String parameterLine) {
		assert parameterLine != null;
		Map<String, String> parameters = new HashMap<String, String>();
		StringTokenizer parms = new StringTokenizer(parameterLine, "&");
		//Parse pairs
		while (parms.hasMoreTokens()) {
			StringTokenizer pair = new StringTokenizer(parms.nextToken(), "=");
			String key = pair.nextToken();
			String value = "";
			//Handle 'key=' case with no value
			if (pair.hasMoreTokens())
				value = pair.nextToken();
			
			parameters.put(key, value);		
		}

		return parameters;
	}

	private final String parsePercentEncoding(String uriString) {
		//Rebuild string and decode percent encoding
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < uriString.length(); i++) {
			char c = uriString.charAt(i);
			switch (c) {
			case '+':
				s.append(' ');
				break;
			case '%':
				s.append((char) Integer.parseInt(uriString.substring(i + 1, i + 3), 16));
				i += 2;
				break;
			default:
				s.append(c);
				break;		
			}
		}

		return s.toString();
	}

	private final HttpMultiValueCollection parseKeyValue(BufferedReader reader) throws Exception {

		String line = null;

		HttpMultiValueCollection pair = new HttpMultiValueCollection();
		while ((line = reader.readLine()) != null) {
			StringTokenizer eol = new StringTokenizer(line,"\r\n");
			//Parse lines
			while (eol.hasMoreTokens()) {
				StringTokenizer colon = new StringTokenizer(eol.nextToken(), ":");
				String key = colon.nextToken().trim();
				StringTokenizer sep = new StringTokenizer(colon.nextToken(), ",;");

				while (sep.hasMoreTokens()) {
					String value = sep.nextToken().trim();
					pair.appendValueToKey(key, value);
				};			
			}
		}

		return pair;

	}

	public static class HttpProtocolConfig extends Config {

		@SuppressWarnings("unused")
		private static final String TAG = "HttpProtocol Config";

		//Configuration settings
		private volatile int _configBufferLength;
		private volatile int _configDefaultConnectionTimeoutSeconds;
		private volatile boolean _configSessionEnabled;
		private volatile String _configSessionCookieIdentifier;

		//defaults
		{
			setConfigBufferLength(4 * 1024);
			setConfigDefaultConnectionTimeoutSeconds(20);
			setConfigSessionEnabled(true);
			setConfigSessionCookieIdentifier("SocketD");
		}

		public int getConfigBufferLength() {
			return _configBufferLength;
		}

		public void setConfigBufferLength(int configBufferLength) {
			this._configBufferLength = configBufferLength;
		}

		public int getConfigDefaultConnectionTimeoutSeconds() {
			return _configDefaultConnectionTimeoutSeconds;
		}

		public void setConfigDefaultConnectionTimeoutSeconds(
				int configDefaultConnectionTimeoutSeconds) {
			this._configDefaultConnectionTimeoutSeconds = configDefaultConnectionTimeoutSeconds * 1000;
		}

		public boolean isConfigSessionEnabled() {
			return _configSessionEnabled;
		}

		public void setConfigSessionEnabled(boolean configSessionEnabled) {
			this._configSessionEnabled = configSessionEnabled;
		}

		public String getConfigSessionCookieIdentifier() {
			return _configSessionCookieIdentifier;
		}

		public void setConfigSessionCookieIdentifier(
				String configSessionCookieIdentifier) {
			this._configSessionCookieIdentifier = configSessionCookieIdentifier;
		}
	}
}	


