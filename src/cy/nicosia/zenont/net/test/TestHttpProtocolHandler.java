package cy.nicosia.zenont.net.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import cy.nicosia.zenont.net.protocol.HttpProtocol;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.HttpMultiValueCollection;
import cy.nicosia.zenont.net.protocol.http.HttpMessage.MimeTypes;
import cy.nicosia.zenont.net.protocol.http.HttpRequest;
import cy.nicosia.zenont.net.protocol.http.HttpResponse;
import cy.nicosia.zenont.net.protocol.http.HttpSessionManager.HttpSession;

public class TestHttpProtocolHandler extends HttpProtocol {

	static ArrayList<HttpSession> validSession;
	
	static {
		validSession = new ArrayList<HttpSession>();
	}
	
	@Override
	public HttpResponse executeRequest(HttpSession session, HttpRequest httpRequest) {
		HttpResponse resp = null;
		try {
			//return new HttpResponse(HttpResponse.OK, null, "<html><title>Hello!</title><body><p/>Got your <b>request</b>!</body></html>");
			String rootdir = "/Users/zenon/website";
			String file = httpRequest.getUri().getPath();
			/*
			if (!validSession.contains(session)) {			
				file = "/login.html";
				validSession.add(session);
			}
			
			file = file.replace("/", "\\"); */
			resp = new HttpResponse();
			resp.setHttpResponseStatus(HttpResponse.Status.OK);
			HttpMultiValueCollection mv = new HttpMultiValueCollection();
			
			if (file.endsWith("pdf"))
				mv.appendValueToKey("Content-Type", MimeTypes.PDF);
			else if (file.endsWith("htm") || file.endsWith("html"))
				mv.appendValueToKey("Content-Type", MimeTypes.HTML);
			
			resp.setHeaders(mv);
			resp.setBody(new File(rootdir + file), false);
			return resp;
		} catch (FileNotFoundException f) {
			resp = new HttpResponse(HttpResponse.Status.NOT_FOUND, null, HttpResponse.Status.NOT_FOUND);
			return resp;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
