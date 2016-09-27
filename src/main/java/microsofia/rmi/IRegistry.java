package microsofia.rmi;

/**
 * The remote interface of the object registry containing all the exported objects of a server.
 * It can be seen as a map which key is the id of the exported object and the value is the exported object.
 * 
 * */
public interface IRegistry {

	/**
	 * Returns all the ids of the exported objects.
	 * 
	 * @return the ids of the exported objects
	 * */
	public String[] getIds();
	
	/**
	 * Return a proxy to an exported object for a given id.
	 * If the object is not found, it returns a null object.
	 * 
	 * @return a proxy to an exported object
	 * */
	public <T> T getObject(Class<T> c, String id);

	/**
	 * Return a proxy to an exported object for a given id
	 * If the object is not found, it returns a null object.
	 * 
	 * @return a proxy to an exported object
	 * */
	public Object getObject(String id);
}
