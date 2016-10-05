package microsofia.rmi.impl;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import microsofia.rmi.IRegistry;
import microsofia.rmi.IServer;
import microsofia.rmi.ObjectAddress;
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.gc.ClientGC;
import microsofia.rmi.impl.gc.IClientGC;
import microsofia.rmi.impl.gc.IServerGC;
import microsofia.rmi.impl.gc.ServerGC;
import microsofia.rmi.impl.handler.ServerErrorHandler;
import microsofia.rmi.impl.handler.codec.ServerDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.impl.invocation.ClientInvocationHandler;
import microsofia.rmi.impl.invocation.IClientInvoker;

/**
 * Server implementation.
 * */
public class ServerImpl extends Server implements IServerImpl{
	private static Log log = LogFactory.getLog(ServerImpl.class);
	//the registry implementation of the server
	@Inject
	private IRegistryImpl registry;
	//the classloader used by the server while unmarshalling objects
	@Inject
	private ClassLoader classLoader;
	//executorservice of the server
	@Inject
	private ExecutorService executorService;
	//ScheduledExecutorService of the server
	@Inject
	private ScheduledExecutorService scheduledExecutorService;
	//EventLoopGroup of the server shared by the clients channel
	@Inject
	private EventLoopGroup group;
	//Client invoker used to encapsulate client side calls
	@Inject
	private IClientInvoker clientInvoker;
	//ServerGC which checks that the clients are calling and are showing their interests in exported objects
	@Inject
	private ServerGC serverGC;
	//ClientGC which pings other servers to show interests in exported objects
	@Inject
	private ClientGC clientGC;
	//Injector provider
	@Inject
	@Named("injector")
	private IInjectorProvider provider;
	private ServerBootstrap server;
	private Channel serverChannel;

	public ServerImpl(){
	}
	
	@Override
	public IRegistry getRegistry() {
		return registry;
	}

	@Override
	public void start() throws Throwable{
		//by default, exporting native interfaces
		registry.export(this, IServer.class);
		registry.export(registry, IRegistry.class);
		registry.export(serverGC, IServerGC.class);
		registry.export(clientGC,IClientGC.class);
	
		server=new ServerBootstrap();
		server.group(group)
			  .channel(NioServerSocketChannel.class)
			  .localAddress(serverAddress.getHost(), serverAddress.getPort())
			  .childHandler(new ChannelInitializer<Channel>() {
				  public void initChannel(Channel c) throws Exception{
					  Injector injector=provider.get();

					  c.pipeline().addLast(injector.getInstance(ServerErrorHandler.class));
					  c.pipeline().addLast(injector.getInstance(ObjectDecoder.class));
					  
					  ObjectEncoder objectEncoder=new ObjectEncoder(null);
					  injector.injectMembers(objectEncoder);
					  c.pipeline().addLast(objectEncoder);

					  c.pipeline().addLast(injector.getInstance(ServerDecoder.class));
				  }
			});
		ChannelFuture future=server.bind().sync();
		if (!future.isSuccess()){
			throw future.cause();
		}
		serverChannel=future.channel();
		
		//setting the port again in case the port is 0 and an anonymous one is used
		serverAddress.setPort(((InetSocketAddress)server.config().localAddress()).getPort());
		
		localServers.put(serverAddress.getPort(), this);
	}

	@Override
	public void export(String id,Object o,Class<?>[] interfaces){
		registry.export(id,o,interfaces);
	}
	
	@Override
	public void export(Object o,Class<?> interf){
		registry.export(o,interf);
	}
	
	@Override
	public void unexport(Object o){
		registry.unexport(o);
	}
	
	public ObjectAddress getObjectAddress(Object o){
		return registry.getObjectAddress(o);
	}
	
	@Override
    public <T> T lookup(ServerAddress serverAddress, Class<T> interf){
		ClientInvocationHandler clientInvocationHandler=new ClientInvocationHandler(clientInvoker, new ObjectAddress(serverAddress, interf.getName(), new Class[]{interf}));
		return interf.cast(Proxy.newProxyInstance(classLoader, new Class[]{interf}, clientInvocationHandler));
    }
	
	@Override
	public IServer getServer(String host,int port){
		ClientInvocationHandler clientInvocationHandler=new ClientInvocationHandler(clientInvoker, new ObjectAddress(new ServerAddress(host, port), IServer.class.getName(), new Class[]{IServer.class}));
		return (IServer)Proxy.newProxyInstance(classLoader, new Class[]{IServer.class}, clientInvocationHandler);
	}
	
	@Override
	public void stop() throws Throwable{
		localServers.remove(serverAddress.getPort());
		clientGC.stop();
		serverGC.stop();
		if (serverChannel.isActive()){
			try{
				serverChannel.disconnect().sync();
			}catch(Exception e){
				log.debug(e,e);
			}
		}
		group.shutdownGracefully().sync();
		clientInvoker.stop();
		executorService.shutdown();
		scheduledExecutorService.shutdown();

	}
}
