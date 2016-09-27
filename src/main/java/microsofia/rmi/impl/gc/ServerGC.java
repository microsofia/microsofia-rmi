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

import microsofia.rmi.IClientInterestListener;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerConfiguration;

/**
 * Server side of a server that checks that clients have pinged recently and showed interested in exported objects.
 * If the clients didn't ping during since last check, it will consider it as dead or not showing interest and 
 * notify a listener on the server side.
 * */
public class ServerGC implements IServerGC{
	private static Log log = LogFactory.getLog(ServerGC.class);
	//server configuration of the local server
	@Inject
	private ServerConfiguration serverConfiguration;
	//used scheduledExecutorService
	@Inject 
	private ScheduledExecutorService scheduledExecutorService;
	//used executorService
	@Inject 
	private ExecutorService executorService;
	//listener set within the local server in order to be notified of client interests adding/removal
	@Inject
	private IClientInterestListener clientInterestListener;
	/**
	 * ServerGC keep for every client server address a structure ServerInfo.
	 * */
	private Map<ServerAddress, ServerInfo> serverInfos;

    public ServerGC() {
        this.serverInfos = new Hashtable<ServerAddress, ServerInfo>();
    }

    @Override
    public synchronized ServerAddress[] getClientAddress() {
        return serverInfos.keySet().toArray(new ServerAddress[0]);
    }

    /**
     * Ping method used by client servers in order to show their interest in the locally exported objects.
     * 
     * @param adr the client server address
     * @param ids the ids of the exported objects
     * @return an array of boolean, for every id it indicates if the object is still exported or not
     * */
    @Override
    public boolean[] ping(ServerAddress adr, String[] ids) throws Throwable {
        boolean[] returnPing = new boolean[ids.length];
        ServerInfo info= serverInfos.get(adr);
        if (info != null) {
            info.ping(returnPing, ids);
        }
        return returnPing;
    }

    /**
     * Add for the client server address an interest of the locally exported objects
     * 
     * @param objIds the locally exported objects
     * @param adr the client server address
     * */
    public synchronized void add(Set<String> objIds, ServerAddress adr) {
        for (String id : objIds) {
            add(id, adr);
        }
    }

    /**
     * Removes any reference and structure that the server GC had for the client server address
     * */
    public void remove(ServerAddress adr) {
        ServerInfo serverInfo;
        synchronized (this) {
            serverInfo = serverInfos.get(adr);
        }
        if (serverInfo != null) {
            serverInfo.stop();
        }
    }

    /**
     * Frees resources and running tasks. Called at server shutdown.
     * */
    public void stop() {
        ServerInfo[] infos = null;
        synchronized (this) {
            infos = serverInfos.values().toArray(new ServerInfo[0]);
        }
        for (ServerInfo epi : infos) {
            epi.stop();
        }
    }

    @Override
    public void clientDead(ServerAddress adr) throws Throwable {
        remove(adr);
    }

    //add interest for one object id
    private void add(String id, ServerAddress adr) {
        ServerInfo serverInfo = serverInfos.get(adr);
        if (serverInfo == null) {
        	serverInfo = new ServerInfo(adr);
        	serverInfos.put(adr, serverInfo);
        }
        serverInfo.add(id);
    }

    /**
     * ServerInfo represents a client server that has interests in locally exported objects.
     * */
    protected class ServerInfo implements Runnable {
    	//the ids of the objects
        private Set<String> exportedObjects;
        //the server address of the client
        private ServerAddress remoteServerAddress;
        private Future<?> future;
        //times used for pinging
        private long lastPingId;
        private long previousPingId;
        private ScheduledFuture<?> scheduledFuture;

        public ServerInfo(ServerAddress remoteServerAddress) {
            this.exportedObjects = Collections.synchronizedSet(new HashSet<String>());
            this.remoteServerAddress = remoteServerAddress;
        }

        /**
         * Did the client ping since last check?
         * */
        public synchronized boolean isUpdated() {
            if (log.isDebugEnabled()) {
                log.debug(ServerInfo.this+": checking for update. (Previous ID: " + previousPingId + ", last ID: " + lastPingId + ").");
            }
            return previousPingId != lastPingId;
        }

        //the client server address
        public ServerAddress getRemoteEndPoint() {
            return remoteServerAddress;
        }

        //this client has a new interest
        public void add(String id) {
            if (exportedObjects.size() == 0) {
            	//if first object, trigger the periodical check
                scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this, serverConfiguration.getServerGCTimeout(), serverConfiguration.getServerGCTimeout(), TimeUnit.MILLISECONDS);
            }
            if (!exportedObjects.contains(id)) {
            	//if first time added, notify the listener
                exportedObjects.add(id);
                clientInterestListener.addInterest(remoteServerAddress,id);
            }
        }

        //remove the object as an interest
        public void remove(String id) {
        	boolean cont=false;
            int size;
            synchronized (this) {
            	cont = exportedObjects.remove(id);
                size = exportedObjects.size();
            }
            if (cont) {
                if (size == 0) {
                	//if it was the last one, remove the ServerInfo
                    stop();
                }
            }
        }

        //remove the ServerInfo from ServerGC. The method is called when client has no more interest (died or stopped)
        public void stop() {
            String[] ids;
            synchronized (this) {
                ids= exportedObjects.toArray(new String[0]);
                serverInfos.remove(remoteServerAddress);
                if (future != null) {
                    future.cancel(false);
                }
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
            if (ids != null) {
            	clientInterestListener.removeInterest(remoteServerAddress, ids);
            }
        }

        //the client pinged, update the timestamps
        public synchronized void update() {
            long tmpOldValue = previousPingId;
            previousPingId = lastPingId;
            if (log.isDebugEnabled()) {
                log.debug(ServerInfo.this+": updated previous ping ID to " + previousPingId + " (was: " + tmpOldValue + ").");
            }
        }

        //the client is pinging
        public synchronized void ping(boolean[] returnPing, String[] ids) {
            lastPingId = lastPingId + 1;
            if (log.isDebugEnabled()) {
                log.debug(ServerInfo.this+": received ping (last ID: " + lastPingId + ").");
            }
            for (int i = 0; i < ids.length; i++) {
                if (exportedObjects.contains(ids[i])) {
                    returnPing[i] = true;
                }
            }
        }

        /**
         * Periodically called to check if the client has pinged.
         * */
        @Override
        public synchronized void run() {

            if (future==null || future.isDone()) {
                future = executorService.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        String oldName = Thread.currentThread().getName();
                        try {
                            Thread.currentThread().setName("ServerGC Thread - " + ServerInfo.this);
                            if (isUpdated()) {
                                update();
                            } else {
                                if (log.isDebugEnabled()) {
                                	log.debug(ServerInfo.this+": GC was not pinged on time by client.");
                                }
                                stop();
                            }
                        } finally {
                            Thread.currentThread().setName(oldName);
                        }
                        return null;
                    }
                });
            }
        }

        @Override
        public String toString() {
            return "ServerGC[RemoteAddress: " + remoteServerAddress + ", # of exportedObjects: " + exportedObjects.size() + "]";
        }
	}
}
