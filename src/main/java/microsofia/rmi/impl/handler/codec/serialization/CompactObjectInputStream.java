package microsofia.rmi.impl.handler.codec.serialization;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.IRegistryImpl;
import microsofia.rmi.impl.invocation.ClientInvocationHandler;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.ObjectAddress;

/**
 * Inspired by Netty CompactObjectInputStream.
 * It implements the resolveObject method which replaces the ObjectAddress by a Proxy or the real object
 * */
public class CompactObjectInputStream extends ObjectInputStream {
	//registry of the local server
	@Inject
	private IRegistryImpl registry;
	//address of the local server
	@Inject 
	private ServerAddress serverAddress;
	//clientInvoker of the local server
	@Inject
	private IClientInvoker clientInvoker;
	//classloader of the server used while unmarshalling classes
	@Inject
	private ClassLoader classLoader;
	//set of all ObjectAddress that are read while unmarshalling.
	//it is used to notify the ClientGC in order to start pinging the serverGC of the remote server
	private Set<ObjectAddress> oas;

    public CompactObjectInputStream(InputStream in) throws IOException {
        super(in);
        enableResolveObject(true);
        oas=new HashSet<>();
    }

    public Set<ObjectAddress> getObjectAddress(){
    	return oas;
    }
    
    @Override
    protected Object resolveObject(Object obj) throws IOException {
    	if (obj!=null){
    		if (Proxy.isProxyClass(obj.getClass())){
    			InvocationHandler invocationHandler=Proxy.getInvocationHandler(obj);
    			if (invocationHandler instanceof ClientInvocationHandler){
    				//when a remote object/Proxy is read, set the current server in order to be able to do remote calls
    				ClientInvocationHandler ih=(ClientInvocationHandler)invocationHandler;
    				ih.setClientInvoker(clientInvoker);

    				if (ih.getObjectAddress().getServerAddress().equals(serverAddress)){
    					//the remote object proxy is pointing to the local server. So we replace with the real object
    					return registry.getObject(ih.getObjectAddress().getId());

    				}else{
    					//we add an interest to the local server
    					oas.add(ih.getObjectAddress());
    				}
    			}
    		}
    	}
        return obj;
    }

    @Override
    protected void readStreamHeader() throws IOException {
        int version = readByte() & 0xFF;
        if (version != STREAM_VERSION) {
            throw new StreamCorruptedException("Unsupported version: " + version);
        }
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int type = read();
        if (type < 0) {
            throw new EOFException();
        }
        switch (type) {
        	case CompactObjectOutputStream.TYPE_FAT_DESCRIPTOR:
        		return super.readClassDescriptor();
        	case CompactObjectOutputStream.TYPE_THIN_DESCRIPTOR:
	            String className = readUTF();
	            Class<?> clazz = classLoader.loadClass(className);
	            return ObjectStreamClass.lookupAny(clazz);
        	default:
        		throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        Class<?> clazz;
        try {
            clazz = classLoader.loadClass(desc.getName());
        } catch (ClassNotFoundException ignored) {
            clazz = super.resolveClass(desc);
        }
        return clazz;
    }
}
