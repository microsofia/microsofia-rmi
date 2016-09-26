package microsofia.rmi;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * The configuration of the server. This object should be used in order to configure the server, before its creation.
 * Once it is created, then it is too late.
 * */
public class ServerConfiguration {
    protected static final long SERVER_GC_TIMEOUT 		 	= 10000;//TODO 20 * 60000;
    protected static final long CLIENT_GC_PERIOD 			= 1000; //TODO 2 * 60000;
    protected static final int CLIENT_GC_EXCEPTION_THRESHOLD = 10;
	private GenericObjectPoolConfig clientConnectionsConfig;
	private Long serverGCTimeout;
    private Long clientGCPeriod;
    private Integer clientGCExceptionThreshold;

    public ServerConfiguration(){
		clientConnectionsConfig=new GenericObjectPoolConfig();
    }
    
    /**
     * Configuration object of the pool containing the clients channels.
     * */
    public GenericObjectPoolConfig getClientConnectionsConfig(){
		return clientConnectionsConfig;
	}

    /**
     * Time out used by the server GC in order to check if the client is still alive and interested in its exported object.
     * Default is 20 mn. If the client doesn't ping between that period, the server will consider that the client is dead and
     * it will notify the registered listeners.
     * 
     * */
    public long getServerGCTimeout() {
        if (serverGCTimeout == null) {
            serverGCTimeout = SERVER_GC_TIMEOUT;
        }
        return serverGCTimeout;
    }

    public void setServerGCTimeout(long t) {
        this.serverGCTimeout = t;
    }

    /**
     * Period used by the client GC in order to ping the server and inform him of the exported objects he is interested in.
     * Default value is 2mn
     * */
    public long getClientGCPeriod() {
        if (clientGCPeriod == null) {
            clientGCPeriod = CLIENT_GC_PERIOD;
        }
        return clientGCPeriod;
    }

    public void setClientGCPeriod(long t) {
        this.clientGCPeriod = t;
    }
    
    /**
     * Threshold used by client GC before considering that server is dead. Default value is 10.
     * */
    public int getClientGCExceptionThreshold() {
        if (clientGCExceptionThreshold == null) {
            clientGCExceptionThreshold = CLIENT_GC_EXCEPTION_THRESHOLD;
        }
        return clientGCExceptionThreshold;
    }

    public void setClientGCExceptionThreshold(int t) {
        this.clientGCExceptionThreshold = t;
    }
}
