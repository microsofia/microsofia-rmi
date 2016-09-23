package microsofia.rmi.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import microsofia.rmi.Registry;
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import java.io.Serializable;
import java.net.InetSocketAddress;

public class ObjectEncoder extends MessageToByteEncoder<Serializable> {
    
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private Server server;
    private Registry registry;
    private ServerAddress remoteServerAddress;

    public ObjectEncoder(Server server,Registry registry,ServerAddress remoteServerAddress){
    	this.server=server;
    	this.registry=registry;
    	this.remoteServerAddress=remoteServerAddress;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();

        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        bout.write(LENGTH_PLACEHOLDER);
        CompactObjectOutputStream oout = new CompactObjectOutputStream(bout,registry);
        oout.writeObject(msg);
        oout.flush();
        oout.close();

        int endIdx = out.writerIndex();

        out.setInt(startIdx, endIdx - startIdx - 4);
        ServerAddress tmp=remoteServerAddress;
        if (tmp==null){
        	InetSocketAddress adr=(InetSocketAddress)ctx.channel().remoteAddress();
        	tmp=new ServerAddress(adr.getHostName(),adr.getPort());
        }
        server.getServerGC().export(oout.getIds(),tmp);
    }
}
