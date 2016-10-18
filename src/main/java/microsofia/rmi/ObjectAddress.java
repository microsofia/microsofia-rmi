package microsofia.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the address of a remote object, which consists of the server address and the object unique id.
 * */
public class ObjectAddress implements Externalizable {
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
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(getId());
		out.writeUTF(getServerAddress().getHost());
		out.writeInt(getServerAddress().getPort());
		out.writeInt(getInterfaces().length);
		for (Class<?> c : getInterfaces()){
			out.writeUTF(c.getName());
		}
    }

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    	setId(in.readUTF());
    	
    	ServerAddress sa=new ServerAddress();
    	setServerAddress(sa);
    	
    	sa.setHost(in.readUTF());
    	sa.setPort(in.readInt());

    	List<Class<?>> interfaces=new ArrayList<>();

    	int l=in.readInt();
    	for (int i=0;i<l;i++){
			interfaces.add(Class.forName(in.readUTF()));
    	}
    	setInterfaces(interfaces.toArray(new Class<?>[0]));
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
