package microsofia.rmi.gc;

import microsofia.rmi.ServerAddress;

public interface IClientGC {
	public ServerAddress[] getServerAddress() throws Exception;
}
