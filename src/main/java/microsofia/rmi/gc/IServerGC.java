package microsofia.rmi.gc;

import microsofia.rmi.ServerAddress;

public interface IServerGC {

	public boolean[] ping(ServerAddress adr, String[] objectId) throws Throwable;

    public void serverDead(ServerAddress adr) throws Throwable;

    public ServerAddress[] getExportedServerAddress() throws Throwable;

}
