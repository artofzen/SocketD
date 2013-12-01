package cy.nicosia.zenont.net;

/**
 * Interface which defines expected functionality from all classes that inherit from
 * abstract class SocketD. Implemented by SocketD abstract class.
 */
public interface ISocketD {

	/**
	 * Use to setup and start any classes implementing ISocketD.
	 * 
	 * @return boolean True if executed successfully.
	 */
	public boolean start();
	/**
	 * Use to stop any classes implementing ISocketD. 
	 */
	public void stop();
	
}
