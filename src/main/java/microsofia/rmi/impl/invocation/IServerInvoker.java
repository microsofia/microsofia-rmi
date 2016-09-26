package microsofia.rmi.impl.invocation;

import java.util.concurrent.Future;

import io.netty.channel.ChannelHandlerContext;

/**
 * Represents the server side of the server abstracted for Netty handlers.
 * */
public interface IServerInvoker {

	/**
	 * Netty handler will call the following method, each time it unmarshalled an InvocationRequest
	 * */
	public Future<InvocationResult> invoke(ChannelHandlerContext ctx,InvocationRequest request);
}
