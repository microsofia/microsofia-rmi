package microsofia.rmi.impl;

import com.google.inject.Injector;

/**
 * Used to inject a Guice Injector.
 * */
public interface IInjectorProvider {

	public Injector get();
}
