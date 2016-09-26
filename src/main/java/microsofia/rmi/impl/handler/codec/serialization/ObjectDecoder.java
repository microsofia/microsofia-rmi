package microsofia.rmi.impl.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import microsofia.rmi.impl.IInjectorProvider;
import microsofia.rmi.impl.gc.ClientGC;

import javax.inject.Inject;

import com.google.inject.name.Named;

/**
 * Inspired by Netty ObjectDecoder.
 * */
public class ObjectDecoder extends LengthFieldBasedFrameDecoder {
	//local ClientGC
	@Inject
	private ClientGC clientGC;
	//local Guice Injector
	@Inject 
	@Named("injector")
	private IInjectorProvider provider;

    public ObjectDecoder() {
    	this(1048576);
    }

    public ObjectDecoder(int maxObjectSize) {
        super(maxObjectSize, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        CompactObjectInputStream is = new CompactObjectInputStream(new ByteBufInputStream(frame));
        provider.get().injectMembers(is);
        Object result = is.readObject();
        is.close();
        
        //notifying ClientGC of the encountered remote server and its exported objects while unmarshalling
        clientGC.add(is.getObjectAddress());
        
        return result;
    }

    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.slice(index, length);
    }
}
