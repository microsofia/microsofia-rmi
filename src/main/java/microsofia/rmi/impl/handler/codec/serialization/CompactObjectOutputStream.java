package microsofia.rmi.impl.handler.codec.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import microsofia.rmi.impl.IRegistryImpl;
import microsofia.rmi.impl.invocation.ObjectAddress;

/**
 * Inspired by Netty CompactObjectOutputStream.
 * It implements the replaceObject method which replaces the exported object by its proxy.
 * */
public class CompactObjectOutputStream extends ObjectOutputStream {
    static final int TYPE_FAT_DESCRIPTOR = 0;
    static final int TYPE_THIN_DESCRIPTOR = 1;
    //registry of the local server
    @Inject
    private IRegistryImpl registry;
    //ids of all exported objects encountered while marshalling.
    //it is used to notify ServerGC that the current client server is interested in the following exported objects.
    private Set<String> ids;

    public CompactObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
        ids=new HashSet<>();
    }
    
    /**
     * Ids of the encountered exported objects
     */
    public Set<String> getIds(){
    	return ids;
    }
    
    @Override
    protected Object replaceObject(Object obj) throws IOException {
        if (!(obj instanceof Serializable)){
    		ObjectAddress oa=registry.getObjectAddress(obj);
    		if (oa!=null){
    			//mark that the current channel is interested in the this object id
    			ids.add(oa.getId());
    			
    			Object proxy=registry.getObjectProxy(obj);
    			//return the object proxy when the object is exported and we are trying to serialize it
    			return proxy;
    		}
    	}
    	return obj;
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        writeByte(STREAM_VERSION);
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        Class<?> clazz = desc.forClass();
        if (clazz.isPrimitive() || clazz.isArray() || clazz.isInterface() ||
            desc.getSerialVersionUID() == 0) {
            write(TYPE_FAT_DESCRIPTOR);
            super.writeClassDescriptor(desc);
        } else {
            write(TYPE_THIN_DESCRIPTOR);
            writeUTF(desc.getName());
        }
    }
}
