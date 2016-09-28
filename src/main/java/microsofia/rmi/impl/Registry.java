package microsofia.rmi.impl;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.inject.Inject;

import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.invocation.ClientInvocationHandler;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.ObjectAddress;

/**
 * Implementation of the registry.
 * */
public class Registry implements IRegistryImpl{
	//Map containing all Objects by their ids
	private Map<String,ObjectInfo> objectInfoByIds;
	//Map containing for each object its id
	private Map<Object,String> objects;
	//local server address
	@Inject
	private ServerAddress serverAddress;
	//local classloader used to create proxy
	@Inject
	private ClassLoader classLoader;
	//local client invoker used to create proxy
	@Inject
	private IClientInvoker clientInvoker;

	public Registry(){
		objectInfoByIds=new Hashtable<>();
		objects=Collections.synchronizedMap(new IdentityHashMap<>());
	}
	
	@Override
	public String[] getIds() {
		return objectInfoByIds.keySet().toArray(new String[0]);
	}

	@Override
	public <T> T getObject(Class<T> c, String id) {
		ObjectInfo oi=objectInfoByIds.get(id);
		if (oi==null){
			return null;
		}
		return c.cast(oi.object);
	}
	
	@Override
	public Object getObject(String id) {
		ObjectInfo oi=objectInfoByIds.get(id);
		if (oi==null){
			return null;
		}
		return oi.object;
	}

	/**
	 * Returns for a given object its address.
	 * */
	@Override
	public ObjectAddress getObjectAddress(Object o){
		String id=objects.get(o);
		if (id!=null){
			ObjectInfo oi=objectInfoByIds.get(id);
			if (oi==null){
				return null;
			}
			return oi.address;
		}
		return null;
	}
	
	/**
	 * Returns for a given object its proxy.
	 * */
	public Object getObjectProxy(Object o){
		String id=objects.get(o);
		if (id!=null){
			ObjectInfo oi=objectInfoByIds.get(id);
			if (oi==null){
				return null;
			}
			return oi.proxy;
		}
		return null;
	}

	/**
	 * Export consists only of putting the object in the internal structures.
	 * */
	@Override
	public void export(String id,Object o,Class<?>[] interfaces){
		if (id==null){
			throw new IllegalArgumentException("Object id for "+o+" is null.");
		}
		ObjectInfo oi=new ObjectInfo(new ObjectAddress(serverAddress,id,interfaces), o);
		objectInfoByIds.put(id, oi);
		objects.put(o, id);
	}
	
	@Override
	public void export(Object o,Class<?> interf){
		export(interf.getName(),o,new Class[]{interf});
	}
	
	/**
	 * Unexport consists of removing the object of all the internal structures.
	 * */
	@Override
	public void unexport(Object o){
		String id=objects.remove(o);
		if (id!=null){
			objectInfoByIds.remove(id);
		}
	}
	
	/**
	 * Holder containing an object and its address
	 * */
	private class ObjectInfo{
		public ObjectAddress address;
		public Object object;
		public Object proxy;
		
		public ObjectInfo(ObjectAddress address,Object object){
			this.address=address;
			this.object=object;
			this.proxy=Proxy.newProxyInstance(classLoader, address.getInterfaces(), new ClientInvocationHandler(clientInvoker, address));
		}
	}
}
