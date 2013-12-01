package cy.nicosia.zenont.net.protocol;

/**
 * Interface for classes implementing a socket protocol.
 */
public interface IProtocol {
	
	/**
	 * After implementing this interface use the transferable object to move any objects or data from the 
	 * socket handler to your implementation of a protocol handler.
	 * @param transferable Data or Object
	 * @throws Exception 
	 */
	public abstract void exec(Object... transferable) throws Exception;
	public abstract void dispose();
}
