package microsofia.rmi.invocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ClientInvocationHandler implements InvocationHandler{
	private transient ClientInvoker clientInvoker;
	private ObjectAddress objectAddress;
	
	public ClientInvocationHandler(ClientInvoker clientInvoker,ObjectAddress objectAddress){
		this.clientInvoker=clientInvoker;
		this.objectAddress=objectAddress;
	}
	
	public void setClientInvoker(ClientInvoker clientInvoker){
		this.clientInvoker=clientInvoker;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().equals(Object.class)){
			return method.invoke(this, args);
		}
		return clientInvoker.invoke(objectAddress,method,args);
	}

	@Override
	public String toString(){
		return "Proxy:[ObjectAddress:"+objectAddress+"]";
	}
	
	@Override
	public int hashCode(){
		return objectAddress.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof ClientInvocationHandler)){
			return false;
		}
		ClientInvocationHandler ih=(ClientInvocationHandler)o;
		return objectAddress.equals(ih.objectAddress);
	}
}
