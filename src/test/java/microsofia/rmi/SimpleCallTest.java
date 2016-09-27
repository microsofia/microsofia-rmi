package microsofia.rmi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SimpleCallTest extends AbstractTest{
	private Sample sample=new Sample();

	public SimpleCallTest(){
	}

	@Override
	public void setup() throws Throwable{
		super.setup();
		localServer1.export(sample, ISample.class);
	}
	
	@Test
	public void testNumericCalls(){
		ISample remoteSample=localServer2.lookup(localServer1.getServerAddress(), ISample.class);

		Assert.assertEquals((long)5, remoteSample.getLong(5));
		Assert.assertEquals("5", remoteSample.getString("5"));
	}
	
	@Test
	public void testSerializableCalls(){
		ISample remoteSample=localServer2.lookup(localServer1.getServerAddress(), ISample.class);
		SerializableSample ss=new SerializableSample(1978,1982);
		Assert.assertEquals(ss, remoteSample.getSerializableSample(ss));
		
		List<SerializableSample> lss=Arrays.asList(ss);
		List<SerializableSample> lr=remoteSample.getSerializableSample(lss);
		Assert.assertEquals(1, lr.size());
		Assert.assertEquals(ss, lr.get(0));
		
		SerializableSample[] ass=new SerializableSample[]{ss};
		SerializableSample[] ar=remoteSample.getSerializableSample(ass);
		Assert.assertEquals(1, ass.length);
		Assert.assertEquals(ss, ar[0]);
	}
	
	@Test
	public void testProxyEquality(){
		ISample remoteSample1=localServer2.lookup(localServer1.getServerAddress(), ISample.class);
		ISample remoteSample2=localServer2.lookup(localServer1.getServerAddress(), ISample.class);
		Assert.assertEquals(remoteSample1, remoteSample2);
	}
	
	@Test
	public void testThrowingException(){
		ISample remoteSample1=localServer2.lookup(localServer1.getServerAddress(), ISample.class);
		Exception e=null;
		try{
			remoteSample1.throwException();
		}catch(Exception e2){
			e=e2;
		}
		Assert.assertNotNull(e);
	}
	
	@Override
	public void tearDown() throws Throwable{
		localServer1.unexport(sample);
		super.tearDown();
	}
	
	public static class SerializableSample implements Serializable{
		private static final long serialVersionUID = 0L;
		public float f;
		public int i;

		public SerializableSample(){
		}	

		public SerializableSample(float f,int i){
			this.f=f;
			this.i=i;
		}	
		
		@Override
		public boolean equals(Object o){
			SerializableSample ss=(SerializableSample)o;
			return f==ss.f && i==ss.i;
		}
	}
	
	public interface ISample{
		
		public long getLong(long l);
		
		public String getString(String s);

		public SerializableSample getSerializableSample(SerializableSample s);
		
		public List<SerializableSample> getSerializableSample(List<SerializableSample> s);
		
		public SerializableSample[] getSerializableSample(SerializableSample[] s);
		
		public void throwException() throws Exception;
	}

	public static class Sample implements ISample{

		public Sample(){
		}

		@Override
		public long getLong(long l) {
			return l;
		}

		@Override
		public String getString(String s) {
			return s;
		}

		@Override
		public SerializableSample getSerializableSample(SerializableSample s) {
			return s;
		}

		@Override
		public List<SerializableSample> getSerializableSample(List<SerializableSample> s) {
			return s;
		}

		@Override
		public SerializableSample[] getSerializableSample(SerializableSample[] s) {
			return s;
		}

		@Override
		public void throwException() throws Exception {
			throw new IllegalStateException("Sample test");
		}
	}
}
