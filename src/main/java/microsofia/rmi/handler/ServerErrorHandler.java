package microsofia.rmi.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class ServerErrorHandler extends ChannelOutboundHandlerAdapter{
	private final Log log = LogFactory.getLog(ServerErrorHandler.class);

	public ServerErrorHandler(){
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause.getMessage(),cause);
        ctx.close();
    }
}
