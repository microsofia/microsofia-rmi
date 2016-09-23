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

import microsofia.rmi.Server;
import microsofia.rmi.ServerAddress;
import microsofia.rmi.ServerConfiguration;
import microsofia.rmi.invocation.ObjectAddress;

public class ClientGC implements IClientGC{
	private static Log log = LogFactory.getLog(ClientGC.class);
    private Server server;
    private IClientGCListener listener;    
    private ServerConfiguration serverConfiguration;
	private Map<ServerAddress, ServerInfo> serverInfos;

	public ClientGC(Server server, IClientGCListener listener,ServerConfiguration serverConfiguration) {
        this.server = server;
        this.listener = listener;
        this.serverConfiguration=serverConfiguration;
        this.serverInfos = new Hashtable<ServerAddress, ServerInfo>();
    }

	@Override
	public synchronized ServerAddress[] getServerAddress() throws Exception {
        return serverInfos.keySet().toArray(new ServerAddress[0]);
    }

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

    public void unexport() {
        ServerInfo[] si = null;
        synchronized (this) {
            si = serverInfos.values().toArray(new ServerInfo[0]);
        }
        for (ServerInfo i : si) {
            i.unexport();
        }
    }


    private class ServerInfo implements Runnable {
        private Future<?> future;
        private IServerGC gc;
        private int gcExceptionCount;
        private AtomicBoolean isRunning;
        private Set<String> ids;
        private ServerAddress remoteServerAddress;
        private ScheduledFuture<?> scheduledFuture;
        private Throwable throwable;

        public ServerInfo(ServerAddress remoteServerAddress) throws Exception {
            this.remoteServerAddress = remoteServerAddress;
            this.ids= Collections.synchronizedSet(new HashSet<String>());
            this.gc = server.lookup(remoteServerAddress, IServerGC.class);
            this.isRunning = new AtomicBoolean();
        }

        public void export(String id) {
            if (ids.size() == 0) {
                scheduledFuture = server.getScheduledExecutorService().scheduleAtFixedRate(this, serverConfiguration.getGcClientTimeout(),serverConfiguration.getGcClientTimeout(), TimeUnit.MILLISECONDS);
            }
            ids.add(id);
        }

        public void unexport(String id) {
            ids.remove(id);
            if (ids.size() == 0) {
                unexport();
            }
        }

        public boolean ping() {
            throwable = null;
            String[] idsArray = ids.toArray(new String[0]);
            boolean[] ping = null;
            long t1 = 0;
            try {
                t1 = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug(ServerInfo.this.toString()+": pinging.");
                }
                ping = gc.ping(server.getServerAddress(), idsArray);
            } catch (Throwable e) {
                throwable = e;
                if (log.isDebugEnabled()) {
                    log.debug(ServerInfo.this.toString()+": error while pinging.", e);
                }
                if (++gcExceptionCount >= serverConfiguration.getGcClientExceptionThreshold()) {
                    if (log.isDebugEnabled()) {
                        log.debug(ServerInfo.this.toString()+": pinging at " + remoteServerAddress + " failed after " + gcExceptionCount + " attempts. The remote address will no longer be pinged. The last received exception follows", e);
                    }
                    unexport();
                    return false;
                }
                return true;
            } finally {
                if (log.isDebugEnabled()) {
                    long rtt = System.currentTimeMillis() - t1;
                    log.debug(ServerInfo.this.toString()+": pinging RTT: " + rtt + "ms");
                }
            }
            gcExceptionCount = 0;
            for (int i = 0; i < ping.length; i++) {
                if (!ping[i]) {
                    if (log.isDebugEnabled()) {
                        log.debug(ServerInfo.this.toString()+": pinging informed that objects were unexported from " + remoteServerAddress + ", unexporting object of ID: " + idsArray[i]);
                    }
                    unexport(idsArray[i]);
                }
            }
            if (ids.size() == 0) {
                return false;
            }
            return true;
        }

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

        @Override
        public void run() {
            if (!isRunning.get()) {
                future = server.getExecutorService().submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        String oldName = Thread.currentThread().getName();
                        try {
                            isRunning.set(true);
                            Thread.currentThread().setName("ClientGC Thread - " + ServerInfo.this.toString());
                            boolean ping = ping();
                            String[] values = null;
                            synchronized (ServerInfo.this) {
                                values = ids.toArray(new String[0]);
                            }
                            if (!ping) {
                                if (gcExceptionCount == 0) {
                                    listener.objectDead(remoteServerAddress, values);
                                } else {
                                    listener.objectDead(remoteServerAddress, values, throwable);
                                }
                            } else {
                                if (gcExceptionCount == 0) {
                                    listener.objectAlive(remoteServerAddress, values);
                                } else {
                                    listener.objectMaybeDead(remoteServerAddress, values, throwable);
                                }
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
            return "ClientGC[RemoteServerAddress: " + remoteServerAddress + ", Num Exported Objects: " + ids.size() + "]";
        }
    }
}
