package microsofia.rmi;

/**
 * A listener of the client interests in objects exported by a server.
 * When a client receives a Proxy of an exported object, the server considers that the client is interested in this object.
 * The server remembers all the objects in which the client is interested in. 
 * And when the client dies or doesn't ping the server anymore, then the server notifies the listener 
 * that the client is not interested anymore.
 * 
 * */
public interface IClientInterestListener {

	/**
	 * Notifies the listener that the client represented by the remoteServerAddress is interested by the exported object 
	 * with the provided id
	 * 
	 * @param remoteServerAddress the address of the remote client
	 * @param id the object id in which the client is interested
	 * */
	public void addInterest(ServerAddress remoteServerAddress,String id);
	
	/**
	 * Notifies the listener that the client represented by the remoteServerAddress is not interested anymore by the
	 * exported object with the provided ids
	 * 
	 * @param remoteServerAddress the address of the remote client
	 * @param ids the objects id in which the client is not interested anymore
	 * */
	public void removeInterest(ServerAddress remoteServerAddress,String[] ids);
}
