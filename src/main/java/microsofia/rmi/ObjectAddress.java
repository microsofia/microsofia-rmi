package microsofia.rmi;

import java.io.Serializable;

/**
 * Represents the address of a remote object, which consists of the server address and the object unique id.
 * */
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
	
	/**
	 * Returns the server address that contains the remote object.
	 * */
	public ServerAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * Set the server address that contains the remote object.
	 * */
	public void setServerAddress(ServerAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Returns the unique id of the remote object.
	 * */
	public String getId() {
		return id;
	}

	/**
	 * Sets the unique id of the remote object.
	 * */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the interfaces of the remote object.
	 * */
	public Class<?>[] getInterfaces() {
		return interfaces;
	}

	/**
	 * Sets the interfaces of the remote object.
	 * */
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
	
	//no need of the interfaces in hashcode computation
	@Override
	public int hashCode(){
		return serverAddress.hashCode()+id.hashCode();
	}
	
	//no need of the interfaces in equals implementation
	@Override
	public boolean equals(Object o){
		if (!(o instanceof ObjectAddress)){
			return false;
		}
		ObjectAddress ih=(ObjectAddress)o;
		return serverAddress.equals(ih.serverAddress) && id.equals(ih.id);
	}
}
