An RMI implementation based on (powerful) [Netty](https://github.com/netty/netty) 


#Example


##The object to export/locate
```

	//the interface of the remote object
	public interface ISample{
		public void doStuff();		
	}

	//the implementation
	public class Sample implements ISample{
		
		public Sample(){
		}
	
		@Override
		public void doStuff(){
			...
		}
	}
```

##Server side


```

	//Creating a server
	ServerBuilder serverBuilder1=new ServerBuilder().host("server_host").port(9999);
	Server server1=serverBuilder1.build();
	server1.start();

	//Creating the remote object
	Sample sample=new Sample();
	...
	
	//exporting the remote object to the server
	server1.export(sample,ISample.class);
 
 ```
 
##Client side
 
``` 

	//Creating a server
	ServerBuilder serverBuilder2=new ServerBuilder().host("client_host").port(9998);
	Server server2=serverBuilder2.build();			
	server2.start();		

	//locating the remote server
	IServer remoteServer1=server2.getServer("server_host",9999);

	//locating the registry of the remote server
	IRegistry remoteRegistry1=remoteServer1.getRegistry();

	//now we can get the remote object sample
	ISample remoteSample=remoteRegistry1.getObject(ISample.class,ISample.class.getName());
	
	//we can now call Sample using the remote proxy
	remoteSample.doStuff();
```

 