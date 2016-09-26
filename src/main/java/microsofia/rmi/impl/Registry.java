package microsofia.rmi.impl;

import java.util.Collections;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.inject.Inject;

import microsofia.rmi.ServerAddress;
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
	 * Export consists only of putting the object in the internal structures.
	 * */
	@Override
	public void export(String id,Object o,Class<?>[] interfaces){
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
	private static class ObjectInfo{
		public ObjectAddress address;
		public Object object;
		
		public ObjectInfo(ObjectAddress address,Object object){
			this.address=address;
			this.object=object;
		}
	}
}
