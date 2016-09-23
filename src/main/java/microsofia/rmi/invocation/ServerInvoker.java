package microsofia.rmi.invocation;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.netty.channel.ChannelHandlerContext;
import microsofia.rmi.Registry;

public class ServerInvoker {
	private Registry registry; 
	private ClassesMetada classesMetada;
	private ExecutorService executorService;
	
	public ServerInvoker(Registry registry,ClassesMetada classesMetada){
		this.registry=registry;
		this.classesMetada=classesMetada;
		this.executorService=Executors.newCachedThreadPool();
	}

	public Future<InvocationResult> invoke(final ChannelHandlerContext ctx,final InvocationRequest request){
		Future<InvocationResult> future=executorService.submit(new Callable<InvocationResult>() {
			@Override
			public InvocationResult call() throws InterruptedException{
				InvocationResult invocationResult=new InvocationResult();//TODO: name the thread?
				invocationResult.setId(request.getId());
				try{
					Object o=registry.getObject(request.getObjectId());
					if (o==null){
						throw new IllegalStateException("Object with id "+request.getObjectId()+" is not exported anymore.");
					}
					Method method=classesMetada.getMethod(o.getClass(), request.getMethod());
					Object result=method.invoke(o, request.getArgs());
					invocationResult.setResult(result);
				}catch(Throwable th){
					invocationResult.setThrowable(th);
				}

				ctx.writeAndFlush(invocationResult).sync();
				return invocationResult;
			}
		});
		return future;
	}
	
	public void stop(){
		executorService.shutdown();
	}
}