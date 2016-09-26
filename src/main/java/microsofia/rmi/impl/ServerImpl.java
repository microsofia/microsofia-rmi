package microsofia.rmi.impl;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

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
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerBuilder;
import microsofia.rmi.impl.gc.ClientGC;
import microsofia.rmi.impl.gc.IClientGC;
import microsofia.rmi.impl.gc.IServerGC;
import microsofia.rmi.impl.gc.ServerGC;
import microsofia.rmi.impl.handler.ServerErrorHandler;
import microsofia.rmi.impl.handler.codec.serialization.ObjectDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.impl.invocation.ClientInvocationHandler;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.ObjectAddress;
import microsofia.rmi.implhandler.codec.ServerDecoder;

/**
 * Server implementation.
 * */
public class ServerImpl extends Server implements IServerImpl{
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
	//ServerGC which checks that the client servers calls and showed their interests in exported objects
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
		serverChannel.disconnect().sync();
		group.shutdownGracefully().sync();
		clientInvoker.stop();
		executorService.shutdown();
		scheduledExecutorService.shutdown();
	}
	
	//TODO remove the static main and use unit tests
	public static void main(String[] argv) throws Throwable{
		Server server1=new ServerBuilder().host("localhost").port(9999).build();
		server1.start();
		
		Server server2=new ServerBuilder().host("localhost").port(9998).build();
		server2.start();
		
		IServer server11=server2.getServer("localhost",9999);
		System.out.println(server11);

		IRegistry reg1=server11.getRegistry();
		System.out.println(reg1);
		List<Thread> ths=new ArrayList<>();
		for (int j=0;j<100;j++){
			ths.add(new Thread(){
				public void run(){
					for (int i=0;i<100;i++){
						for (String id :reg1.getIds()){
							System.out.println("ID="+id);
						}
					}
				}
			});
			ths.get(ths.size()-1).start();
		}
		ths.forEach(it->{try {
				it.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		IRegistry reg2=server11.getRegistry();
		System.out.println("reg1=="+reg1);
		System.out.println("reg2=="+reg2);
		System.out.println("reg1.equals(reg2)=="+reg1.equals(reg2));

		server1.stop();
		server2.stop();
	}
}