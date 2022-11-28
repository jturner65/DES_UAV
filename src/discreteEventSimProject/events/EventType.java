package discreteEventSimProject.events;

import java.util.HashMap;
import java.util.Map;

//types of events that can be gnerated
public enum EventType {
	ArriveResource(0), LeaveResource(1), EnterQueue(2), ConsumerWaiting(3);		
	private int value; 
	private static Map<Integer, EventType> map = new HashMap<Integer, EventType>(); 
	static { for (EventType enumV : EventType.values()) { map.put(enumV.value, enumV);}}
	private EventType(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static EventType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum	
};
