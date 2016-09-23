package microsofia.rmi.handler.codec;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;
import microsofia.rmi.invocation.InvocationRequest;
import microsofia.rmi.invocation.ServerInvoker;

@Sharable
public class ServerDecoder extends MessageToMessageDecoder<InvocationRequest>{
	private final Log log = LogFactory.getLog(ServerDecoder.class);
	private ServerInvoker serverInvoker;

	public ServerDecoder(ServerInvoker serverInvoker){
		this.serverInvoker=serverInvoker;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, InvocationRequest msg, List<Object> out) throws Exception {
		serverInvoker.invoke(ctx,msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(), cause);
		ctx.close();
	}
};
