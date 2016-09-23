package microsofia.rmi.invocation.connection;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.netty.channel.EventLoopGroup;
import microsofia.rmi.Registry;
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;

public class ClientConnections {
	private Server server;
	private Registry registry;
	private EventLoopGroup group;
	private GenericObjectPoolConfig config;
	private Map<ServerAddress,ClientConnection> connections;
	
	public ClientConnections(Server server,Registry registry,EventLoopGroup group,GenericObjectPoolConfig config){
		this.server=server;
		this.registry=registry;
		this.group=group;
		this.config=config;
		connections=new Hashtable<>();
	}

	public ClientConnection getClientConnection(ServerAddress remoteServerAddress){
		ClientConnection con=connections.get(remoteServerAddress);
		if (con==null){
			synchronized(this){
				con=connections.get(remoteServerAddress);
				if (con==null){
					con=new ClientConnection(this,server,registry,remoteServerAddress,group,config);
					connections.put(remoteServerAddress, con);
				}
			}
		}
		return con;
	}
	
	//TODO implement remote GC so that we can free client connections
	public void killClientConnection(ServerAddress adr){
		ClientConnection cc=connections.remove(adr);
		cc.stop();
	}
	
	public void stop(){
		connections.values().forEach(it -> it.stop());
	}
}
