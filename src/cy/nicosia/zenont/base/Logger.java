package cy.nicosia.zenont.base;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Generic logging class
 */
public abstract class Logger {
	
	public static boolean _debug = true; 
	
	private static final DateFormat df = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.sss");
	
	public static void debug(String origin, String msg) {
		if (_debug)
			System.out.println(df.format(Calendar.getInstance().getTime()) + " DEBUG:" + origin + ":" + msg);
	}
	
	public static void error(String origin, String msg) {
		System.out.println(df.format(Calendar.getInstance().getTime()) + " ERROR:" + origin + ":" + msg);
	}
	
	public static void error(String origin, Exception ex) {
		Logger.error(origin, "Exception:" + (ex.getMessage() != null ? ex.getMessage() : "Empty message"));
	}
	
}
