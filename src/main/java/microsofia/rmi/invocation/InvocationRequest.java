package microsofia.rmi.invocation;

import java.io.Serializable;

public class InvocationRequest implements Serializable{
	private static final long serialVersionUID = 0L;
	private long id;
	private String objectId;
	private int method;
	private Object[] args;

	public InvocationRequest(){
	}

	public InvocationRequest(long id,String objectId,int method,Object[] args){
		this.id=id;
		this.objectId=objectId;
		this.method=method;
		this.args=args;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public int getMethod() {
		return method;
	}

	public void setMethod(int method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}
}
