package microsofia.rmi;

import java.io.Serializable;

public class ServerAddress implements Serializable{
	private static final long serialVersionUID = 0L;
	private String host;
	private int port;
	
	public ServerAddress(){
	}

	public ServerAddress(String host,int port){
		this.host=host;
		this.port=port;
	}	
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString(){
		return "[Host:"+host+"][Port:"+port+"]";
	}
	
	@Override
	public int hashCode(){
		return host.hashCode()+port;
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof ServerAddress)){
			return false;
		}
		ServerAddress ih=(ServerAddress)o;
		return host.equals(ih.host) && port==ih.port;
	}
}
