package microsofia.rmi.impl.invocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches for every class, a hashcode of every method.
 * */
public class ClassesMetada {
	//by class, keeps a structure ClassMetadata
	private Map<Class<?>,ClassMetadata> metadas;
	
	public ClassesMetada(){
		metadas=new HashMap<>();
	}
	
	/**
	 * Returns for a method in the class its hashcode
	 * 
	 * @param c the class that contains the method
	 * @param m the method
	 * @return the hashcode of the method m
	 * */
	public synchronized int getHashCode(Class<?> c,Method m){
		ClassMetadata met=metadas.get(c);
		if (met==null){
			met=new ClassMetadata(c);
			metadas.put(c, met);
		}
		return met.hashCode(m);
	}
	
	/**
	 * Returns for a given hashcode, the method that corresponds to it in a class
	 * 
	 * @param c the class that contains the method we are looking for
	 * @param h the hashcode of the method
	 * @return the method that has h as hashcode
	 * */
	public synchronized Method getMethod(Class<?> c,int h){
		ClassMetadata met=metadas.get(c);
		if (met==null){
			met=new ClassMetadata(c);
			metadas.put(c, met);
		}
		Method m=met.getMethod(h);
		if (m==null){
			throw new IllegalStateException("Method with hashcode "+h+" not found in class "+c);
		}
		return m;
	}
	
	/**
	 * A structure that keep for every method a double link with its hashcode.
	 * */
	private static class ClassMetadata{
		//the method by hashcode
		private Map<Integer,Method> methods;
		//the hashcode by method
		private Map<Method, Integer> hashCodes;
		
		ClassMetadata(Class<?> c){
			methods=new HashMap<>();
			hashCodes=new HashMap<>();
			for (Method m : c.getDeclaredMethods()){
				int h=getHashCode(m);
				methods.put(h, m);
				hashCodes.put(m, h);
			}
		}

		//computes the hashcode which is simply the hashcode of the string
		//representing its signature
		private int getHashCode(Method m){
			String s=m.getName();
			if (m.getParameterTypes()!=null){
				for (Class<?> c : m.getParameterTypes()){
					s+=c.getName();
				}
			}
			return s.hashCode();
		}

		//returns the hashcode of a method
		public int hashCode(Method m){
			return hashCodes.get(m);
		}
		
		//returns a method for a given hashcode
		public Method getMethod(int i){
			return methods.get(i);
		}
	}
}
