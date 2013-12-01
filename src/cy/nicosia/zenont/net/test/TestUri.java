package cy.nicosia.zenont.net.test;

import java.net.URI;

public class TestUri {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new TestUri().exec();
	}
	
	public void exec() {
		
		String[] urls = new String[] {
				"/test.html?test+1=%201",
				"https://www.google.com/",
				"http://www.google.com/",
				"http://www.google.com:80/",
				"http://www.google.com/?test=1",
				"http://www.google.com?test=1",
				"http://www.google.com/?test1=1&test2=2",
				"http://www.google.com",
                 "http://www.google.com:80",
				"/",
				"/test.html",
				"/test.html?test1=1",
				"/test.html?test1=1&test2=2",
				"/test.html/?test1=1"
		};
		
		for (String s : urls) {
			System.out.println(s);
			System.out.println(URI.create(s).toString());
		}
		
	}

}
