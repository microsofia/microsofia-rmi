package microsofia.rmi;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import microsofia.rmi.gc.ClientGC;
import microsofia.rmi.gc.IClientGC;
import microsofia.rmi.gc.IClientGCListener;
import microsofia.rmi.gc.IServerGC;
import microsofia.rmi.gc.ServerGC;
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
	private ServerConfiguration serverConfiguration;
	private ServerAddress serverAddress;
	private Registry registry;
	private ClassesMetada classesMetada;	
	private ClassLoader classLoader;
	private ClientInvoker clientInvoker;
	private ServerInvoker serverInvoker;
	private ServerGC serverGC;
	private ClientGC clientGC;
	private ExecutorService executorService;
	private ScheduledExecutorService scheduledExecutorService;
	private EventLoopGroup group;
	private ServerBootstrap server;
	private Channel serverChannel;

	public Server(){
		serverConfiguration=new ServerConfiguration();
		serverAddress=new ServerAddress("localhost",0);
		registry=new Registry(serverAddress);
		classesMetada=new ClassesMetada();
		classLoader=getClass().getClassLoader();
		serverInvoker=new ServerInvoker(this,registry, classesMetada);
		serverGC=new ServerGC(this, registry, serverConfiguration);
		clientGC=new ClientGC(this, new ClientGCListener(), serverConfiguration);
	}
	
	public ServerConfiguration getServerConfiguration(){
		return serverConfiguration;
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
	
	public ClientGC getClientGC(){
		return clientGC;
	}
	
	public ServerGC getServerGC(){
		return serverGC;
	}
	
	public ExecutorService getExecutorService(){
		return executorService;
	}
	
	public ScheduledExecutorService getScheduledExecutorService(){
		return scheduledExecutorService;
	}

	public void start() throws Throwable{
		registry.export(this, IServer.class);
		registry.export(registry, IRegistry.class);
		registry.export(serverGC, IServerGC.class);
		registry.export(clientGC,IClientGC.class);
	
		executorService=Executors.newCachedThreadPool();
		scheduledExecutorService=Executors.newScheduledThreadPool(0);
		
		group=new NioEventLoopGroup();
		clientInvoker=new ClientInvoker(this,classesMetada,registry,group,serverConfiguration.getClientConnectionsConfig());
		
		server=new ServerBootstrap();
		server.group(group)
			  .channel(NioServerSocketChannel.class)
			  .localAddress(serverAddress.getHost(), serverAddress.getPort())
			  .childHandler(new ChannelInitializer<Channel>() {
				  public void initChannel(Channel c) throws Exception{
					  c.pipeline().addLast(new ServerErrorHandler());
					  c.pipeline().addLast(new ObjectDecoder(Server.this,classLoader));
					  c.pipeline().addLast(new ObjectEncoder(Server.this,registry,null));
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
	
    public <T> T lookup(ServerAddress serverAddress, Class<T> interf){
		ClientInvocationHandler clientInvocationHandler=new ClientInvocationHandler(clientInvoker, new ObjectAddress(serverAddress, interf.getName(), new Class[]{interf}));
		return interf.cast(Proxy.newProxyInstance(classLoader, new Class[]{interf}, clientInvocationHandler));
    }
		
	public IServer getServer(String host,int port){
		ClientInvocationHandler clientInvocationHandler=new ClientInvocationHandler(clientInvoker, new ObjectAddress(new ServerAddress(host, port), IServer.class.getName(), new Class[]{IServer.class}));
		return (IServer)Proxy.newProxyInstance(classLoader, new Class[]{IServer.class}, clientInvocationHandler);
	}
	
	public void stop() throws Exception{
		serverChannel.disconnect().sync();
		group.shutdownGracefully().sync();
		clientInvoker.stop();
		executorService.shutdown();
		scheduledExecutorService.shutdown();
	}
	
	@Override
	public ServerAddress getServerAddress() {
		return serverAddress;
	}

	@Override
	public IRegistry getRegistry() {
		return registry;
	}
	
	private class ClientGCListener implements IClientGCListener{//TODO

		@Override
		public void objectAlive(ServerAddress serverAddress, String[] objects) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void objectMaybeDead(ServerAddress serverAddress, String[] objects, Throwable throwable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void objectDead(ServerAddress serverAddress, String[] objects, Throwable throwable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void objectDead(ServerAddress serverAddress, String[] objects) {
			// TODO Auto-generated method stub
			
		}
		
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
		
		IRegistry reg2=server11.getRegistry();
		System.out.println("reg1=="+reg1);
		System.out.println("reg2=="+reg2);
		System.out.println("reg1.equals(reg2)=="+reg1.equals(reg2));

		server1.stop();
		server2.stop();
	}
}
