package microsofia.rmi.handler.codec.serialization;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import microsofia.rmi.Server;
import microsofia.rmi.invocation.ClientInvocationHandler;
import microsofia.rmi.invocation.ClientInvoker;
import microsofia.rmi.invocation.ObjectAddress;

public class CompactObjectInputStream extends ObjectInputStream {
	private Server server;
	private ClientInvoker clientInvoker;
	private ClassLoader classLoader;
	private Set<ObjectAddress> oas;

    public CompactObjectInputStream(InputStream in, Server server,ClassLoader classLoader) throws IOException {
        super(in);
        this.server=server;
        this.clientInvoker=server.getClientInvoker();
        this.classLoader=classLoader;
        enableResolveObject(true);
        oas=new HashSet<>();
    }

    public Set<ObjectAddress> getObjectAddress(){
    	return oas;
    }
    
    @Override
    protected Object resolveObject(Object obj) throws IOException {
    	if (obj!=null){
    		if (obj instanceof ObjectAddress){
	    		ObjectAddress oa=(ObjectAddress)obj;
	    		if (oa.getServerAddress().equals(server.getServerAddress())){
	    			//the remote object @ is pointing to the local server. So we replace with the real object
	    			return server.getRegistry().getObject(oa.getId());

	    		}else{
	    			oas.add(oa);
	    			//if it is pointing to a remote object, we replace the @ object by a Proxy
	    			return Proxy.newProxyInstance(classLoader, oa.getInterfaces(), new ClientInvocationHandler(clientInvoker, oa));
	    		}
    		}
    		if (obj instanceof ClientInvocationHandler){
    			//when a remote object/Proxy is read, set the current server in order to be able to do remote calls
    			ClientInvocationHandler ih=(ClientInvocationHandler)obj;
    			ih.setClientInvoker(clientInvoker);
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
