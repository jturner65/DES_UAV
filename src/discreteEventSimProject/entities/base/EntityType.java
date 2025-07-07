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
    Consumer, Resource, Queue, Group;    
    private static final String[] _typeExplanation = new String[]{
        "Consumes resource",
        "Some resource to be consumed",
        "FIFO Ordered Collection",
        "Unordered Collection"
    };
    private static final String[] _typeName = new String[]{"Consumer","Resource","Queue","Group"};
    public static String[] getListOfTypes() {return _typeName;}    
    private static Map<Integer, EntityType> map = new HashMap<Integer, EntityType>(); 
    static { for (EntityType enumV : EntityType.values()) { map.put(enumV.ordinal(), enumV);}}
    public int getOrdinal() {return ordinal();}     
    public static EntityType getEnumByIndex(int idx){return map.get(idx);}
    public static EntityType getEnumFromValue(int idx){return map.get(idx);}
    public static int numVals(){return map.size();}                        //get # of values in enum    
    @Override
    public String toString() { return ""+this.name()+":"+_typeExplanation[ordinal()]; }    
    public String toStrBrf() { return ""+_typeExplanation[ordinal()]; }    
 };
