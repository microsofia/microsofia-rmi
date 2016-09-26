package microsofia.rmi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import microsofia.rmi.impl.IInjectorProvider;
import microsofia.rmi.impl.IRegistryImpl;
import microsofia.rmi.impl.IServerImpl;
import microsofia.rmi.impl.Registry;
import microsofia.rmi.impl.ServerImpl;
import microsofia.rmi.impl.gc.ClientGC;
import microsofia.rmi.impl.gc.ServerGC;
import microsofia.rmi.impl.invocation.ClassesMetada;
import microsofia.rmi.impl.invocation.ClientInvoker;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.IServerInvoker;
import microsofia.rmi.impl.invocation.ServerInvoker;
import microsofia.rmi.impl.invocation.connection.ClientConnections;

/**
 * The only way to build a server is to use this builder.
 * 
 * <br>
 * Example:
 * <bre>
 *<pre>
	ServerBuilder builder=new ServerBuild().host("localhost").port(9999);
	Server server=builder.build();
	server.start();

</pre>
 * 
 * 
 * */
public class ServerBuilder {
	private ServerConfiguration serverConfiguration;
	private ServerAddress serverAddress;
	private ClassLoader cl;
	private ExecutorService es;
	private ScheduledExecutorService ses;
	private IClientInterestListener clientInterestListener;
	private Injector injector;
	
	public ServerBuilder(){
		serverConfiguration=new ServerConfiguration();
		serverAddress=new ServerAddress("localhost",0);
		cl=ServerBuilder.class.getClassLoader();
	}

	/**
	 * Setting the server configuration. If not called, the server will use the default values.
	 * */
	public ServerBuilder configuration(ServerConfiguration serverConfiguration){
		this.serverConfiguration=serverConfiguration;
		return this;
	}
	
	/**
	 * Setting the host of the server. Default value is localhost
	 * */
	public ServerBuilder host(String h){
		serverAddress.setHost(h);
		return this;
	}

	/**
	 * Setting the port of the server. If not provided, an anonymous port will be used.
	 * */
	public ServerBuilder port(int p){
		serverAddress.setPort(p);
		return this;
	}

	/**
	 * Setting the classloader to used.
	 * */
	public ServerBuilder classLoader(ClassLoader cl){
		this.cl=cl;
		return this;
	}

	/**
	 * Setting the ExecutorServer to use.
	 * */
	public ServerBuilder executorService(ExecutorService se){
		this.es=se;
		return this;
	}
	
	/**
	 * Setting the ScheduledExecutorServer to use.
	 * */
	public ServerBuilder scheduledExecutorService(ScheduledExecutorService ses){
		this.ses=ses;
		return this;
	}
	
	/**
	 * Setting the client interest listener to use.
	 * */
	public ServerBuilder interestListener(IClientInterestListener l){
		clientInterestListener=l;
		return this;
	}

	/**
	 * Creates a new Server instance.
	 * */
	public Server build(){
		if (es==null){
			es=Executors.newCachedThreadPool();
		}
		if (ses==null){
			ses=Executors.newScheduledThreadPool(0);
		}
		final IInjectorProvider provider=new IInjectorProvider() {

			@Override
			public Injector get() {
				return injector;
			}
			
		};
		final IClientInterestListener listener=new IClientInterestListener(){

			@Override
			public void addInterest(ServerAddress remoteServerAddress, String id) {
				if (clientInterestListener!=null){
					clientInterestListener.addInterest(remoteServerAddress, id);
				}
			}

			@Override
			public void removeInterest(ServerAddress remoteServerAddress, String[] ids) {
				if (clientInterestListener!=null){
					clientInterestListener.removeInterest(remoteServerAddress, ids);
				}
			}
			
		};
		
		AbstractModule module=new AbstractModule() {			
			@Override
			protected void configure() {
				bind(ServerConfiguration.class).toInstance(serverConfiguration);
				bind(ServerAddress.class).toInstance(serverAddress);
				bind(ClassLoader.class).toInstance(cl);
				bind(ExecutorService.class).toInstance(es);
				bind(ScheduledExecutorService.class).toInstance(ses);
				bind(IClientInterestListener.class).toInstance(listener);
				
				bind(Key.get(IInjectorProvider.class, Names.named("injector"))).toInstance(provider);
				
				bind(EventLoopGroup.class).to(NioEventLoopGroup.class).asEagerSingleton();
				bind(ClassesMetada.class).toInstance(new ClassesMetada());
				bind(IClientInvoker.class).to(ClientInvoker.class).asEagerSingleton();
				bind(ClientConnections.class).toInstance(new ClientConnections());
				bind(IServerInvoker.class).to(ServerInvoker.class).asEagerSingleton();
				bind(ClientGC.class).toInstance(new ClientGC());
				bind(ServerGC.class).toInstance(new ServerGC());
				bind(IRegistryImpl.class).to(Registry.class).asEagerSingleton();
				bind(IServerImpl.class).to(ServerImpl.class).asEagerSingleton();
				
			}
		};
		injector=Guice.createInjector(module);
		return (Server)injector.getInstance(IServerImpl.class);
	}
}