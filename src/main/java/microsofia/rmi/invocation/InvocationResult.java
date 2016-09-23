package microsofia.rmi.invocation;

import java.io.Serializable;

public class InvocationResult implements Serializable{
	private static final long serialVersionUID = 0L;
	private long id;
	private Throwable throwable;
	private Object result;
	
	public InvocationResult(){
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
