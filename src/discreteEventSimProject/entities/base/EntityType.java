package discreteEventSimProject.entities.base;

import java.util.HashMap;
import java.util.Map;

//types of possible entity roles that may be filled - multiple roles may be filled (for instance, resource-queue is an assemply line)
public enum EntityType {
	Consumer(0), Resource(1), Queue(2), Group(3);		
	private int value; 
	private static Map<Integer, EntityType> map = new HashMap<Integer, EntityType>(); 
	static { for (EntityType enumV : EntityType.values()) { map.put(enumV.value, enumV);}}
	private EntityType(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static EntityType getVal(int idx){return map.get(idx);}
	public static int numVals(){return map.size();}						//get # of values in enum	
};
