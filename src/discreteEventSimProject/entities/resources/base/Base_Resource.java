package discreteEventSimProject.entities.resources.base;

import java.util.concurrent.*;

import base_Math_Objects.vectorObjs.floats.myPointf;
import discreteEventSimProject.entities.base.Base_Entity;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.events.DES_Event;
import discreteEventSimProject.sim.base.Base_DESSimulator;

/**
 * class containing functionality shared by all resources in UAV DES sim
 * @author john
 */

public abstract class Base_Resource extends Base_Entity {
	/**
	 * timestamp when currently executing event is complete
	 */
	public long taskCompleteTS;	
	/**
	 * resource(s) calling this resource	- key just should be unique, order is irrelevant
	 */
	protected ConcurrentSkipListMap<Float, Base_Resource> parentResources;
	/**
	 * resources downstream from this resource - key is probability of resource being chosen, if more than one
	 */
	protected ConcurrentSkipListMap<Float, Base_Resource> childResources;	
	/**
	 * entity flags structure idxs
	 */
	public static final int 
			dbgEntityIDX  = 0,
			reqValsSet = 1,					//all required components of this entity are initialized
			taskInUseIDX  = 2,				//this task is being performed/occupied
			taskIsFullIDX  = 3,				//task cannot accept any more teams
			teamIsWaitingIDX = 4;			//parent queue has a team waiting
	public int numEntityFlags = 5;
	
	/**
	 * radius of rendered task zone sphere and transit lane cylinders
	 */
	protected float rad = 10.0f;
	
	/**
	 * Description of this resource, either a task or a lane
	 */
	protected final String descr;

	public Base_Resource(Base_DESSimulator _sim, String _name, myPointf _loc, EntityType[] _types, float _rad, String _descr) {
		super( _sim, _name, _loc, _types);
		rad=_rad;
		descr = _descr;
		//move about 1/2 length of name to left, to center label over center of transit lane
		labelVals = new float[] {-name.length() * 3.0f, 1.1f*_rad, 0};
	}//Base_Resource ctor

	/**
	 * called by super at end of super ctor
	 */
	protected void initEntity() {
		//initialize the flags for this entity
		initEntityFlags(numEntityFlags);
		//other resouce init stuff ... 
		childResources = new ConcurrentSkipListMap<Float, Base_Resource>();
		parentResources = new ConcurrentSkipListMap<Float, Base_Resource>();		
	}//initEntity
	
	/**
	 * Set the resource's radius
	 * @param _rad
	 */
	public void setRadius(float _rad) {rad = _rad;}
	/**
	 * Get the resources radius
	 * @return
	 */
	public float getRadius() {return rad;}
	
	public void addChild(float _key, Base_Resource _c) {childResources.put(_key, _c);}
	//key doesn't matter for parentResources
	public void addParent(float _key, Base_Resource _p) {parentResources.put(_key, _p);}	

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
	
//	/**
//	 * add arriving team to local queue - if team already in queue at location, move key back 1 millisecond. return time added to queue
//	 * @param q
//	 * @param timeProc
//	 * @param team
//	 * @return time added to queue
//	 */
//	protected long addToLocalQueue(ConcurrentSkipListMap<Long, UAV_Team> q, long timeProc, UAV_Team team) {
//		UAV_Team tmp = null;
//		while (tmp == null) {//move add time to later time, to avoid collision
//			//sim.dispOutput("Note : collision in adding to queue in " + name + " Transit lane @ " + timeProc);
//			tmp = q.get(timeProc);
//			if(tmp != null) {++timeProc;}
//		}
//		q.put(timeProc, team);	
//		return timeProc;
//	}//addToQueue
	
	public abstract void finalInit();
//	//put in this function all variables that need to be cleared/remade if resource is reinitialized/reset
//	protected abstract void resetResource();
	public abstract DES_Event arriveAtRes(DES_Event ev);
	public abstract DES_Event leaveRes(DES_Event ev);
	
	@Override
	public void drawEntity(float delT, boolean drawMe) {
		ri.pushMatState();
		//draw resource-based instance-specific stuff
		drawEntityPriv(drawMe);		
		ri.popMatState();		
	}
	protected abstract void drawEntityPriv(boolean drawMe);
	
	/**
	 * Draw the description data to label this resource
	 * @param hLiteIDX
	 * @param idx
	 * @param rtSideYVals float array holding : 
	 * 		idx 0 : start y value for text
	 * 		idx 1 : per-line y offset for grouped text
	 * 		idx 2 : per-line y offset for title-to-group text (small space)
	 * 		idx 3 : per-line y offset for text that is not grouped (slightly larger)
	 */
	public final void drawResourceDescr(int hLiteIDX, int idx, float[] rtSideYVals) {
		ri.pushMatState();
			AppMgr.showMenuTxt_LightCyan(""+(idx+1) +" : ");
			AppMgr.showMenuTxt_White(name);				
			_drawRsrcsDescrStr_Indiv(rtSideYVals);
			AppMgr.showMenuTxt_White("| #Teams Proc: ");
			AppMgr.showMenuTxt_LightCyan(String.format("%3d", getTTLNumTeamsProc()));				
		ri.popMatState();
		rtSideYVals[0] += rtSideYVals[1];		ri.translate(0.0f,rtSideYVals[1], 0.0f);
		ri.pushMatState();
		if(hLiteIDX==idx) {
			AppMgr.showOffsetText_RightSideMenu(255,44,80,255,"TTL Task Time: ");
			AppMgr.showMenuTxt_LightRed(String.format("%07d", getTTLRunTime()/1000));
			AppMgr.showOffsetText_RightSideMenu(255,44,80,255,"s");
		} else {
			AppMgr.showMenuTxt_White("TTL Task Time: ");
			AppMgr.showMenuTxt_LightRed(String.format("%07d", getTTLRunTime()/1000));
			AppMgr.showMenuTxt_White("s");			
		}
		ri.popMatState();
		rtSideYVals[0] += rtSideYVals[1];		ri.translate(0.0f,rtSideYVals[1], 0.0f);
		
		_drawRsrcsDescrPost_Indiv(rtSideYVals);
	}//drawResourceDescr
	
	/**
	 * Draw the instance-class specific description for this resource
	 * @param rtSideYVals float array holding : 
	 * 		idx 0 : start y value for text
	 * 		idx 1 : per-line y offset for grouped text
	 * 		idx 2 : per-line y offset for title-to-group text (small space)
	 * 		idx 3 : per-line y offset for text that is not grouped (slightly larger)
	 */
	protected abstract void _drawRsrcsDescrStr_Indiv(float[] rtSideYVals);
	
	/**
	 * Draw the instance-class specific description for this resource
	 * @param rtSideYVals float array holding : 
	 * 		idx 0 : start y value for text
	 * 		idx 1 : per-line y offset for grouped text
	 * 		idx 2 : per-line y offset for title-to-group text (small space)
	 * 		idx 3 : per-line y offset for text that is not grouped (slightly larger)
	 * @return
	 */
	protected abstract void _drawRsrcsDescrPost_Indiv(float[] rtSideYVals);
	
	public String toString(){
		String res = "Resource : "  + super.toString();
		return res;
	}

}//Base_Resource




