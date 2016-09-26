package microsofia.rmi.impl;

import microsofia.rmi.IServer;
import microsofia.rmi.ServerAddress;

/**
 * Internal interface of the server.
 * */
public interface IServerImpl extends IServer{

	/**
	 * Check the Server documentation.
	 * */
	public void export(String id,Object o,Class<?>[] interfaces);

	/**
	 * Check the Server documentation.
	 * */
	public void export(Object o,Class<?> interf);
	
	/**
	 * Check the Server documentation.
	 * */
    public <T> T lookup(ServerAddress serverAddress, Class<T> interf);
		
	/**
	 * Check the Server documentation.
	 * */
	public IServer getServer(String host,int port);
}
