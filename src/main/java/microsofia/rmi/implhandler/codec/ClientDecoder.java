package microsofia.rmi.implhandler.codec;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;
import microsofia.rmi.impl.handler.codec.serialization.ObjectEncoder;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.InvocationResult;

/**
 * Netty handler used to notify when a result (InvocationResult) has arrived, is unmarshalled.
 * */
@Sharable
public class ClientDecoder extends MessageToMessageDecoder<InvocationResult>{
	private final Log log = LogFactory.getLog(ObjectEncoder.class);
	//local clientinvoker
	@Inject
	private IClientInvoker clientInvoker;

	public ClientDecoder(){
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, InvocationResult msg, List<Object> out) throws Exception {
		clientInvoker.requestDone(msg);//an answer arrived
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {		
		log.error(cause.getMessage(),cause);
		ctx.close();
	}
}