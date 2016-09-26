package microsofia.rmi.impl.invocation;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import io.netty.channel.ChannelHandlerContext;
import microsofia.rmi.impl.IRegistryImpl;

/**
 * Netty handler abstraction of the server. Called each time a handler unmarshalls an InvocationRequest.
 * */
public class ServerInvoker implements IServerInvoker{
	//local registry
	@Inject
	private IRegistryImpl registry; 
	//local method hashcode
	@Inject
	private ClassesMetada classesMetada;
	//local executor
	@Inject
	private ExecutorService executorService;
	
	public ServerInvoker(){
	}

	public Future<InvocationResult> invoke(final ChannelHandlerContext ctx,final InvocationRequest request){
		Future<InvocationResult> future=executorService.submit(new Callable<InvocationResult>() {
			@Override
			public InvocationResult call() throws InterruptedException{
				InvocationResult invocationResult=new InvocationResult();//TODO: name the thread?
				
				//setting the result id as the request id
				invocationResult.setId(request.getId());

				try{
					Object o=registry.getObject(request.getObjectId());
					if (o==null){
						//the object is not exported
						throw new IllegalStateException("Object with id "+request.getObjectId()+" is not exported anymore.");
					}
					
					Method method=classesMetada.getMethod(o.getClass(), request.getMethod());
					Object result=method.invoke(o, request.getArgs());
					invocationResult.setResult(result);
				}catch(Throwable th){
					//an error happened, it should be sent back to the client
					invocationResult.setThrowable(th);
				}

				ctx.writeAndFlush(invocationResult).sync();
				return invocationResult;
			}
		});
		return future;
	}
}