package microsofia.rmi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing GC client and server side, with an interest listener.
 * */
public class GCTest extends AbstractTest{
	private Sample sample1=new Sample();
	private static Set<String> addedIds=new HashSet<>();
	private static Set<String> removedIds=new HashSet<>();
	
	public GCTest(){
		config=new ServerConfiguration();
		config.setClientGCPeriod(2000);
		config.setServerGCTimeout(8000);
		listener=new IClientInterestListener(){

			@Override
			public void addInterest(ServerAddress remoteServerAddress, String id) {
				addedIds.add(id);
			}

			@Override
			public void removeInterest(ServerAddress remoteServerAddress, String[] ids) {
				removedIds.addAll(Arrays.asList(ids));
			}
		};
	}


	@Override
	public void setup() throws Throwable{
		super.setup();
		localServer1.export(sample1, ISample.class);
	}
	
	@Test
	public void testGC() throws Throwable{
		//show an interest
		ISample remoteSample1=remoteServer1.getRegistry().getObject(ISample.class,ISample.class.getName());
		Thread.sleep(3000);
		//wait for the listener to be notified
		Assert.assertTrue(addedIds.contains(ISample.class.getName()));
		
		//stop the client server
		localServer2.stop();
		Thread.sleep(15000);
		//the server should detect it via GC and fire the interest listener
		Assert.assertTrue(removedIds.contains(ISample.class.getName()));		
	}
	
	@Override
	public void tearDown() throws Throwable{
		super.tearDown();
	}
	
	
	public interface ISample{
		
		public ISample setSample(ISample s);		
	}

	public static class Sample implements ISample{
		private ISample s;

		public Sample(){
		}

		@Override
		public ISample setSample(ISample s){
			this.s=s;
			return s;
		}
	}
}
