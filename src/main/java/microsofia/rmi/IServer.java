package microsofia.rmi;

/**
 * The remote interface of a server.
 * */
public interface IServer {
	
	/**
	 * The address of the server.
	 * 
	 * @return the address of the server
	 * */
	public ServerAddress getServerAddress();

	/**
	 * The registry associated to the server.
	 * 
	 * @return associated registry
	 * */
	public IRegistry getRegistry();
}
