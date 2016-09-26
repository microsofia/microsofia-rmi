package microsofia.rmi.impl.gc;

import microsofia.rmi.ServerAddress;

/**
 * Remote interface of ServerGC that manages for every client server the objects in which its interested.
 * */
public interface IServerGC {

	/**
	 * Indicate the ServerGC that the server adr is interested in the following object ids.
	 * Returns an array of boolean, true if the object is still exported false if not.
	 * 
	 * @param adr the client that shows the interest
	 * @param objectId the objects in which the client is interested
	 * @return an array indicating if every object is still exported
	 * */
	public boolean[] ping(ServerAddress adr, String[] objectId) throws Throwable;

	/**
	 * Indicates that a client is dead. This is used as a quick way to tell the server GC that a client 
	 * is not valid anymore, faster than waiting the pinging validation.
	 * */
    public void clientDead(ServerAddress adr) throws Throwable;

    /**
     * Returns all the client addresses that the server GC has currently.
     * */
    public ServerAddress[] getClientAddress() throws Throwable;
}
