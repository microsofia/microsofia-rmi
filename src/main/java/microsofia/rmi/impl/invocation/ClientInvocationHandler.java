package microsofia.rmi.impl.invocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Invocation handler used within all the created Proxies.
 * */
public class ClientInvocationHandler implements InvocationHandler{
	/**
	 * The clientInvoker of the local server. It is transient as it points to the local server.
	 * When unmarshalled by any server, it is set to the local one.
	 * */
	private transient IClientInvoker clientInvoker;
	//the remote object address that this proxy points to
	private ObjectAddress objectAddress;
	
	public ClientInvocationHandler(IClientInvoker clientInvoker,ObjectAddress objectAddress){
		this.clientInvoker=clientInvoker;
		this.objectAddress=objectAddress;
	}
	
	//used while unmarshalling
	public void setClientInvoker(IClientInvoker clientInvoker){
		this.clientInvoker=clientInvoker;
	}
	
	/**
	 * If the method is an Object one, delegate to the current object.
	 * If not, do a remote call.
	 * */
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
	
	//the following implementation should allow 2 proxies pointing to the same remote object
	//to be equally true.
	@Override
	public boolean equals(Object o){
		ClientInvocationHandler cih=null;
		
		InvocationHandler ih=Proxy.getInvocationHandler(o);
		if (ih!=null){
			if (ih instanceof ClientInvocationHandler){
				cih=(ClientInvocationHandler)ih;
			}

		}else if (o instanceof ClientInvocationHandler){
			cih=(ClientInvocationHandler)o;
		}

		if (cih==null){
			return false;
		}
		return objectAddress.equals(cih.objectAddress);
	}
}
