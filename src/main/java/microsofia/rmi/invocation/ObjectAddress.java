package microsofia.rmi.invocation;

import java.io.Serializable;

import microsofia.rmi.ServerAddress;

public class ObjectAddress implements Serializable {
	private static final long serialVersionUID = 0L;
	private ServerAddress serverAddress;
	private String id;
	private Class<?>[] interfaces;
	
	public ObjectAddress(){
	}

	public ObjectAddress(ServerAddress serverAddress,String id,Class<?>[] interfaces){
		this.serverAddress=serverAddress;
		this.id=id;
		this.interfaces=interfaces;
	}
	
	public ServerAddress getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(ServerAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Class<?>[] getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(Class<?>[] interfaces) {
		this.interfaces = interfaces;
	}
	
	@Override
	public String toString(){
		String str= "";
        if (getInterfaces()!=null){
        	for (Class<?> c : getInterfaces()){
        		str += c.getName() + ", ";
        	}
        }
		return "[ServerAddress:"+serverAddress+"][Id:"+id+"][Interfaces:"+str+"]";
	}
	
	@Override
	public int hashCode(){
		return serverAddress.hashCode()+id.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof ObjectAddress)){
			return false;
		}
		ObjectAddress ih=(ObjectAddress)o;
		return serverAddress.equals(ih.serverAddress) && id.equals(ih.id);
	}
}
