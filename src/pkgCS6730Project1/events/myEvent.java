package pkgCS6730Project1.events;

import pkgCS6730Project1.entities.myUAVResource;
import pkgCS6730Project1.entities.myUAVTeam;

/**
 * an event/message, sortable on timestamp
 * @author john
 *
 */
public class myEvent implements Comparable<myEvent> {
	public int ID;
	private static int IDcount = 0;
	//timestamp is in milliseconds from beginning of simulation : event's timestamp set on construction
	private long timeStamp;
	//event name
	public final String name;
	//consumer entity attached to this event
	public final myUAVTeam consumer;
	//resource entity this event is attached to - the consumer entity will use this resource; parent is the resource that generated this event
	public final myUAVResource resource, parent;
	//type of event
	public final EventType type;

	public myEvent(long _ts, EventType _type, myUAVTeam _c_ent, myUAVResource _r_ent, myUAVResource _p_ent) {
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
		
	//set a new time stamp for this even - DO NOT PERFORM ON EVENTS STILL QUEUED
	public void setNewTimestamp(long _ts) {timeStamp = _ts;	}
	//return timestamp (timestamp is set during ctor)
	public long getTimestamp() {return timeStamp;}	
	
	//compare on timeStamp
	@Override
	public int compareTo(myEvent otr) {
		if(otr == null) {System.out.println("Error : otr is null : this :  " + this.toString()); return 0;}
		if(this.timeStamp == otr.timeStamp) {return 0;}
		return (this.timeStamp > otr.timeStamp) ? 1 : -1;
	}
	
	//just return name, time and timestamp
	public String toStrBrf() {
		String res = "Timestamp : " + timeStamp +  " | Name : " + name + " | Event Type : " + type;
		return res;
	}
	
	public String toString() {
		String res = "Timestamp : " + timeStamp + " | ID : " + ID+  " | Name : " + name;
		if((consumer == null) || (resource==null)){
			res += " | Null Consumer | Null Resource ";
		} else {
			res += " | Consumer : "+ consumer.name + " | Resource : " + resource.name;
		}
		res += " | Event Type : " + type;		
		return res;
	}

}//myEvent
