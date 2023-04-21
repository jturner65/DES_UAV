package discreteEventSimProject.events;

import discreteEventSimProject.entities.consumers.UAV_Team;
import discreteEventSimProject.entities.resources.base.Base_Resource;

/**
 * an event/message for the discrete event sim, sortable on timestamp
 * @author john
 *
 */
public class DES_Event implements Comparable<DES_Event> {
	public int ID;
	private static int IDcount = 0;
	/**
	 * timestamp is in milliseconds from beginning of simulation : event's timestamp set on construction
	 */
	private long timeStamp;
	/**
	 * event name
	 */
	public final String name;
	/**
	 * consumer entity attached to this event
	 */
	public final UAV_Team consumer;
	/**
	 * resource entity this event is attached to - the consumer entity will use this resource; parent is the resource that generated this event
	 */
	public final Base_Resource resource, parent;
	/**
	 * type of event
	 */
	public final DES_EventType type;

	public DES_Event(long _ts, DES_EventType _type, UAV_Team _c_ent, Base_Resource _r_ent, Base_Resource _p_ent) {
		ID = IDcount++;
		timeStamp = _ts;		
		type = _type;
		consumer=_c_ent;
		resource = _r_ent;
		parent = (_p_ent == null ? _r_ent : _p_ent);
		String entName;
		if((consumer == null) &&  (resource==null) && (parent==null)){
			entName = "null_entity_Test_Event";
		} else {
			entName = ""+ ((consumer == null) ? "null_consumer" : consumer.name) + "_"+((resource==null) ? "null_resource" : resource.name);
		}
		name =entName+"_"+type+"Req_"+timeStamp;
	}//ctor
		
	/**
	 * set a new time stamp for this even - DO NOT PERFORM ON EVENTS STILL QUEUED
	 * @param _ts
	 */
	public void setNewTimestamp(long _ts) {timeStamp = _ts;	}
	/**
	 * return timestamp (timestamp is set during ctor)
	 * @return
	 */
	public long getTimestamp() {return timeStamp;}	
	
	/**
	 * compare on timeStamp
	 */
	@Override
	public int compareTo(DES_Event otr) {
		if(otr == null) {System.out.println("Error : otr is null : this :  " + this.toString()); return 0;}
		if(this.timeStamp == otr.timeStamp) {return 0;}
		return (this.timeStamp > otr.timeStamp) ? 1 : -1;
	}
	
	/**
	 * just return name, time and timestamp
	 * @return
	 */
	public String toStrBrf() {
		return "Timestamp : " + timeStamp +  " | Name : " + name + " | Event Type : " + type;
	}
	
	public String toString() {
		String res = toStrBrf()+ " | ID : " + ID;
		res += (consumer == null) ? " | Null Consumer" : " | Consumer : "+ consumer.name ;
		res += (resource == null) ? " | Null Resource" : " | Resource : " + resource.name;
		return res;
	}

}//myEvent
