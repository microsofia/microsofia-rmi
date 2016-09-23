package microsofia.rmi.handler.codec.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import microsofia.rmi.Registry;
import microsofia.rmi.invocation.ObjectAddress;

public class CompactObjectOutputStream extends ObjectOutputStream {
    static final int TYPE_FAT_DESCRIPTOR = 0;
    static final int TYPE_THIN_DESCRIPTOR = 1;
    private Registry registry;
    private Set<String> ids;

    public CompactObjectOutputStream(OutputStream out,Registry registry) throws IOException {
        super(out);
        this.registry=registry;
        enableReplaceObject(true);
        ids=new HashSet<>();
    }
    
    public Set<String> getIds(){
    	return ids;
    }
    
    @Override
    protected Object replaceObject(Object obj) throws IOException {
        if (!(obj instanceof Serializable)){
    		ObjectAddress oa=registry.getObjectAddress(obj);
    		if (oa!=null){
    			//return the object @ when the object is exported and we are trying to serialize it
    			ids.add(oa.getId());
    			return oa;
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
