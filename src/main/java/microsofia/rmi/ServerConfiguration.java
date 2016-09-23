package microsofia.rmi;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ServerConfiguration {
    protected static final long GC_TIMEOUT 		 = 10000;//TODO 20 * 60000;
    protected static final long GCCLIENT_TIMEOUT = 1000; //TODO 2 * 60000;
    protected static final int GCCLIENT_EXCEPTION_THRESHOLD = 10;
	private GenericObjectPoolConfig clientConnectionsConfig;
	private Long gcTimeout;
    private Long gcClientTimeout;
    private Integer gcClientExceptionThreshold;

    public ServerConfiguration(){
		clientConnectionsConfig=new GenericObjectPoolConfig();
    }
    
    public GenericObjectPoolConfig getClientConnectionsConfig(){
		return clientConnectionsConfig;
	}

    public long getGcTimeout() {
        if (gcTimeout == null) {
            gcTimeout = GC_TIMEOUT;
        }
        return gcTimeout;
    }

    public void setGcTimeout(long gcTimeout) {
        this.gcTimeout = gcTimeout;
    }

    public long getGcClientTimeout() {
        if (gcClientTimeout == null) {
            gcClientTimeout = GCCLIENT_TIMEOUT;
        }
        return gcClientTimeout;
    }

    public void setGcClientTimeout(long gcClientTimeout) {
        this.gcClientTimeout = gcClientTimeout;
    }
    
    public int getGcClientExceptionThreshold() {
        if (gcClientExceptionThreshold == null) {
            gcClientExceptionThreshold = GCCLIENT_EXCEPTION_THRESHOLD;
        }
        return gcClientExceptionThreshold;
    }

    public void setGcClientExceptionThreshold(int gcClientExceptionThreshold) {
        this.gcClientExceptionThreshold = gcClientExceptionThreshold;
    }
}
