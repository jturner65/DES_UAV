package discreteEventSimProject.entities;

import java.util.concurrent.ConcurrentSkipListMap;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.events.EventType;
import discreteEventSimProject.events.myEvent;
import discreteEventSimProject.sim.base.mySimulator;

//class representing transition from one task to another - acts also as a queue
//Transit lane always has one entry and one exit
public class myUAVTransitLane extends myUAVResource{
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
			sim.exec.dispOutput("myUAVTransitLane","finalInit","Error in myUAVTransitLane::finalInitPriv() for object : " + name + " :: Incorrect # parents/children specified for transit lane : # Parents : "+ parentResources.size() + " # children : " +childResources.size() );			
		}
		stLoc = new myVectorf(parentResources.firstEntry().getValue().loc);
		endLoc = new myVectorf(childResources.firstEntry().getValue().loc);	 
		
		travelLane = new myVectorf(stLoc, endLoc);
		//move back from end of travel lane the radius of the child task's sphere
		float lenTravelLane = (travelLane.magn - childResources.firstEntry().getValue().rad);
		//change travel lane length to be length from stLoc to right outside task's sphere
		travelLane._normalize()._mult(lenTravelLane);
		endLoc.set(myPointf._add(stLoc, travelLane));
		
		sim.exec.dispOutput("myUAVTransitLane","finalInit","\t" + name + " : travel lane : "+ travelLane.toString());
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
		long dur = (long) (_distToGo * 1000.0f / (myUAVTeam.teamSpeed*laneVel));		//meters / meters per second / 1000 == milliseconds
		_team.setTrajAndDur(_stLoc, lclEndLoc, dur);
		return dur;		
	}//setTeamTTandDur
	//return distance passed team should go to arrive at specified place in line
	private float getDistToGo(myUAVTeam team, int place) {
		//minimum distance to go is the distance that would require 1 millisecond to travel 
		float minDistToGo =  (myUAVTeam.teamSpeed*laneVel)/1000.0f;
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
		sim.exec.dispOutput("myUAVTransitLane","arriveAtRes","\t" + name +" : arriveAtRes : " + timeProc + " q size : "+teamQ.size()); 
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
		sim.exec.dispOutput("myUAVTransitLane","leaveRes","\t" + name +" : leaveRes : " + timeProc + " q size : "+teamQ.size()); 
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
		sim.exec.dispOutput("myUAVTransitLane","enterQueue","\t" + name + " : enterQueue : " + timeArrive + " q size : "+teamQ.size()); 
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
	protected void drawEntityPriv(IRenderInterface pa, boolean drawMe) {
		boolean it = getEntityFlags(taskInUseIDX);
		if((drawMe)||(it)){
			pa.pushMatState();	
			if((drawMe) && (!it)) {
				int clr = (this.laneVel > 1) ? IRenderInterface.gui_TransBlack : IRenderInterface.gui_TransGray;
				pa.drawCylinder_NoFill(stLoc,endLoc, rad, clr, clr);
			} else if (it) {
				int clr =  (this.laneVel > 1) ? IRenderInterface.gui_Green : IRenderInterface.gui_Yellow;
				pa.drawCylinder_NoFill(stLoc,endLoc, rad, IRenderInterface.gui_Red , clr);
			}			
			pa.popMatState();	
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
