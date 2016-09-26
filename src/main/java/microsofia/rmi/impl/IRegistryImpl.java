package microsofia.rmi.impl;

import microsofia.rmi.IRegistry;
import microsofia.rmi.impl.invocation.ObjectAddress;

/**
 * Internal interface of the registry.
 * */
public interface IRegistryImpl extends IRegistry{

	/**
	 * Returns the object address of an object
	 * */
	public ObjectAddress getObjectAddress(Object o);

	/**
	 * Export in the registry an object with its id and its interfaces
	 * */
	public void export(String id,Object o,Class<?>[] interfaces);

	/**
	 * Shortcut method to the previous one. It exports an object with one interface and its id equal to the interface's name.
	 * */
	public void export(Object o,Class<?> interf);

	/**
	 * Unexports an object. It only removes any reference to it.
	 * */
	public void unexport(Object o);
}
