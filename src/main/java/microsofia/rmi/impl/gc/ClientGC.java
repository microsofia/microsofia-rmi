package microsofia.rmi.impl.gc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerConfiguration;
import microsofia.rmi.impl.IServerImpl;
import microsofia.rmi.impl.invocation.ObjectAddress;

/**
 * Client side of a server that pings other remote server(s) in order to indicate the exported objects in which the local
 * one is interested in.
 * 
 * */
public class ClientGC implements IClientGC{
	private static Log log = LogFactory.getLog(ClientGC.class);
	//the local server
	@Inject
    private IServerImpl server;
	//the local server configuration
	@Inject
    private ServerConfiguration serverConfiguration;
	//scheduledExecutorService of the server
	@Inject 
	private ScheduledExecutorService scheduledExecutorService;
	//executorService of the server
	@Inject 
	private ExecutorService executorService;
	/*
	 * for every remote server adr, ClientGC keeps a ServerInfo containing all the ids of the exported objects
	 * it is interested in.
	 * */
	private Map<ServerAddress, ServerInfo> serverInfos;

	public ClientGC() {
        this.serverInfos = new Hashtable<ServerAddress, ServerInfo>();
    }

	@Override
	public synchronized ServerAddress[] getServerAddress() throws Exception {
        return serverInfos.keySet().toArray(new ServerAddress[0]);
    }

	/**
	 * Adds to the ClientGC new ObjectAddress that the local server is interested in.
	 * */
    public synchronized void add(Set<ObjectAddress> oas) throws Exception {
        for (ObjectAddress oa : oas) {
            ServerAddress remoteServerAddress = oa.getServerAddress();
            ServerInfo serverInfo = serverInfos.get(remoteServerAddress);
            if (serverInfo == null) {
                serverInfo = new ServerInfo(remoteServerAddress);
                serverInfos.put(remoteServerAddress, serverInfo);
            }
            serverInfo.export(oa.getId());
        }
    }

    /**
     * Stop the pinging. Called at the shutdown of the server.
     * */
    public void unexport() {
        ServerInfo[] si = null;
        synchronized (this) {
            si = serverInfos.values().toArray(new ServerInfo[0]);
        }
        for (ServerInfo i : si) {
            i.unexport();
        }
    }

    /**
     * ServerInfo represents a remote Server and contains all the needed info to ping the remote server
     * */
    private class ServerInfo implements Runnable {
        private Future<?> future;
        //the remote ServerGC
        private IServerGC gc;
        //how many exceptions already happened with that server
        private int gcExceptionCount;
        //is something already running?
        private AtomicBoolean isRunning;
        //set of the objects ids that the local client is interested in and is pinging the remote server with
        private Set<String> ids;
        //remote server address the client is pinging
        private ServerAddress remoteServerAddress;
        private ScheduledFuture<?> scheduledFuture;

        public ServerInfo(ServerAddress remoteServerAddress) throws Exception {
            this.remoteServerAddress = remoteServerAddress;
            this.ids= Collections.synchronizedSet(new HashSet<String>());
            this.gc = server.lookup(remoteServerAddress, IServerGC.class);
            this.isRunning = new AtomicBoolean();
        }

        /**
         * Adding an object id for that remote server
         * */
        public void export(String id) {
            if (ids.size() == 0) {
                scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this, serverConfiguration.getClientGCPeriod(),serverConfiguration.getClientGCPeriod(), TimeUnit.MILLISECONDS);
            }
            ids.add(id);
        }

        /**
         * Removing an object id for that remote server
         * */
        public void unexport(String id) {
            ids.remove(id);
            if (ids.size() == 0) {
                unexport();
            }
        }

        /**
         * ping method that is called periodically.
         * */
        public void ping() {
            String[] idsArray = ids.toArray(new String[0]);
            boolean[] ping = null;
            long t1 = 0;
            
            //calling the server and handling the exception
            try {
                t1 = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug(ServerInfo.this.toString()+": pinging.");
                }
                ping = gc.ping(server.getServerAddress(), idsArray);
            } catch (Throwable e) {
                if (log.isDebugEnabled()) {
                    log.debug(ServerInfo.this.toString()+": error while pinging.", e);
                }
                if (++gcExceptionCount >= serverConfiguration.getClientGCExceptionThreshold()) {
                    if (log.isDebugEnabled()) {
                        log.debug(ServerInfo.this.toString()+": pinging at " + remoteServerAddress + " failed after " + gcExceptionCount + " attempts. The remote address will no longer be pinged. The last received exception follows", e);
                    }
                    unexport();
                    return;
                }
                return;
            } finally {
                if (log.isDebugEnabled()) {
                    long rtt = System.currentTimeMillis() - t1;
                    log.debug(ServerInfo.this.toString()+": pinging RTT: " + rtt + "ms");
                }
            }
            
            //handling the returned results from the server
            gcExceptionCount = 0;
            for (int i = 0; i < ping.length; i++) {
                if (!ping[i]) {
                    if (log.isDebugEnabled()) {
                        log.debug(ServerInfo.this.toString()+": pinging informed that objects were unexported from " + remoteServerAddress + ", unexporting object of ID: " + idsArray[i]);
                    }
                    unexport(idsArray[i]);
                }
            }
        }

        //removing the ServerInfo from ClientGC. This is used when the server is considered dead.
        public void unexport() {
            synchronized (this) {
                serverInfos.remove(remoteServerAddress);
                if (future != null) {
                    future.cancel(false);
                }
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        }

        //this method is called periodically
        @Override
        public void run() {
            if (!isRunning.get()) {
                future = executorService.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        String oldName = Thread.currentThread().getName();
                        try {
                            isRunning.set(true);
                            Thread.currentThread().setName("ClientGC Thread - " + ServerInfo.this.toString());
                            ping();
                        } finally {
                            Thread.currentThread().setName(oldName);
                            isRunning.set(false);
                        }
                        return null;
                    }
                });
            }
        }

        @Override
        public String toString() {
            return "ClientGC[RemoteServerAddress: " + remoteServerAddress + ", Num Exported Objects: " + ids.size() + "]";
        }
    }
}
