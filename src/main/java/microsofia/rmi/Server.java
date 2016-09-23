package microsofia.rmi;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import microsofia.rmi.handler.ServerErrorHandler;
import microsofia.rmi.handler.codec.ServerDecoder;
import microsofia.rmi.handler.codec.serialization.ObjectDecoder;
import microsofia.rmi.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.invocation.ClassesMetada;
import microsofia.rmi.invocation.ClientInvocationHandler;
import microsofia.rmi.invocation.ClientInvoker;
import microsofia.rmi.invocation.ObjectAddress;
import microsofia.rmi.invocation.ServerInvoker;

public class Server implements IServer{
	private ServerAddress serverAddress;
	private GenericObjectPoolConfig clientConnectionsConfig;
	private Registry registry;
	private ClassesMetada classesMetada;	
	private ClassLoader classLoader;
	private ClientInvoker clientInvoker;
	private ServerInvoker serverInvoker;
	private EventLoopGroup group;
	private ServerBootstrap server;
	private Channel serverChannel;

	public Server(){
		serverAddress=new ServerAddress("localhost",0);
		clientConnectionsConfig=new GenericObjectPoolConfig();
		registry=new Registry(serverAddress);
		classesMetada=new ClassesMetada();
		classLoader=getClass().getClassLoader();
		serverInvoker=new ServerInvoker(registry, classesMetada);		
	}
	
	public GenericObjectPoolConfig getClientConnectionsConfig(){
		return clientConnectionsConfig;
	}
	
	public void setHost(String host){
		serverAddress.setHost(host);
	}
	
	public int getPort(){
		return serverAddress.getPort();
	}

	public void setPort(int port){
		serverAddress.setPort(port);
	}
	
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public ClientInvoker getClientInvoker(){
		return clientInvoker;
	}

	public void start() throws Throwable{
		registry.export(this, IServer.class);
		registry.export(registry, IRegistry.class);
		
		group=new NioEventLoopGroup();
		clientInvoker=new ClientInvoker(this,classesMetada,registry,group,clientConnectionsConfig);
		
		server=new ServerBootstrap();
		server.group(group)
			  .channel(NioServerSocketChannel.class)
			  .localAddress(serverAddress.getHost(), serverAddress.getPort())
			  .childHandler(new ChannelInitializer<Channel>() {
				  public void initChannel(Channel c) throws Exception{
					  c.pipeline().addLast(new ServerErrorHandler());
					  c.pipeline().addLast(new ObjectDecoder(Server.this,classLoader));
					  c.pipeline().addLast(new ObjectEncoder(registry));
					  c.pipeline().addLast(new ServerDecoder(serverInvoker));
				  }
			});
		ChannelFuture future=server.bind().sync();
		if (!future.isSuccess()){
			throw future.cause();
		}
		serverChannel=future.channel();
		serverAddress.setPort(((InetSocketAddress)server.config().localAddress()).getPort());
	}
		
	public void export(String id,Object o,Class<?>[] interfaces){
		registry.export(id,o,interfaces);
	}
	
	public void export(Object o,Class<?> interf){
		registry.export(o,interf);
	}
		
	public IServer getServer(String host,int port){
		ClientInvocationHandler clientInvocationHandler=new ClientInvocationHandler(clientInvoker, new ObjectAddress(new ServerAddress(host, port), IServer.class.getName(), new Class[]{IServer.class}));
		return (IServer)Proxy.newProxyInstance(classLoader, new Class[]{IServer.class}, clientInvocationHandler);
	}
	
	public void stop() throws Exception{
		serverChannel.disconnect().sync();
		group.shutdownGracefully().sync();
		serverInvoker.stop();
		clientInvoker.stop();
	}
	
	@Override
	public ServerAddress getServerAddress() {
		return serverAddress;
	}

	@Override
	public IRegistry getRegistry() {
		return registry;
	}
	
	public static void main(String[] argv) throws Throwable{
		Server server1=new Server();
		server1.setHost("localhost");
		server1.setPort(9999);
		server1.start();
		
		Server server2=new Server();
		server2.setHost("localhost");
		server2.setPort(9998);
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

		server1.stop();
		server2.stop();
	}
}
