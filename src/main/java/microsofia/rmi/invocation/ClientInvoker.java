package microsofia.rmi.invocation;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import microsofia.rmi.Registry;
import microsofia.rmi.Server;
import microsofia.rmi.invocation.connection.ClientConnection;
import microsofia.rmi.invocation.connection.ClientConnections;

public class ClientInvoker {
	private Server server;
	private ClassesMetada classesMetada;
	private ClientConnections clientConnections;
	private boolean stopped;
	private AtomicLong requestCount;
	private Map<Long,Request> results;
	private Map<Channel,Request> resultsByChannel;
	
	public ClientInvoker(Server server,ClassesMetada classesMetada,Registry registry,EventLoopGroup group,GenericObjectPoolConfig config){
		this.server=server;
		this.classesMetada=classesMetada;
		this.clientConnections=new ClientConnections(server,registry,group,config);
		stopped=false;
		requestCount=new AtomicLong();
		results=new Hashtable<>();
		resultsByChannel=new Hashtable<>();
	}
	
	public void stop(){
		synchronized(results){
			stopped=true;
			results.notifyAll();
		}
		clientConnections.stop();
		results.clear();
		resultsByChannel.clear();
	}
	
	private void putRequest(Request rq){
		results.put(rq.id,rq);
		resultsByChannel.put(rq.channel, rq);
	}
	
	private Request removeRequest(long id){
		Request req=results.get(id);
		if (req!=null && req.result!=null){
			results.remove(id);
			resultsByChannel.remove(req.channel);
			return req;
		}
		return null;
	}
	
	public Object invoke(ObjectAddress objectAddress,Method method,Object[] args) throws Throwable{
		long id=requestCount.incrementAndGet();

		ClientConnection cc=clientConnections.getClientConnection(objectAddress.getServerAddress());
		Channel channel=cc.takeChannel();
		try{
			InvocationRequest invocationRequest=new InvocationRequest(id,objectAddress.getId(),classesMetada.getHashCode(method.getDeclaringClass(),method),args);
			putRequest(new Request(id, channel));
		
			channel.writeAndFlush(invocationRequest).sync();
		}catch(Throwable th){
			removeRequest(id);
			throw th;
		}finally{
			cc.returnChannel(channel);
		}

		Request req=null;
		synchronized(results){
			req=removeRequest(id);
			while (!stopped && req==null){
				try{
					results.wait();
				}catch(Exception e){
				}
				req=removeRequest(id);
			}
		}
		if (req==null || req.result==null){
			throw new IllegalStateException("Server "+server.getServerAddress()+" was shutdown.");
		}
		if (req.result.getThrowable()!=null){
			throw req.result.getThrowable();
		}
		return req.result.getResult();
	}
	
	public void channelClosed(Channel channel,Throwable cause){
		synchronized(results){
			if (!stopped){
				Request rq=resultsByChannel.get(channel);
				if (rq!=null){
					rq.setResult(cause);
				}
			}
			results.notifyAll();
		}
	}
	
	public void requestDone(InvocationResult result){
		synchronized(results){
			if (!stopped){
				Request rq=results.get(result.getId());
				if (rq!=null){
					rq.setResult(result);
				}
			}
			results.notifyAll();
		}
	}
	
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
