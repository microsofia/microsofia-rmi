package microsofia.rmi.impl.invocation;

import java.io.Serializable;

/**
 * Between a client (that is also a server) and a server, only InvocationRequest and InvocationResult are marshalled and 
 * unmarshalled. InvocationResult represents the answer of a client call.
 * */
public class InvocationResult implements Serializable{
	private static final long serialVersionUID = 0L;
	private long id;
	private Throwable throwable;
	private Object result;
	
	public InvocationResult(){
	}

	/**
	 * Returns the unique id of the call. It is the same than the InvocationRequest that triggered the call.
	 * */
	public long getId() {
		return id;
	}

	/**
	 * Sets the unique id of the call. It is the same than the InvocationRequest that triggered the call.
	 * */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns the error that happened during the call.
	 * If it is null, then no error happened and result contains the answer of the call.
	 * */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Sets the error that happened during the call
	 * */
	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	/**
	 * Returns the result of the call.
	 * */
	public Object getResult() {
		return result;
	}

	/**
	 * Sets the result of the call
	 * */
	public void setResult(Object result) {
		this.result = result;
	}
}
