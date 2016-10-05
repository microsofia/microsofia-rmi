package microsofia.rmi;

import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;

/**
 * A server which provides remote method invocation (RMI) in a simple way.
 * */
public abstract class Server implements IServer{
	@Inject
	protected ServerConfiguration serverConfiguration;
	@Inject 
	protected ServerAddress serverAddress;

	protected Server(){
	}

	/**
	 * Returns the configuration of the running server.
	 * 
	 * @return the server configuration
	 * */
	public ServerConfiguration getServerConfiguration(){
		return serverConfiguration;
	}

	/**
	 * Returns the address of the running server.
	 * 
	 * @return the address of the server
	 * */
	@Override
	public ServerAddress getServerAddress(){
		return serverAddress;
	}

	/**
	 * Once the server is created it is not running till the start method is called.
	 * 
	 * */
	public abstract void start() throws Throwable;

	/**
	 * Exports the provided object o with the following interfaces with the provided id
	 * 
	 * @param id the id used to export the object
	 * @param o the object to export
	 * @param interfaces the interfaces that will be used by the proxy
	 * */
	public abstract void export(String id,Object o,Class<?>[] interfaces);

	/**
	 * The following method is a shortcut to the previous method.
	 * In this case, there is only one interface to the exported object and the id will be the interface name.
	 * 
	 * @param o the object to export
	 * @param interf the interface that will be used for the id and the proxy
	 * */
	public abstract void export(Object o,Class<?> interf);
	
	/**
	 * Unexport the already exported object.
	 * 
	 * @param o the object to unexport
	 * */
	public abstract void unexport(Object o);
	
	/**
	 * Returns the object address of an exported object
	 * */
	public abstract ObjectAddress getObjectAddress(Object o);
	
	/**
	 * Returns a proxy to a remote object in a given server provided by its address having the 
	 * interf as interface. The id of the located object will be the interface name.
	 * The returned proxy is never null even if the remote server is down or the remote object is not exported.
	 * 
	 * @param serverAddress the address of the remote server which contains the object that we are trying to locate
	 * @param interf the interface of the remote object
	 * @return a proxy of the remote object
	 * */
    public abstract <T> T lookup(ServerAddress serverAddress, Class<T> interf);

    /**
     * Returns a proxy to a remote server. The returned proxy is never null even if the remote server is down.
	 * 
	 * @param host the host of the remote server
	 * @param port the port of the remote server
	 * @return a proxy of the remote server
     * */
	public abstract IServer getServer(String host,int port);
	
	/**
	 * Stops the server and frees all used resources (threads, sockets, ...)
	 * */
	public abstract void stop() throws Throwable;

	//list of all local servers created within the JVM
	protected static Map<Integer,Server> localServers = new Hashtable<>();
	
	/**
	 * Returns the server listening on the port within the JVM.
	 * */
	public static Server getServer(int port){
		return localServers.get(port);
	}
}
