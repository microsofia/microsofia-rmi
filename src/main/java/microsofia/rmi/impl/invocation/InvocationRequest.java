package microsofia.rmi.impl.invocation;

import java.io.Serializable;

/**
 * Between a client (that is also a server) and a server, only InvocationRequest and InvocationResult are marshalled and 
 * unmarshalled. InvocationRequest represents a client call.
 * */
public class InvocationRequest implements Serializable{
	private static final long serialVersionUID = 0L;
	//unique id of the call
	private long id;
	//the object id the call is targeting
	private String objectId;
	//the hashcode of the method the call is targeting
	private int method;
	//the arguments to use when calling the method
	private Object[] args;

	public InvocationRequest(){
	}

	public InvocationRequest(long id,String objectId,int method,Object[] args){
		this.id=id;
		this.objectId=objectId;
		this.method=method;
		this.args=args;
	}
	
	/**
	 * Returns the unique id of the call
	 * */
	public long getId() {
		return id;
	}

	/**
	 * Sets the unique id of the call
	 * */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns the object id that the call is targeting
	 * */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * Sets the object id that the call is targeting
	 * */
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	/**
	 * Returns the method hashcode that the call is targeting
	 * */
	public int getMethod() {
		return method;
	}

	/**
	 * Sets the method hashcode that the call is targeting
	 * */
	public void setMethod(int method) {
		this.method = method;
	}

	/**
	 * Returns the arguments to be used when calling the method
	 * */
	public Object[] getArgs() {
		return args;
	}

	/**
	 * Sets the arguments to be used when calling the method
	 * */
	public void setArgs(Object[] args) {
		this.args = args;
	}
}
