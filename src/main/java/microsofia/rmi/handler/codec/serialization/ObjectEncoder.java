package microsofia.rmi.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import microsofia.rmi.Registry;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectEncoder extends MessageToByteEncoder<Serializable> {
    
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private Registry registry;

    public ObjectEncoder(Registry registry){
    	this.registry=registry;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();

        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        bout.write(LENGTH_PLACEHOLDER);
        ObjectOutputStream oout = new CompactObjectOutputStream(bout,registry);
        oout.writeObject(msg);
        oout.flush();
        oout.close();

        int endIdx = out.writerIndex();

        out.setInt(startIdx, endIdx - startIdx - 4);
    }
}
