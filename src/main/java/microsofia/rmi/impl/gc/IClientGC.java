package microsofia.rmi.impl.gc;

import microsofia.rmi.ServerAddress;

/**
 * Remote interface of Client GC which aim is to ping the remote server and indicate its interested objects.
 * */
public interface IClientGC {
	
	/**
	 * Remote server addresses that client GC is currently pinging
	 * */
	public ServerAddress[] getServerAddress() throws Exception;
}
