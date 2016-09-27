package microsofia.rmi;

import org.junit.After;
import org.junit.Before;

/**
 * Base class for all tests.
 * */
public class AbstractTest {
	protected ServerConfiguration config;
	protected IClientInterestListener listener;
	protected Server localServer1;
	protected Server localServer2;
	protected IServer remoteServer1;
	protected IServer remoteServer2;
	
	protected AbstractTest(){
	}

	/**
	 * Sets up 2 servers, server1 and server2, and locate for each its remote proxy.
	 * */
	@Before
	public void setup() throws Throwable{
		ServerBuilder serverBuilder1=new ServerBuilder();
		if (config!=null){
			serverBuilder1.configuration(config);
		}
		if (listener!=null){
			serverBuilder1.interestListener(listener);
		}
		localServer1=serverBuilder1.port(9999).build();
		localServer1.start();
		
		ServerBuilder serverBuilder2=new ServerBuilder();
		if (config!=null){
			serverBuilder2.configuration(config);
		}
		if (listener!=null){
			serverBuilder2.interestListener(listener);
		}
		localServer2=serverBuilder2.port(9998).build();			
		localServer2.start();		
		
		remoteServer1=localServer2.getServer("localhost", 9999);
		remoteServer2=localServer1.getServer("localhost", 9998);
	}
	

	@After
	public void tearDown() throws Throwable{
		localServer1.stop();
		localServer2.stop();
	}
}
