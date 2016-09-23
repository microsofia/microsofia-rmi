package microsofia.rmi.gc;

import microsofia.rmi.ServerAddress;

public interface IClientGCListener {//TODO remove, useless

	public void objectAlive(ServerAddress serverAddress, String[] objects);
    
    public void objectMaybeDead(ServerAddress serverAddress, String[] objects, Throwable throwable);
    
    public void objectDead(ServerAddress serverAddress, String[] objects,  Throwable throwable);

    public void objectDead(ServerAddress serverAddress, String[] objects);

}
