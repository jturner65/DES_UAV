package discreteEventSimProject.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Types of events that can be generated
 * @author John Turner
 */
public enum EventType {
	ArriveResource(0), LeaveResource(1), EnterQueue(2), ConsumerWaiting(3);		
	private int value; 
	private static final String[] _typeExplanation = new String[] {
		"Consumer has arrived at Resource",
		"Consumer is leaving Resource",
		"Consumer has entered a Queue",
		"Consumer is waiting for Resource to be available"		
	};
	private static final String[] _typeName = new String[] {"ArriveAtResource","LeaveResource","EnterQueue","ConsumerWaiting"};
	public static String[] getListOfTypes() {return _typeName;}	
	private static Map<Integer, EventType> map = new HashMap<Integer, EventType>(); 
	static { for (EventType enumV : EventType.values()) { map.put(enumV.value, enumV);}}
	private EventType(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static EventType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum	
	@Override
    public String toString() { return ""+this.name()+":"+_typeExplanation[value]; }	
    public String toStrBrf() { return ""+_typeExplanation[value]; }	
 };
