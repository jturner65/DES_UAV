package pkgCS6730Project1;

import java.util.Map.Entry;
import java.util.concurrent.*;

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
	
//	//all functionality to set/reset entity variables that change during sim without requiring entire entity to be remade
//	@Override
//	protected void reInitEntityPriv() {
//		boolean isMade = getEntityFlags(reqValsSet);
//		if(!isMade) {//this should never happen, means trying to reset an object that hasn't been fully initialized			
//		}
//		//reset all state flags
//		initEntityFlags(numEntityFlags);
//		setEntityFlags(reqValsSet,true);		
//		resetResource();
//	}//reInitEntityPriv	
	
	
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
	public void drawEntity(UAV_DESSim pa, float delT, boolean drawMe, boolean drawLbls) {
		pa.pushMatrix();pa.pushStyle();
		//draw resource-based instance-specific stuff
		drawEntityPriv(pa, drawMe);
		
		if(drawLbls) {dispEntityLabel(pa);		}
		pa.popStyle();pa.popMatrix();		
	}
	protected abstract void drawEntityPriv(UAV_DESSim pa, boolean drawMe);
	
	public String toString(){
		String res = "Resource : "  + super.toString();
		return res;
	}

}//myUAVResource

/**
 * class holding a task resource
 * @author john
 *
 */
class myUAVTask extends myUAVResource{	
	//all task related values held in taskDesc object for reporting purposes - any changes to values require this task to be rebuilt
	public taskDesc td;
	//all teams currently being served, if group task
	protected ConcurrentSkipListMap<String, myUAVTeam> teamsBeingServed;

	public myUAVTask(mySimulator _sim, taskDesc _td) {
		super(_sim, _td.name, _td.taskLoc, (_td.isGroupTask ?  new EntityType[] {EntityType.Resource, EntityType.Group} : new EntityType[] {EntityType.Resource}),_td.rad);
		td = _td;
		lblColors = new int[] {150,0,20,255};
		//consumer = null;
		finalInit();
	}//ctor

	//finalize initialization of this resource, after all connections are set
	@Override
	public void finalInit() {
		teamsBeingServed = new ConcurrentSkipListMap<String, myUAVTeam>();
		setEntityFlags(reqValsSet, true);		
	}//finalInit
	//all the initialization/reinit needed to reset this resource without remaking it, and only what is needed to reset resource
//	@Override
//	protected void resetResource() {
//		teamsBeingServed = new ConcurrentSkipListMap<String, myUAVTeam>();
//	}//

		
	//generate time for this event to complete and returns event to perform when calling task is done 
	//timeProc is the "now" time that the arrival event was processed - time since beginning of simulation
	//team is consumer UAV team in event
	//ev is event		
	private myEvent _arriveAtRes(long timeProc, myUAVTeam team, myEvent ev) {
		long cmpTime = td.getCompletionTime();
		//inform team for metric recording
		team.addTimeInTask(name, cmpTime);
		taskCompleteTS = timeProc + cmpTime;
		//add time to complete
		timeVals[ttlRunTime] += cmpTime;
		//make new event at timeProc + compTime from now and return
		return new myEvent(taskCompleteTS, EventType.LeaveResource, team, this, this);		
	}//_arriveAtRes
	
	//returns event to perform when this task is done - consumer will wait at task while this is performed
	//timeProc is the "now" time that the arrival event was processed - time since beginning of simulation
	//team is consumer UAV team in event
	//ev is event
	private myEvent arriveAtResSingle(long timeProc, myUAVTeam team, myEvent ev) {
		myEvent res = null;	
		//when uav team arrives, check if occupied or not
		if(getEntityFlags(taskIsFullIDX)) {//task is currently executing - any team arriving needs to come back later, so modify event to be after current task finishes
			//sim.dispOutput("myUAVTask::arriveAtResSingle : Collision with occupied task :  " + ev.toString());
			//			setEntityFlags(taskInUseIDX, true) <- should be superflous
			//nextEventTS is timestamp when currently processing task will be complete			
			res = new myEvent(taskCompleteTS + 1, ev.type, ev.consumer, ev.resource, ev.parent);			
		} else {
			setEntityFlags(taskIsFullIDX, true);
			setEntityFlags(taskInUseIDX, true);
			//consumer = team;
			res = _arriveAtRes(timeProc, team, ev);
		}
		return res;
	}//arriveAtTaskSingle
	
	//doesn't block so no checking of task in use
	private myEvent arriveAtResGroup(long timeProc, myUAVTeam team, myEvent ev) {
		setEntityFlags(taskInUseIDX, true);
		for (myUAVResource parent : parentResources.values()) {
			sim.addEvent(new myEvent(timeProc, EventType.LeaveResource, team, parent, this));
		}			
		//generate time for this event to complete		
		teamsBeingServed.put(team.name, team);
		myEvent res = _arriveAtRes(timeProc, team, ev);
		return res;
	}//arriveAtTask
	
	@Override
	//doesn't block so no checking of task in use
	public myEvent arriveAtRes(myEvent ev) {
		if(!td.isGroupTask) {
			return arriveAtResSingle(ev.getTimestamp(), ev.consumer, ev);
		} else {
			return arriveAtResGroup(ev.getTimestamp(), ev.consumer, ev);
		}
	}//arriveAtTask	
	
	//return an event requesting the first UAV with appropriate (lowest) timestamp from one of the parents of this task
	private myEvent getFirstParentUAV(long timeNow) {
		long minTS = Long.MAX_VALUE-1;
		myUAVResource minP = null;
		for (myUAVResource p : parentResources.values()) {
			long tsVal = ((myUAVTransitLane)p).getFirstUAVTeamTime();
			if(tsVal < minTS) {minTS = tsVal;    minP = p;}
		}
		if(minTS > timeNow) {return null;}//if least time is in the future - shouldn't be possible
		return new myEvent(timeNow,EventType.LeaveResource, null, minP, this);		
	}//getFirstParentUAV
	
	//if this task is ready then will build a LeaveResource event for parent queue 
	//otherwise will set a flag so that leave resource can build an LeaveResource event for parent queue
	public myEvent consumerReady(myEvent ev) {
		long timeProc = ev.getTimestamp();
		if(getEntityFlags(taskIsFullIDX)) {//this is full, return a null event - when this task is done it will generate a "ready for input" event
			sim.dispOutput("\t" + name +" : consumerReady : " + timeProc + " | Not Ready for Consumer : Wait."); 
			return null;
		} else {//task is ready, generate immediate LeaveResource event for right now
			sim.dispOutput("\t" + name +" : consumerReady : " + timeProc + " | Ready for Consumer : Generating Leave Queue event."); 
			//find parent with lowest arrival time team and generate a "LeaveResource" event targeting it to pull the next object from it
			return getFirstParentUAV(timeProc);
		}		
	}//consumerReady
	
	//returns event used to determine where to go when leaving this task
	//timeAhead is the "now" time that the arrival event was processed
	//draws randomly for which transit lane to take, should always have lane at 0 key	
	private myEvent _leaveResEnd(long timeAhead, myUAVTeam team, myEvent ev) {		
		float _draw = ThreadLocalRandom.current().nextFloat();
		//get biggest key less than or equal to _draw - with only 1 entry, will always return same entry
		Entry<Float, myUAVResource> nextResource = childResources.floorEntry(_draw);
		if(null == nextResource) {sim.dispOutput("ERROR : _leaveResEnd : null entry as next resource (transit lane) "+ name); return null;}	
		
		//generate event that this task is available for to deque any teams in parent queue
		if(parentResources.firstEntry().getValue().name.equals(sim.holdingLane.name)) {
			team.addCompletedProcess();
			if (((myUAVTransitLane)sim.holdingLane).queueIsEmpty()) {//holding lane is empty and we're in first task
				myUAVTeam t = sim.addNewTeam(timeAhead);
				if(t != null) {//if null then no more UAVs available for teams
					sim.addEvent(new myEvent(timeAhead,EventType.ArriveResource, t, sim.holdingLane, this));
				}
			} else {
				sim.addEvent(getFirstParentUAV(ev.getTimestamp()));				
			}
		} else {//need to broadcast to all parent resources that this task is ready for a team.  
			//find parent with lowest arrival time team and generate a "leaveResource" event to pull the next object from it
			sim.addEvent(getFirstParentUAV(ev.getTimestamp()));
		}
		//increment # of teams this entity has processed
		++timeVals[ttlNumTeamsProc];
		myEvent res = new myEvent(timeAhead,EventType.ArriveResource, team, nextResource.getValue(),this);		
		return res;		
	}//_leaveResEnd
	
	//returns event used to determine where to go when leaving this task
	//timeProc is the "now" time that the arrival event was processed
	@Override
	public myEvent leaveRes( myEvent ev) {
		long timeProc = ev.getTimestamp();
		myUAVTeam team = ev.consumer;
		if(!td.isGroupTask) {		//single uav served at a time	
			//as of now, consumer is leaving, task is unoccupied and not in use	
			setTaskIsEmpty();
			//consumer = null;
		} else {
			//as of now, consumer is leaving, task is unoccupied - verify teams are the same	
			myUAVTeam _team = teamsBeingServed.remove(team.name);
			if(null == _team) {	sim.dispOutput("ERROR : leaveRes : attempting to remove team not present in Group Resource Task "+ name); return null;}		
			if(teamsBeingServed.size() == 0) {setTaskIsEmpty();}
		}		
		//time to disengage this task		
		return _leaveResEnd( timeProc, team, ev);

	}//leaveTask	
	
	protected void setTaskIsEmpty() {
		setEntityFlags(taskIsFullIDX, false);
		setEntityFlags(taskInUseIDX, false);		
	}//setTaskIsEmpty
	
	//get location to put team when entering this resource - if group resource, find random spot within sphere, otw use center
	public myPointf getTargetLoc() {
		if(!td.isGroupTask) {return loc;}
		else {	
			myPointf offset = sim.getRandPosInSphere(.75f*rad);
			return myPointf._add(offset, loc);}
	}//getTargetLoc	
	
	//put together list of values regarding this UAV team to show on screen
	@Override
	protected String[] showStatus() {
		String[] res = {
			"Name : " + name ,
			"# Teams Proc : " + ((int)timeVals[ttlNumTeamsProc]),
			"TTL Use Time : " + timeVals[ttlRunTime],
			"Opt Team Size : " + td.optUAVTeamSize,
			"STD Dev TTC : " + String.format("%4.3f", td.stdDev),
		};
		return res;
	}//showStatus
	
	@Override 
	protected void drawEntityPriv(UAV_DESSim pa, boolean drawMe) {
		if((!drawMe)&&(!getEntityFlags(taskInUseIDX))){return;}
		pa.sphereDetail(10);
		pa.noFill();
		pa.translate(loc);
		//don't draw at all if drawMe is false, unless occupied
		if(drawMe) {//always draw
			pa.setColorValStroke(getEntityFlags(taskInUseIDX) ? (td.isGroupTask ? pa.gui_TransMagenta : pa.gui_TransCyan) : pa.gui_TransGray);
			pa.sphere(rad); 
		} else if(getEntityFlags(taskInUseIDX)) {//only draw if occupied
			pa.setColorValStroke( (td.isGroupTask ? pa.gui_TransMagenta : pa.gui_TransCyan) );
			pa.sphere(rad); 
		}
	}//drawEntityPriv
		
	public String toString(){
		String res = (td.isGroupTask ? "Group Task "  : "Task ")  + super.toString();
		if(parentResources.size()  == 0) {
			res += "\nNo Parent Transit Lanes\n";			
		} else {
			res += "\nParent Transit Lanes : \n";
			for(myUAVResource tl : parentResources.values()) {res+= "\t"+tl.name+"\n";}
		}
		if(childResources.size()  == 0) {
			res += "\nNo Child Transit Lanes\n";			
		} else {
			res += "\nChild Transit Lanes and cumulative probs : \n";
			float prob = 1.0f/childResources.size();
			for(Float key : childResources.keySet()) {res+= "\tCum Prob : "+ key + " to " + (key+prob) + " Transit Lane : " +childResources.get(key).name+"\n";	}		
		}
		res += "Task Description : " + td.toString() + "\n";
		return res;
	}
}//myUAVTask 


//class representing transition from one task to another - acts also as a queue
//Transit lane always has one entry and one exit
class myUAVTransitLane extends myUAVResource{
	//uses map to hold UAV teams transitioning to next task, keyed by arrival time (i.e. this is queue of teams)
	//earliest arriving is first to complete transit and first to move to next task, if available
	private ConcurrentSkipListMap<Long, myUAVTeam> teamQ;
	
	//start and end location for this transit lane
	private myPointf stLoc, endLoc;
	
	//vector from parent resource to child resource - teams will follow this vector, also use this to render visualization
	public myVectorf travelLane;
	
	//"velocity" multiplier for traveling this lane - for most lanes this will be 1, but for the
	//holding lane it will be high so that UAVs that are finished with the final task are immediately available	
	private float laneVel;
	
	public myUAVTransitLane(mySimulator _sim, String _name, myPointf _loc, float _rad, float _laneVel) {
		super(_sim, _name, _loc, new EntityType[] {EntityType.Resource, EntityType.Queue},_rad);
		laneVel = _laneVel;
		lblColors = new int[] {0,110,20,255};
	}//myUAVTransitLane ctor
	
	//initialize interdependencies after all resources are made
	public void setTransitLaneConnections(myUAVResource _pTask, myUAVResource _cTask) {
		addParent(0,_pTask);//lane only ever has 1 parent, but parent task my have multiple lanes as children
		//each lane only ever has 1 child, child task may have multiple parent lanes
		addChild(0, _cTask);
		//finalize initialization based on what kind of resource this is
		finalInit();		
	}//finalInit

	//finalize initialization of this resource, after all connections are set
	@Override
	public void finalInit() {
		// Set transit lane vector between parent and child task - travel lanes always only have 1 of each
		if((parentResources.size() != 1) || (childResources.size() != 1)){
			sim.dispOutput("Error in myUAVTransitLane::finalInitPriv() for object : " + name + " :: Incorrect # parents/children specified for transit lane : # Parents : "+ parentResources.size() + " # children : " +childResources.size() );			
		}
		stLoc = new myVectorf(parentResources.firstEntry().getValue().loc);
		endLoc = new myVectorf(childResources.firstEntry().getValue().loc);	 
		
		travelLane = new myVectorf(stLoc, endLoc);
		//move back from end of travel lane the radius of the child task's sphere
		float lenTravelLane = (travelLane.magn - childResources.firstEntry().getValue().rad);
		//change travel lane length to be length from stLoc to right outside task's sphere
		travelLane._normalize()._mult(lenTravelLane);
		endLoc.set(myPointf._add(stLoc, travelLane));
		
		sim.dispOutput("\t" + name + " : travel lane : "+ travelLane.toString());
		//initialize map of teams in transit 
		teamQ = new  ConcurrentSkipListMap<Long, myUAVTeam>();
		
		setEntityFlags(reqValsSet, true);
	}//finalInitPriv
	
	//all the initialization/reinit needed to reset this resource without remaking it, and only what is needed to reset resource
//	@Override
//	protected void resetResource() {
//		//initialize map of teams in transit 
//		teamQ = new ConcurrentSkipListMap<Long, myUAVTeam>();		
//	}//

	//returns a specific point on the travel lane based on given starting point and passed distance to travel
	private myPointf getLocOnLane(myPointf _stLoc , float distToGo) {
		myVectorf offsetVec = new myVectorf(travelLane);		
		offsetVec._normalize()._mult(distToGo);
		//sim.dispOutput("arriveAtRes : dist to go : " + distToGo + " offset vec mag : " + offsetVec.magn + " travel lane : "  + travelLane.magn);
		return new myPointf(_stLoc, offsetVec);
	}//
	
	//return travel time, set team values so team can move down transit lane
	private long setTeamTTandDur(myUAVTeam _team, myPointf _stLoc, float _distToGo) {
		//find end location as start location + travel vector
		myPointf lclEndLoc = getLocOnLane (_stLoc, _distToGo);
		long dur = (long) (_distToGo * 1000.0f / (_team.teamSpeed*laneVel));		//meters / meters per second / 1000 == milliseconds
		_team.setTrajAndDur(_stLoc, lclEndLoc, dur);
		return dur;		
	}//setTeamTTandDur
	//return distance passed team should go to arrive at specified place in line
	private float getDistToGo(myUAVTeam team, int place) {
		//minimum distance to go is the distance that would require 1 millisecond to travel 
		float minDistToGo =  (team.teamSpeed*laneVel)/1000.0f;
		float distToGo = Math.max(travelLane.magn - (place * team.getNoflyDist()), minDistToGo);
		return distToGo;
	}
	
	//arrival is when a team has been first inserted into this transit lane, has just left previous task
	//queue doesn't care about being in use, time is always based on distance between tasks and UAV speed
	//team put in queue when arrived at queue (which is after moving through transit lane)
	//TODO teams in queue will be rendered sequentially
	@Override
	public myEvent arriveAtRes(myEvent ev) {
		long timeProc = ev.getTimestamp();
		myUAVTeam team = ev.consumer;	
		//if we are entering the holding lane, finish off the time recording for the team
		if(this.name.equals(sim.holdingLane.name)) {team.finishedProcess();		}
		setEntityFlags(taskInUseIDX, true);
		sim.dispOutput("\t" + name +" : arriveAtRes : " + timeProc + " q size : "+teamQ.size()); 
		//distance to travel is == to distance to next task - # of teams x team diameter
		//minimum distance to go is the distance that would require 1 millisecond to travel 
		float distToGo = getDistToGo(team, teamQ.size());
		long travelDur = setTeamTTandDur(team, stLoc, distToGo), 
				simTimeEnterQ = timeProc+travelDur;
		//time to in transit until arrival at Queue
		//record how much time was spent traveling
		timeVals[ttlTravelTime] += travelDur; 
		//increase value for total time in use
		timeVals[ttlRunTime] += travelDur; 
		//allow team to monitor it's own travel time - it is also recording the time it will enter the queue
		team.addTimeInTransit(name, travelDur, simTimeEnterQ);
		myEvent newEv = new myEvent(simTimeEnterQ, EventType.EnterQueue, team, this, this);
		return newEv;
	}//arriveAtRes
	
	//event generatd when task has accepted the team, so this event generates no response event
	@Override
	public myEvent leaveRes(myEvent ev) {
		if (queueIsEmpty()) { return null;}
		long timeProc = ev.getTimestamp();
		sim.dispOutput("\t" + name +" : leaveRes : " + timeProc + " q size : "+teamQ.size()); 
		long firstKey = teamQ.firstKey();		
		myUAVTeam team = teamQ.remove(firstKey);
		//increment # of teams this entity has processed
		++timeVals[ttlNumTeamsProc];

		team.moveUAVTeamToDest(((myUAVTask)childResources.firstEntry().getValue()).getTargetLoc());//childResources.firstEntry().getValue().loc);
		//team leaving queue knows how long it spent in queue
		long timeInQueue = team.leaveQueueAddQTime(timeProc);	
		//record how much time was spent in queue
		timeVals[ttlQueueTime] += timeInQueue; 
		//increase value for total time in use
		timeVals[ttlRunTime] += timeInQueue; 
		
		if(teamQ.size() == 0) {
			setEntityFlags(taskInUseIDX, false);
		} else {
		//move other teams up in queue (for visualization - setting motion target to be closer to task)
			myUAVTeam t = teamQ.firstEntry().getValue();
			float distToGo = getDistToGo(t, 0);
			myPointf newLoc = getLocOnLane (stLoc, distToGo), oldLoc;
			for(Long key : teamQ.keySet()) {
				t = teamQ.get(key);
				oldLoc = new myPointf(t.loc);
				t.moveUAVTeamToDest(newLoc);
				//set next team's new location to this team's old location
				newLoc.set(oldLoc);			
			}
		}
		//this will only be processed when child resource is unoccupied, either after an immediate check, or when child resource is finished
		myEvent newEvent = new myEvent(timeProc, EventType.ArriveResource, team, childResources.firstEntry().getValue(), this);
		return newEvent;
	}//leaveRes
	
	//handles entering and leaving the queue - move to explicit location when entering queue - once in queue, is available for arrival
	public myEvent enterQueue(myEvent ev) {
		//add team to queue with event time as key
		long timeArrive = ev.getTimestamp();
		sim.dispOutput("\t" + name + " : enterQueue : " + timeArrive + " q size : "+teamQ.size()); 
		myUAVTeam team = ev.consumer;
		//move team to position in queue, based on size - size == position in queue
		float dist2Go = getDistToGo(team, teamQ.size());
		myPointf lclEndLoc = getLocOnLane (stLoc, dist2Go);		
		team.moveUAVTeamToDest(lclEndLoc);
		//verify team not at current arrival time - increment time if any teams already in queue at same time
		myUAVTeam t = teamQ.get(timeArrive);
		while (t != null) {
			++timeArrive;
			t = teamQ.get(timeArrive);
		}
		teamQ.put(timeArrive, team);
		//make an event to enter the child task at this time
		myEvent newEvent = new myEvent(timeArrive, EventType.ConsumerWaiting, teamQ.firstEntry().getValue(), childResources.firstEntry().getValue(), this);
		return newEvent;
	}//entered queue	
//	
	//return the time of the first uav team's entry to this queue
	public long getFirstUAVTeamTime() {
		if (teamQ.size() > 0) {	return teamQ.firstKey();}
		else {		return Long.MAX_VALUE;}
	}
	
	public boolean queueIsEmpty() {return teamQ.size() == 0;}
	
	//put together list of values regarding this UAV team to show on screen
	protected String[] showStatus() {
		String[] res = {
			"Name : " + name ,
			"# Teams Proc : " + ((int)timeVals[ttlNumTeamsProc]),
			"TTL Use Time : " + timeVals[ttlRunTime]  ,
			"TTL Travel Time : " + timeVals[ttlTravelTime],
			"TTL Q Time : " + timeVals[ttlQueueTime],
		};
		return res;
	}//showStatus
	
	//transitlane-specifics
	@Override
	protected void drawEntityPriv(UAV_DESSim pa, boolean drawMe) {
		boolean it = getEntityFlags(taskInUseIDX);
		if((drawMe)||(it)){
			pa.pushMatrix();pa.pushStyle();	
			if((drawMe) && (!it)) {
				int clr = (this.laneVel > 1) ? pa.gui_Black : pa.gui_TransGray;
				pa.cylinder(stLoc,endLoc, rad, clr, clr);
			} else if (it) {
				int clr =  (this.laneVel > 1) ? pa.gui_Green : pa.gui_Yellow;
				pa.cylinder(stLoc,endLoc, rad, pa.gui_Red , clr);
			}			
			pa.popStyle();pa.popMatrix();	
		}
		pa.translate(loc);
	}//drawEntityPriv

	public static String getTLResCSV_Hdr() {return "Transit Lane Name, Teams Processed, TTL Lane Time (ms), TTL Travel Time (ms), TTL Queue Time (ms)";}
	
	public String getTLResCSV() {	return name +"," + timeVals[ttlNumTeamsProc]+"," + timeVals[ttlRunTime]+"," + timeVals[ttlTravelTime]+"," + timeVals[ttlQueueTime];}
	
	
	public String toString(){
		String res = "Transit Lane " + super.toString();
		res += "\nParent Task : \n";
		for(myUAVResource tl : parentResources.values()) {res+= "\t"+tl.name+"\n";}
		res += "\nChild Task \n";
		for(myUAVResource cl : childResources.values()) {res+= "\t"+cl.name+"\n";}	
		return res;
	}
}//myUAVTransitLane
