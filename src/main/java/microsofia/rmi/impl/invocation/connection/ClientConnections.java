package microsofia.rmi.impl.invocation.connection;

import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;
import com.google.inject.name.Named;

import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.IInjectorProvider;

/**
 * Encapsulates all the connections/channels opened by the server when playing the role of a client,
 * meaning when calling other servers.
 * */
public class ClientConnections {
	//the local Guice Injector
	@Inject
	@Named("injector")
	private IInjectorProvider provider;
	/**
	 * For every remote server, a ClientConnection is created and cached.
	 * */
	private Map<ServerAddress,ClientConnection> connections;
	
	public ClientConnections(){
		connections=new Hashtable<>();
	}

	/**
	 * Get and creates a ClientConnection for the remote server address.
	 * */
	public ClientConnection getClientConnection(ServerAddress remoteServerAddress){
		ClientConnection con=connections.get(remoteServerAddress);
		if (con==null){
			synchronized(this){
				con=connections.get(remoteServerAddress);
				if (con==null){
					con=new ClientConnection(remoteServerAddress);
					provider.get().injectMembers(con);
					connections.put(remoteServerAddress, con);
				}
			}
		}
		return con;
	}
	
	/**
	 * Called to free ClientConnection when there is no more client channel opened to the remote server.
	 * */
	public void killClientConnection(ServerAddress adr){
		ClientConnection cc=connections.remove(adr);
		if (cc!=null){
			cc.stop();
		}
	}
	
	/**
	 * Frees all resources used by the object. It is called at server shutdown.
	 * */
	public void stop(){
		connections.values().forEach(it -> it.stop());
	}
}
