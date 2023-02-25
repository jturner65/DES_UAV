package discreteEventSimProject.entities.resources.base;

import java.util.concurrent.*;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import discreteEventSimProject.entities.base.Base_Entity;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.events.DES_Event;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.ui.base.Base_DESWindow;

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

	public Base_Resource(DES_Simulator _sim, String _name, myPointf _loc, EntityType[] _types, float _rad, String _descr) {
		super( _sim, _name, _loc, _types);
		rad=_rad;
		descr = _descr;
		//move about 1/2 length of name to left, to center label over center of transit lane
		labelVals = new float[] {-name.length() * 3.0f, 1.1f*_rad, 0};
	}//myUAVResource ctor

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
	public void drawEntity(IRenderInterface ri, Base_DESWindow win, float delT, boolean drawMe) {
		ri.pushMatState();
		//draw resource-based instance-specific stuff
		drawEntityPriv(ri, drawMe);		
		ri.popMatState();		
	}
	protected abstract void drawEntityPriv(IRenderInterface ri, boolean drawMe);
	
	/**
	 * Draw the description data to label this resource
	 * @param ri
	 * @param hLiteIDX
	 * @param idx
	 * @param yVal
	 * @param yOff
	 */
	public final float drawResourceDescr(IRenderInterface ri, int hLiteIDX, int idx, float xVal, float yVal, float yOff) {
		ri.setFill(0,255,255,255);
		ri.showText(""+(idx+1) +" : "+name,0, yVal);
		ri.setFill(255,255,255,255);
		_drawRsrcsDescrStr_Indiv(ri, yVal);
		yVal += yOff; 
		ri.showText("#Teams Proc: " + String.format("%3d", getTTLNumTeamsProc()), 0, yVal);
		if(hLiteIDX==idx) {ri.setFill(255,44,80,255);}
		ri.showText("TTL Task Time: " + String.format("%07d", getTTLRunTime()/1000) + " s",xVal,yVal);
		return _drawRsrcsDescrPost_Indiv(ri, xVal, yVal + yOff);
	}//drawResourceDescr
	
	/**
	 * Draw the instance-class specific description for this resource
	 * @param ri
	 * @param yVal
	 */
	protected abstract void _drawRsrcsDescrStr_Indiv(IRenderInterface ri, float yVal);
	
	/**
	 * Draw the instance-class specific description for this resource
	 * @param ri
	 * @param yValyOff
	 * @return
	 */
	protected abstract float _drawRsrcsDescrPost_Indiv(IRenderInterface ri, float xVal, float yValyOff);
	
	public String toString(){
		String res = "Resource : "  + super.toString();
		return res;
	}

}//myUAVResource




