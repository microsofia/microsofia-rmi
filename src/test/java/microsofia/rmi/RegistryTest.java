package microsofia.rmi;

import org.junit.Assert;
import org.junit.Test;

public class RegistryTest extends AbstractTest{

	public RegistryTest(){
	}


	@Test
	public void testContent(){
		IRegistry registry=remoteServer1.getRegistry();
		Assert.assertTrue("Registry has at least one object exported",registry.getIds().length>0);
		
		IRegistry registry1=(IRegistry)registry.getObject(IRegistry.class.getName());
		Assert.assertEquals(registry,registry1);
		
		IRegistry registry2=registry.getObject(IRegistry.class,IRegistry.class.getName());
		Assert.assertEquals(registry,registry2);
	}
}