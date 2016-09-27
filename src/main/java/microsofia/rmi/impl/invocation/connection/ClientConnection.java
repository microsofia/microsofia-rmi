package microsofia.rmi.impl.invocation.connection;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerConfiguration;
import microsofia.rmi.impl.IInjectorProvider;
import microsofia.rmi.impl.handler.ClientErrorHandler;
import microsofia.rmi.impl.handler.codec.ClientDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.impl.invocation.InvocationRequest;

/**
 * Encapsulates all the connections/channels opened by the current server to a remote server, while doing
 * remote calls. A pool of channel is created in order to reuse as much as possible the already opened channels. 
 * */
public class ClientConnection implements PooledObjectFactory<Channel>{
	private static Log log=LogFactory.getLog(ClientConnection.class);
	//the ClientConnections containing the current ClientConnection
	@Inject
	private ClientConnections clientConnections;
	//the local server address
	@Inject
	private ServerAddress serverAddress;
	//the remote server address
	private ServerAddress remoteServerAddress;
	//the EventLoopGroup used for all Netty bootstraping
	@Inject
	private EventLoopGroup group;
	//the local server configuration
	@Inject
	private ServerConfiguration config;
	//the local Guice Injector
	@Inject
	@Named("injector")
	private IInjectorProvider provider;
	/**
	 * A pool of channel
	 * */
	private ObjectPool<Channel> pool;

	public ClientConnection(ServerAddress remoteServerAddress){
		this.remoteServerAddress=remoteServerAddress;
	}

	//lazy pool initialization
	private synchronized ObjectPool<Channel> getPool(){
		if (pool==null){
			pool=new GenericObjectPool<>(this, config.getClientConnectionsConfig());	
		}
		return pool;
	}
	
	/**
	 * The remote server address
	 * */
	public ServerAddress getRemoteServerAddress(){
		return remoteServerAddress;
	}
	
	/**
	 * Takes a channel from the pool
	 * */
	public Channel takeChannel() throws Exception{
		return getPool().borrowObject();
	}
	
	/**
	 * Invalidates a Channel taken from the pool
	 * */
	public void invalidateChannel(Channel channel){
		try{
			getPool().invalidateObject(channel);
		}catch(Exception e){
			log.error(e,e);
		}
	}

	/**
	 * Returns back a channel in the pool
	 * */
	public void returnChannel(Channel channel) throws Exception{
		getPool().returnObject(channel);
	}

	/**
	 * Frees all resources used by this object (the pool).
	 * It is used at server shutdown,
	 * */
	public void stop(){
		getPool().close();
	}
	
	/**
	 * Called when a Channel is closed.
	 * */
    public void channelClosed(Channel channel, Throwable cause){
    	synchronized(clientConnections){
    		if (pool.getNumActive()==0){//there is no more borrowed channel, remove the ClientConnection from its parent
    			clientConnections.killClientConnection(remoteServerAddress);
    		}
    	}
    }

	/**
	 * Implementation of the pool.
	 * Creates a new channel to the remote server.
	 * */
	@Override
	public PooledObject<Channel> makeObject() throws Exception {
		Bootstrap client=new Bootstrap();
		client.group(group)
			  .channel(NioSocketChannel.class)
			  .remoteAddress(remoteServerAddress.getHost(), remoteServerAddress.getPort())
			  .handler(new ChannelInitializer<Channel>() {
				  public void initChannel(Channel c) throws Exception{
					  Injector injector=provider.get();
					  
					  c.pipeline().addLast(injector.getInstance(ObjectDecoder.class));
					  
					  ObjectEncoder oe=new ObjectEncoder(remoteServerAddress);
					  injector.injectMembers(oe);
					  c.pipeline().addLast(oe);
					  
					  c.pipeline().addLast(injector.getInstance(ClientDecoder.class));
					  
					  ClientErrorHandler clientErrorHandler=new ClientErrorHandler(ClientConnection.this);
					  injector.injectMembers(clientErrorHandler);
					  c.pipeline().addLast(clientErrorHandler);
				  }
			});
			
		ChannelFuture future=client.connect().sync();
		
		//before using the channel, we first send a low level message in order to set the correct client server address
		future.channel().writeAndFlush(InvocationRequest.createSetServerAddressRequest(serverAddress)).sync();
		return new DefaultPooledObject<Channel>(future.channel());
	}

	/**
	 * Implementation of the pool.
	 * Closes the channel if it is still active.
	 * */
	@Override
	public void destroyObject(PooledObject<Channel> p) throws Exception {
		if (p.getObject().isActive()){
			p.getObject().close().sync();
		}
	}

	/**
	 * Implementation of the pool. Returns true if the channel is still active.
	 * */
	@Override
	public boolean validateObject(PooledObject<Channel> p) {
		return p.getObject().isActive();
	}

	/**
	 * Implementation of the pool
	 * */
	@Override
	public void activateObject(PooledObject<Channel> p) throws Exception {
	}

	/**
	 * Implementation of the pool
	 * */
	@Override
	public void passivateObject(PooledObject<Channel> p) throws Exception {
	}
}
