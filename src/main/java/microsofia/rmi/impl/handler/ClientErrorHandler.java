package microsofia.rmi.impl.handler;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import microsofia.rmi.impl.invocation.IClientInvoker;
import microsofia.rmi.impl.invocation.connection.ClientConnection;

/**
 * Handler that notifies the client side that an exception happened with the associated channel.
 * */
@Sharable
public class ClientErrorHandler extends ChannelInboundHandlerAdapter{
	private final Log log = LogFactory.getLog(ClientErrorHandler.class);
	@Inject
	private IClientInvoker clientInvoker;
	private ClientConnection clientConnection;

	public ClientErrorHandler(ClientConnection clientConnection){
		this.clientConnection=clientConnection;
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(),cause);
		clientInvoker.channelClosed(ctx.channel(),cause);
		
		clientConnection.channelClosed(ctx.channel(),cause);

		ctx.close();
    }
}
