package discreteEventSimProject.entities.base;

import java.util.HashMap;
import java.util.Map;

/**
 * Types of possible entity roles that may be filled - multiple roles may be filled (for instance, resource-queue is an assembly line)
 * Defined as per Birta ABCMod
 * @author John Turner
 *
 */
public enum EntityType {
	Consumer(0), Resource(1), Queue(2), Group(3);		
	private int value; 
	private static final String[] _typeExplanation = new String[] {
		"Consumes resource",
		"Some resource to be consumed",
		"FIFO Ordered Collection",
		"Unordered Collection"
	};
	private static final String[] _typeName = new String[] {"Consumer","Resource","Queue","Group"};
	public static String[] getListOfTypes() {return _typeName;}	
	private static Map<Integer, EntityType> map = new HashMap<Integer, EntityType>(); 
	static { for (EntityType enumV : EntityType.values()) { map.put(enumV.value, enumV);}}
	private EntityType(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static EntityType getVal(int idx){return map.get(idx);}
	public static int numVals(){return map.size();}						//get # of values in enum	
	@Override
    public String toString() { return ""+this.name()+":"+_typeExplanation[value]; }	
    public String toStrBrf() { return ""+_typeExplanation[value]; }	
 };
