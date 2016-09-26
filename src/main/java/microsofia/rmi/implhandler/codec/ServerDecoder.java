package microsofia.rmi.implhandler.codec;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;
import microsofia.rmi.impl.invocation.IServerInvoker;
import microsofia.rmi.impl.invocation.InvocationRequest;

/**
 * Netty handler used to notify when a request (InvocationRequest) has arrived, is unmarshalled.
 * */
@Sharable
public class ServerDecoder extends MessageToMessageDecoder<InvocationRequest>{
	private final Log log = LogFactory.getLog(ServerDecoder.class);
	//local serverInvoker
	@Inject
	private IServerInvoker serverInvoker;

	public ServerDecoder(){
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, InvocationRequest msg, List<Object> out) throws Exception {
		serverInvoker.invoke(ctx,msg);//a new request has arrived
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(), cause);
		ctx.close();
	}
};
