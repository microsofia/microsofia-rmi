package microsofia.rmi.invocation.connection;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import microsofia.rmi.Registry;
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.handler.ClientErrorHandler;
import microsofia.rmi.handler.codec.ClientDecoder;
import microsofia.rmi.handler.codec.serialization.ObjectDecoder;
import microsofia.rmi.handler.codec.serialization.ObjectEncoder;

public class ClientConnection implements PooledObjectFactory<Channel>{
	private ClientConnections clientConnections;
	private Server server;
	private Registry registry;
	private ServerAddress remoteServerAddress;
	private EventLoopGroup group;
	private ObjectPool<Channel> pool;

	public ClientConnection(ClientConnections clientConnections,Server server,Registry registry,ServerAddress remoteServerAddress,EventLoopGroup group,GenericObjectPoolConfig config){
		this.clientConnections=clientConnections;
		this.server=server;
		this.registry=registry;
		this.remoteServerAddress=remoteServerAddress;
		this.group=group;
		pool=new GenericObjectPool<>(this, config);
	}
	
	public ServerAddress getRemoteServerAddress(){
		return remoteServerAddress;
	}
	
	public Channel takeChannel() throws Exception{
		return pool.borrowObject();
	}
	
	public void returnChannel(Channel channel) throws Exception{
		pool.returnObject(channel);
	}

	public void stop(){
		pool.close();
	}

	@Override
	public PooledObject<Channel> makeObject() throws Exception {
		Bootstrap client=new Bootstrap();
		client.group(group)
			  .channel(NioSocketChannel.class)
			  .remoteAddress(remoteServerAddress.getHost(), remoteServerAddress.getPort())
			  .handler(new ChannelInitializer<Channel>() {
				  public void initChannel(Channel c) throws Exception{
					  c.pipeline().addLast(new ObjectDecoder(server,server.getClassLoader()));
					  c.pipeline().addLast(new ObjectEncoder(registry));
					  c.pipeline().addLast(new ClientDecoder(server.getClientInvoker()));
					  c.pipeline().addLast(new ClientErrorHandler(server.getClientInvoker()));
				  }
			});
			
		ChannelFuture future=client.connect().sync();
		return new DefaultPooledObject<Channel>(future.channel());
	}

	@Override
	public void destroyObject(PooledObject<Channel> p) throws Exception {
		if (p.getObject().isActive()){
			p.getObject().close().sync();
		}
	}

	@Override
	public boolean validateObject(PooledObject<Channel> p) {
		return p.getObject().isActive();
	}

	@Override
	public void activateObject(PooledObject<Channel> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Channel> p) throws Exception {
	}
}
