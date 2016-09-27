package microsofia.rmi;

import org.junit.Assert;
import org.junit.Test;

public class ProxyTest extends AbstractTest{
	private Sample sample1=new Sample();
	private Sample sample2=new Sample();
	
	public ProxyTest(){
	}

	@Override
	public void setup() throws Throwable{
		super.setup();
		localServer1.export(sample1, ISample.class);
		localServer2.export(sample2, ISample.class);
	}
	
	@Test
	public void testProxyReplacement(){
		ISample remoteSample1=remoteServer1.getRegistry().getObject(ISample.class,ISample.class.getName());
		ISample remoteSample2=remoteServer2.getRegistry().getObject(ISample.class,ISample.class.getName());
		ISample sample2Returned=remoteSample1.setSample(sample2);
		
		Assert.assertEquals(sample2, sample2Returned);
		Assert.assertEquals(remoteSample2, sample1.s);
	}
	
	@Override
	public void tearDown() throws Throwable{
		localServer1.unexport(sample1);
		localServer2.unexport(sample2);
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
