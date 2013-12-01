package cy.nicosia.zenont.net.protocol.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpBody;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpMultiValueCollection;

public class HttpRequest {

	@SuppressWarnings("unused")
	private static final String TAG = "HttpRequest";

	private String _method;
	private URI _uri;
	private String _protocolVersion;
	private HttpMultiValueCollection _headers;
	private HttpBody _body; 
	private Map<String, String> _parameters;

	public HttpRequest() {
		this(Method.GET, URI.create("/"), ProtocolVersion.HTTP_1_0);
	}

	public HttpRequest(String method, URI uri, String protocolVersion) {
		this(method, uri, protocolVersion, new HttpMultiValueCollection(), 
				new HashMap<String, String>(),  new HttpBody());
	}

	public HttpRequest(String method, URI uri, String protocolVersion,
			HttpMultiValueCollection httpHeaders, Map<String, String> parameters, HttpBody body) {
		_method = method;
		_uri = uri;
		_protocolVersion = protocolVersion;
		_headers = httpHeaders;
		_body = body;
		_parameters = parameters;
	}

	public String getMethod() {
		return _method;
	}

	public void setMethod(String method) {
		_method = method;
	}
	
	public URI getUri() {
		return _uri;
	}
	
	public void setUri(URI uri) {
		_uri = uri;
	}

	public String getProtocolVersion() {
		return _protocolVersion;
	}
	
	public void setProtocolVersion(String protocolVersion) {
		_protocolVersion = protocolVersion;
	}

	public HttpMultiValueCollection getHeaders() {
		return _headers;
	}
	
	public void setHeaders(HttpMultiValueCollection headers) {
		_headers = headers;
	}

	public Map<String, String> getParameters() {
		return _parameters;
	}
	
	public void setParameters(Map<String, String> parameters) {
		_parameters = parameters;
	}

	public void addBody(byte[] body) {
		addBody(body, new HttpMultiValueCollection());
	}
	
	public void addBody(byte[] body, HttpMultiValueCollection bodyMetadata) {
		_body.addBody(body, bodyMetadata);
	}
	
	public void addBody(String body) {
		addBody(body, new HttpMultiValueCollection());
	}
	
	public void addBody(String body, HttpMultiValueCollection bodyMetadata) {
		_body.addBody(body, bodyMetadata);
	}
	
	public void addBody(File body, boolean isTempFile) throws FileNotFoundException {
		addBody(body, new HttpMultiValueCollection(), isTempFile);
	}
	
	public void addBody(File body, HttpMultiValueCollection bodyMetadata, 
			boolean isTempFile) throws FileNotFoundException {
		_body.addBody(body, bodyMetadata, isTempFile);
	}

	public int getBodyCount() {
		return _body.getBodyCount();
	}
	
	public ArrayList<InputStream> getBodies() {
		return _body.getBodies();
	}
	
	public long getBodyLength(InputStream body) {
		return _body.getBodyLength(body);
	}
	
	public HttpMultiValueCollection getBodyMetadata(InputStream body) {
		return _body.getBodyMetadata(body);
	}
	
	public String toString() {
		return _method + "\n" + _uri.getPath() + "\n" + _protocolVersion + "\nHeaders:\n" +
				((_headers != null ? _headers.toString() : "") +
						"\nParameters:\n" + _parameters.toString());
	}
	
	public void dispose() {
		_method = null;
		_uri = null;
		_protocolVersion = null;
		_headers = null;
		_parameters = null;
		_body.dispose();
		_body = null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.dispose();
	}
	
	public static abstract class Method {

		public static final String TAG = "Method";

		public static final String[] METHODS = { "GET", "PUT", "POST", "DELETE" };
		public static final String GET = METHODS[0];
		public static final String PUT = METHODS[1];
		public static final String POST = METHODS[2];
		
		public static String validate(String method) {
			for (String s : METHODS)
				if (s.equals(method))
					return method;

			throw new IllegalArgumentException("Invalid Method Specified");
		}
	}
	
	public static abstract class ProtocolVersion {

		public static final String TAG = "ProtocolVersion";

		public static final String[] PROTOCOL_VERSIONS = {"HTTP/1.0", "HTTP/1.1"};
		public static final String HTTP_1_0 = PROTOCOL_VERSIONS[0];
		public static final String HTTP_1_1 = PROTOCOL_VERSIONS[1];

		public static String validate(String protocolVersion) {
			for (String s : PROTOCOL_VERSIONS)
				if (s.equals(protocolVersion))
					return protocolVersion;

			throw new IllegalArgumentException("Invalid Protocol Version Specified");
		}
	}
}
