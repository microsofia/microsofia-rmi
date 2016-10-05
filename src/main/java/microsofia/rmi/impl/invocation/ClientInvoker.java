package microsofia.rmi.impl.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import io.netty.channel.Channel;
import microsofia.rmi.ObjectAddress;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.impl.invocation.connection.ClientConnection;
import microsofia.rmi.impl.invocation.connection.ClientConnections;

/**
 * Encapsulates the client side of the server to ease Netty handler implementations.
 * 
 * */
public class ClientInvoker implements IClientInvoker{
	//local method hashcodes
	@Inject
	private ClassesMetada classesMetada;
	//all client connections
	@Inject
	private ClientConnections clientConnections;
	//the local server address
	@Inject
	private ServerAddress serverAddress;
	//is the server still running
	private boolean stopped;
	//id to generate unique request/result ids
	private AtomicLong requestCount;
	
	//Maps used to unblock threads waiting the end of a call
	private Map<Long,Request> results;//Request by id
	private Map<Channel,Request> resultsByChannel;//Request by Channel
	
	public ClientInvoker(){
		stopped=false;
		requestCount=new AtomicLong();
		results=new Hashtable<>();
		resultsByChannel=new Hashtable<>();
	}
	
	/**
	 * Frees the resources used by the object. It is used at server shutdown.
	 * It also notifies all waiting threads that the server was shutdown.
	 * */
	public void stop(){
		synchronized(results){
			stopped=true;
			results.notifyAll();
		}
		clientConnections.stop();
		results.clear();
		resultsByChannel.clear();
	}
	
	//store a Request
	private void putRequest(Request rq){
		results.put(rq.id,rq);
		resultsByChannel.put(rq.channel, rq);
	}
	
	//check if a result arrived and remove the request from memory
	private Request removeRequest(long id){
		Request req=results.get(id);
		if (req!=null && req.result!=null){
			results.remove(id);
			resultsByChannel.remove(req.channel);
			return req;
		}
		return null;
	}

	/**
	 * Called by the InvocationHandler of the proxies to do a remote call.
	 * */
	@Override
	public Object invoke(ObjectAddress objectAddress,Method method,Object[] args) throws Throwable{
		//first we generate a new unique id
		long id=requestCount.incrementAndGet();

		//then we need a Channel
		ClientConnection cc=clientConnections.getClientConnection(objectAddress.getServerAddress());
		Channel channel=cc.takeChannel();
		try{
			//we create a request and we store it in memory first
			//in case, the answer is faster ...
			InvocationRequest invocationRequest=new InvocationRequest(id,objectAddress.getId(),classesMetada.getHashCode(method.getDeclaringClass(),method),args);
			putRequest(new Request(id, channel));
		
			//we flush the request to the server
			channel.writeAndFlush(invocationRequest).sync();

			//release the channel in any case
			cc.returnChannel(channel);
		}catch(Throwable th){
			//if an exception happens, then remove it from memory
			removeRequest(id);
			
			//close the channel
			cc.invalidateChannel(channel);
			throw th;
		}

		//wait for the answer arrival
		Request req=null;
		synchronized(results){
			req=removeRequest(id);
			while (!stopped && req==null){
				//if no answer and sever alive, then wait
				try{
					results.wait();
				}catch(Exception e){
				}
				req=removeRequest(id);
			}
		}
		
		if (req==null || req.result==null){
			//no answer, so the server stopped
			throw new IllegalStateException("Server "+serverAddress+" was shutdown.");
		}
		
		//there is an answer
		if (req.result.getThrowable()!=null){
			//it is an exception :(
			Throwable th=req.result.getThrowable();
			
			//we play with the stack trace to have something readable
            List<StackTraceElement> serverStackTrace = Arrays.asList(th.getStackTrace());
            List<StackTraceElement> clientStackTrace = Arrays.asList(Thread.currentThread().getStackTrace());
            List<StackTraceElement> newClientStackTrace = new ArrayList<StackTraceElement>(clientStackTrace);
            newClientStackTrace.remove(0);
            List<StackTraceElement> newServerStackTrace = new ArrayList<StackTraceElement>(serverStackTrace);
            newServerStackTrace.addAll(newClientStackTrace);
            
            th.setStackTrace(newServerStackTrace.toArray(new StackTraceElement[0]));
            
            throw th;
		}
		//no exception :)
		return req.result.getResult();
	}
	
	/**
	 * Netty handler uses this method to notify that a channel closed
	 * */
	@Override
	public void channelClosed(Channel channel,Throwable cause){
		synchronized(results){
			if (!stopped){
				//find the request done on that channel
				Request rq=resultsByChannel.get(channel);
				if (rq!=null){
					rq.setResult(cause);//set an answer and notifies the waiting thread
				}
			}
			results.notifyAll();
		}
	}

	/**
	 * Netty handler uses this method to notify that an answer arrived
	 * */
	@Override
	public void requestDone(InvocationResult result){
		synchronized(results){
			if (!stopped){
				//find the request by id
				Request rq=results.get(result.getId());
				if (rq!=null){
					rq.setResult(result);//set an answer and notifies the waiting thread
				}
			}
			results.notifyAll();
		}
	}
	
	/**
	 * Request object that represents a call with its unique id, the channel on which the call was done
	 * and the result the caller thread is waiting for.
	 * */
	private static class Request{
		public long id;
		public InvocationResult result;
		public Channel channel;

		Request(long id,Channel channel){
			this.id=id;
			this.channel=channel;
		}
		
		void setResult(InvocationResult result){
			this.result=result;
		}

		void setResult(Throwable cause){
			this.result=new InvocationResult();
			result.setThrowable(cause);
		}
	}
}
