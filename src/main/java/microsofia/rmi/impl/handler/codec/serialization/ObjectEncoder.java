package microsofia.rmi.impl.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.IInjectorProvider;
import microsofia.rmi.impl.gc.ServerGC;

import java.io.Serializable;
import java.net.InetSocketAddress;

import javax.inject.Inject;
import com.google.inject.name.Named;

/**
 * Inspired by Netty ObjectEncoder.
 * */
public class ObjectEncoder extends MessageToByteEncoder<Serializable> {    
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    //local ServerGC
    @Inject
    private ServerGC serverGC;
    //local Guice Injector
	@Inject 
	@Named("injector")
	private IInjectorProvider provider;
	//the address of the client server
    private ServerAddress remoteServerAddress;

    public ObjectEncoder(ServerAddress remoteServerAddress){
    	this.remoteServerAddress=remoteServerAddress;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();

        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        bout.write(LENGTH_PLACEHOLDER);
        CompactObjectOutputStream oout = new CompactObjectOutputStream(bout);
        provider.get().injectMembers(oout);
        oout.writeObject(msg);
        oout.flush();
        oout.close();

        int endIdx = out.writerIndex();

        out.setInt(startIdx, endIdx - startIdx - 4);
        ServerAddress tmp=remoteServerAddress;
        if (tmp==null){//TODO
        	InetSocketAddress adr=(InetSocketAddress)ctx.channel().remoteAddress();
        	tmp=new ServerAddress(adr.getHostName(),adr.getPort());
        }
        
        //notifies the local server GC of the encountered client server and the objects it is interested in
        serverGC.add(oout.getIds(),tmp);
    }
}
