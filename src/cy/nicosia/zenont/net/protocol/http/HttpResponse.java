package cy.nicosia.zenont.net.protocol.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpBody;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpMultiValueCollection;

public class HttpResponse {

	public static final String TAG = "HttpResponse";

	private String _httpResponseStatus;
	private HttpMultiValueCollection _headers;
	private HttpBody _body;

	public HttpResponse() {
		this(HttpResponse.Status.OK, new HttpMultiValueCollection(), new HttpBody());
	}

	private HttpResponse(String httpResponseStatus, HttpMultiValueCollection httpHeaders, HttpBody body) {
		assert(httpResponseStatus != null) : "Response Status cannot be null";
		_httpResponseStatus = httpResponseStatus;
		_headers = httpHeaders;
		_body = body;
	}

	public HttpResponse(String httpResponseStatus, HttpMultiValueCollection httpHeaders, String body) {
		this(httpResponseStatus, new HttpMultiValueCollection(), new HttpBody());
		setBody(body);
	}

	public HttpResponse(String httpResponseStatus, 
			HttpMultiValueCollection httpHeaders, File body, boolean isTempFile) throws FileNotFoundException  {
		this(httpResponseStatus, new HttpMultiValueCollection(), new HttpBody());
		setBody(body, isTempFile);
	}

	public HttpResponse(String httpResponseStatus, HttpMultiValueCollection httpHeaders, byte[] body)  {
		this(httpResponseStatus, new HttpMultiValueCollection(), new HttpBody());
		setBody(body);
	}

	public String getHttpResponseLine() {
		return "HTTP/1.0 " + _httpResponseStatus + "\r\n";
	}
	/**
	 * @return a string object formatted for output in a HTTP request or response.
	 */
	public String getHeadersResponseFormat() {
		return _headers.toString();
	}

	public String geHttpResponseStatus() {
		return _httpResponseStatus;
	}

	public void setHttpResponseStatus(String httpResponseStatus) {
		this._httpResponseStatus = httpResponseStatus;
	}

	public HttpMultiValueCollection getHeaders() {
		return _headers;
	}

	public void setHeaders(HttpMultiValueCollection httpHeaders) {
		this._headers = httpHeaders;
	}

	public void setBody(byte[] body) {
		setBody(body);
	}

	public void setBody(byte[] body, HttpMultiValueCollection bodyMetadata) {
		_body.setBody(body);
	}

	public void setBody(String body) {
		setBody(body, new HttpMultiValueCollection());
	}

	public void setBody(String body, HttpMultiValueCollection bodyMetadata) {
		_body.setBody(body);
	}

	public void setBody(File body, boolean isTempFile) throws FileNotFoundException {
		setBody(body, new HttpMultiValueCollection(), isTempFile);
	}

	public void setBody(File body, HttpMultiValueCollection bodyMetadata, 
			boolean isTempFile) throws FileNotFoundException {
		_body.setBody(body, isTempFile);
	}

	public InputStream getBody() {
		return _body.getBody(0);
	}

	public long getBodyLength(InputStream body) {
		return _body.getBodyLength(body);
	}

	public void dispose() {
		_httpResponseStatus = null;
		_headers = null;
		_body.dispose();
		_body = null;
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose();
	}

	public static abstract class Status {
		//HttpResponse Status tags
		public static final String OK = "200 OK"; 
		public static final String CREATED = "201 Created";
		public static final String NO_CONTENT = "204 No Content";
		public static final String PARTIAL_CONTENT = "206 Partial Content";
		public static final String REDIRECT = "301 Moved Permanently";
		public static final String NOT_MODIFIED = "304 Not Modified";
		public static final String BAD_REQUEST = "400 Bad Request";
		public static final String UNAUTHORIZED = "401 Unauthorized";
		public static final String FORBIDDEN = "403 Forbidden";
		public static final String NOT_FOUND = "404 Not Found";
		public static final String REQUEST_TIMEOUT = "408 Request Timeout";
		public static final String INTERNAL_ERROR = "500 Internal Server Error";
	}

}