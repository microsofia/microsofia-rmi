package microsofia.rmi.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import microsofia.rmi.invocation.ClientInvoker;

@Sharable
public class ClientErrorHandler extends ChannelInboundHandlerAdapter{
	private final Log log = LogFactory.getLog(ClientErrorHandler.class);
	private ClientInvoker clientInvoker;

	public ClientErrorHandler(ClientInvoker clientInvoker){
		this.clientInvoker=clientInvoker;
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(),cause);
		clientInvoker.channelClosed(ctx.channel(),cause);
        ctx.close();
    }
}
