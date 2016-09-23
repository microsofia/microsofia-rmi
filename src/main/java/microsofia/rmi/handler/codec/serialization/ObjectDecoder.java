package microsofia.rmi.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import microsofia.rmi.Server;

import java.io.ObjectInputStream;

public class ObjectDecoder extends LengthFieldBasedFrameDecoder {
	private Server server;
	private ClassLoader classLoader;

    public ObjectDecoder(Server server,ClassLoader classLoader) {
    	this(1048576,server,classLoader);
    }

    public ObjectDecoder(int maxObjectSize, Server server,ClassLoader classLoader) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.server=server;
        this.classLoader=classLoader;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        ObjectInputStream is = new CompactObjectInputStream(new ByteBufInputStream(frame), server,classLoader);
        Object result = is.readObject();
        is.close();
        return result;
    }

    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.slice(index, length);
    }
}
