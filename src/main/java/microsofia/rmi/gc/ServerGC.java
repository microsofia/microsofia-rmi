package microsofia.rmi.gc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import microsofia.rmi.Registry;
import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerConfiguration;

public class ServerGC implements IServerGC{
	private static Log log = LogFactory.getLog(ServerGC.class);
	private Server server;
	private Registry registry;
	private ServerConfiguration serverConfiguration;
	private Map<ServerAddress, ServerInfo> serverInfos;

    public ServerGC(Server server,Registry registry, ServerConfiguration serverConfiguration) {
    	this.server=server;
    	this.registry=registry;
        this.serverConfiguration = serverConfiguration;
        this.serverInfos = new Hashtable<ServerAddress, ServerInfo>();
    }

    @Override
    public synchronized ServerAddress[] getExportedServerAddress() {
        return serverInfos.keySet().toArray(new ServerAddress[0]);
    }

    @Override
    public boolean[] ping(ServerAddress adr, String[] ids) throws Throwable {
        boolean[] returnPing = new boolean[ids.length];
        ServerInfo info= serverInfos.get(adr);
        if (info != null) {
            info.ping(returnPing, ids);
        }
        return returnPing;
    }

    public synchronized void export(Set<String> objIds, ServerAddress adr) {
        for (String id : objIds) {
            export(id, adr);
        }
    }

    public void unexport(ServerAddress adr) {
        ServerInfo serverInfo;
        synchronized (this) {
            serverInfo = serverInfos.get(adr);
        }
        if (serverInfo != null) {
            serverInfo.unexport();
        }
    }

    public void unexport() {
        ServerInfo[] infos = null;
        synchronized (this) {
            infos = serverInfos.values().toArray(new ServerInfo[0]);
        }
        for (ServerInfo epi : infos) {
            epi.unexport();
        }
    }

    @Override
    public void serverDead(ServerAddress adr) throws Throwable {
        unexport(adr);
    }

    private void export(String id, ServerAddress adr) {
        ServerInfo serverInfo = serverInfos.get(adr);
        if (serverInfo == null) {
        	serverInfo = new ServerInfo(adr);
        	serverInfos.put(adr, serverInfo);
        }
        serverInfo.export(id);
    }

    protected class ServerInfo implements Runnable {
        private Set<String> exportedObjects;
        private ServerAddress remoteServerAddress;
        private AtomicBoolean isRunning;        
        private Future<?> future;
        private long lastPingId;
        private long previousPingId;
        private ScheduledFuture<?> scheduledFuture;

        public ServerInfo(ServerAddress remoteServerAddress) {
            this.exportedObjects = Collections.synchronizedSet(new HashSet<String>());
            this.remoteServerAddress = remoteServerAddress;
            this.isRunning = new AtomicBoolean();
        }

        public synchronized boolean isUpdated() {
            if (log.isDebugEnabled()) {
                log.debug(ServerInfo.this+": checking for update. (Previous ID: " + previousPingId + ", last ID: " + lastPingId + ").");
            }
            return previousPingId != lastPingId;
        }

        public ServerAddress getRemoteEndPoint() {
            return remoteServerAddress;
        }

        public void export(String id) {
            if (exportedObjects.size() == 0) {
                scheduledFuture = server.getScheduledExecutorService().scheduleAtFixedRate(this, serverConfiguration.getGcTimeout(), serverConfiguration.getGcTimeout(), TimeUnit.MILLISECONDS);
            }
            if (!exportedObjects.contains(id)) {
                exportedObjects.add(id);
                registry.fireServerDisconnected(remoteServerAddress,id);
            }
        }

        public void unexport(String id) {
        	boolean cont=false;
            int size;
            synchronized (this) {
            	cont = exportedObjects.remove(id);
                size = exportedObjects.size();
            }
            if (cont) {
                if (size == 0) {
                    unexport();
                }
            }
        }

        public void unexport() {
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
                registry.fireServerDisconnected(remoteServerAddress,ids);
            }
        }

        public synchronized void update() {
            long tmpOldValue = previousPingId;
            previousPingId = lastPingId;
            if (log.isDebugEnabled()) {
                log.debug(ServerInfo.this+": updated previous ping ID to " + previousPingId + " (was: " + tmpOldValue + ").");
            }
        }

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

        @Override
        public void run() {

            if (!isRunning.get()) {
                future = server.getExecutorService().submit(new Callable<Void>() {
                            @Override
                            public Void call() {
                                String oldName = Thread.currentThread().getName();
                                try {
                                    isRunning.set(true);
                                    Thread.currentThread().setName("ServerGC Thread - " + ServerInfo.this);
                                    if (isUpdated()) {
                                        update();
                                    } else {
                                        if (log.isDebugEnabled()) {
                                        	log.debug(ServerInfo.this+": GC was not pinged on time by client.");
                                        }
                                        unexport();
                                    }
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
            return "ServerGC[RemoteAddress: " + remoteServerAddress + ", # of exportedObjects: " + exportedObjects.size() + "]";
        }
	}
}
