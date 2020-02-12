package pkgCS6730Project1.entities;

import java.util.concurrent.*;

import base_UI_Objects.my_procApplet;
import base_Math_Objects.vectorObjs.floats.myPointf;
import pkgCS6730Project1.DESSimWindow;
import pkgCS6730Project1.mySimulator;
import pkgCS6730Project1.entities.base.EntityType;
import pkgCS6730Project1.entities.base.myEntity;
import pkgCS6730Project1.events.myEvent;

/**
 * class containing functionality shared by all resources in UAV DES sim
 * @author john
 */

public abstract class myUAVResource extends myEntity {
	//timestamp when currently executing event is complete
	public long taskCompleteTS;	
	/////////
	//resource(s) calling this resource	- key just should be unique, order is irrelevant
	protected ConcurrentSkipListMap<Float, myUAVResource> parentResources;
	//resources downstream from this resource - key is probability of resource being chosen, if more than one
	protected ConcurrentSkipListMap<Float, myUAVResource> childResources;	
	/////////
	//entity flags structure idxs
	public static final int 
			dbgEntityIDX  = 0,
			reqValsSet = 1,					//all required components of this entity are initialized
			taskInUseIDX  = 2,				//this task is being performed/occupied
			taskIsFullIDX  = 3,				//task cannot accept any more teams
			teamIsWaitingIDX = 4;			//parent queue has a team waiting
	public int numEntityFlags = 5;
	
	//radius of rendered task zone sphere and transit lane cylinders
	public float rad = 30.0f;

	public myUAVResource(mySimulator _sim, String _name, myPointf _loc, EntityType[] _types, float _rad) {
		super( _sim, _name, _loc, _types);
		rad=_rad;
		//move about 1/2 length of name to left, to center label over center of transit lane
		labelVals = new float[] {-name.length() * 3.0f, 1.1f*_rad, 0};
	}//myUAVResource ctor

	//called by super at end of super ctor
	protected void initEntity() {
		//initialize the flags for this entity
		initEntityFlags(numEntityFlags);
		//other resouce init stuff ... 
		childResources = new ConcurrentSkipListMap<Float, myUAVResource>();
		parentResources = new ConcurrentSkipListMap<Float, myUAVResource>();		
	}//initEntity

	
	public void addChild(float _key, myUAVResource _c) {childResources.put(_key, _c);}
	//key doesn't matter for parentResources
	public void addParent(float _key, myUAVResource _p) {parentResources.put(_key, _p);}	

	//set flags true or false
	public void setEntityFlags(int idx, boolean val) {
		boolean curVal = getEntityFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		entityFlags[flIDX] = (val ?  entityFlags[flIDX] | mask : entityFlags[flIDX] & ~mask);
		switch(idx){
			case dbgEntityIDX 			: {	//debugging
				break;}
			case reqValsSet				: { // all required values of this entity are set				
				break;}
			case taskInUseIDX			: {		//task is currently in use				
				break;}
			case taskIsFullIDX			: {				
				break;}
			case teamIsWaitingIDX		:{				
				break;}
		}		
	}//setEntityFlags
	
	//add arriving team to local queue - if team already in queue at location, move key back 1 millisecond
	//return time added to queue
	protected long addToLocalQueue(ConcurrentSkipListMap<Long, myUAVTeam> q, long timeProc, myUAVTeam team) {
		myUAVTeam tmp = null;
		while (tmp == null) {//move add time to later time, to avoid collision
			//sim.dispOutput("Note : collision in adding to queue in " + name + " Transit lane @ " + timeProc);
			tmp = q.get(timeProc);
			if(tmp != null) {++timeProc;}
		}
		q.put(timeProc, team);	
		return timeProc;
	}//addToQueue
	
	public abstract void finalInit();
//	//put in this function all variables that need to be cleared/remade if resource is reinitialized/reset
//	protected abstract void resetResource();
	public abstract myEvent arriveAtRes(myEvent ev);
	public abstract myEvent leaveRes(myEvent ev);
	
	@Override
	public void drawEntity(my_procApplet pa, DESSimWindow win, float delT, boolean drawMe, boolean drawLbls) {
		pa.pushMatrix();pa.pushStyle();
		//draw resource-based instance-specific stuff
		drawEntityPriv(pa, drawMe);
		
		if(drawLbls) {dispEntityLabel(pa, win);		}
		pa.popStyle();pa.popMatrix();		
	}
	protected abstract void drawEntityPriv(my_procApplet pa, boolean drawMe);
	
	public String toString(){
		String res = "Resource : "  + super.toString();
		return res;
	}

}//myUAVResource




