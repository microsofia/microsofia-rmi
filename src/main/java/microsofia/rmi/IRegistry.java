package microsofia.rmi;

public interface IRegistry {

	public String[] getIds();
	
	public <T> T getObject(Class<T> c, String id);

	public Object getObject(String id);
}
