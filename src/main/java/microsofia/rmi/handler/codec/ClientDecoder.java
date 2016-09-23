package microsofia.rmi.handler.codec;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;
import microsofia.rmi.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.invocation.ClientInvoker;
import microsofia.rmi.invocation.InvocationResult;

@Sharable
public class ClientDecoder extends MessageToMessageDecoder<InvocationResult>{
	private final Log log = LogFactory.getLog(ObjectEncoder.class);
	private ClientInvoker clientInvoker;

	public ClientDecoder(ClientInvoker clientInvoker){
		this.clientInvoker=clientInvoker;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, InvocationResult msg, List<Object> out) throws Exception {
		clientInvoker.requestDone(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {		
		log.error(cause.getMessage(),cause);
		ctx.close();
	}
}