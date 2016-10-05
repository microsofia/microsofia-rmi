package microsofia.rmi.impl.invocation;

import java.lang.reflect.Method;
import io.netty.channel.Channel;
import microsofia.rmi.ObjectAddress;

/**
 * Represents the client side of the server.
 * */
public interface IClientInvoker {

	/**
	 * Frees any resource used by this object. It is called at server shutdown.
	 * */
	public void stop();

	/**
	 * Used by the InvocationHandler of the proxies in order to call the remote objects.
	 * */
	public Object invoke(ObjectAddress objectAddress,Method method,Object[] args) throws Throwable;

	/**
	 * Used to notify that a channel was closed in order to notify and wake up the associated call done
	 * on the channel.
	 * */
	public void channelClosed(Channel channel,Throwable cause);

	/**
	 * Method called by a Netty handler when a result is sent by the server.
	 * */
	public void requestDone(InvocationResult result);
}
