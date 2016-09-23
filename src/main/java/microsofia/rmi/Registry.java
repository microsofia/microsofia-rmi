package microsofia.rmi;

import java.util.Collections;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import microsofia.rmi.invocation.ObjectAddress;

public class Registry implements IRegistry{
	private Map<String,ObjectInfo> objectInfoByIds;
	private Map<Object,String> objects;
	private ServerAddress serverAddress;

	public Registry(ServerAddress serverAddress){
		this.serverAddress=serverAddress;
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

	public void export(String id,Object o,Class<?>[] interfaces){
		ObjectInfo oi=new ObjectInfo(new ObjectAddress(serverAddress,id,interfaces), o);
		objectInfoByIds.put(id, oi);
		objects.put(o, id);
	}
	
	public void export(Object o,Class<?> interf){
		export(interf.getName(),o,new Class[]{interf});
	}
	
	public void unexport(Object o){
		String id=objects.remove(o);
		if (id!=null){
			objectInfoByIds.remove(id);
		}
	}
	
	private static class ObjectInfo{
		public ObjectAddress address;
		public Object object;
		
		public ObjectInfo(ObjectAddress address,Object object){
			this.address=address;
			this.object=object;
		}
	}
}
