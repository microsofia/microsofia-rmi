package microsofia.rmi.invocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ClassesMetada {
	private Map<Class<?>,ClassMetadata> metadas;
	
	public ClassesMetada(){
		metadas=new HashMap<>();
	}
	
	public synchronized int getHashCode(Class<?> c,Method m){
		ClassMetadata met=metadas.get(c.getName());
		if (met==null){
			met=new ClassMetadata(c);
			metadas.put(c, met);
		}
		return met.hashCode(m);
	}
	
	public synchronized Method getMethod(Class<?> c,int h){
		ClassMetadata met=metadas.get(c.getName());
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
	
	private static class ClassMetadata{
		private Map<Integer,Method> methods;
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

		private int getHashCode(Method m){
			String s=m.getName();
			if (m.getParameterTypes()!=null){
				for (Class<?> c : m.getParameterTypes()){
					s+=c.getName();
				}
			}
			return s.hashCode();
		}

		public int hashCode(Method m){
			return hashCodes.get(m);
		}
		
		public Method getMethod(int i){
			return methods.get(i);
		}
	}
}
