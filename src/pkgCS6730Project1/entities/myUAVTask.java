package pkgCS6730Project1.entities;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.my_procApplet;
import base_Math_Objects.vectorObjs.floats.myPointf;
import pkgCS6730Project1.mySimulator;
import pkgCS6730Project1.taskDesc;
import pkgCS6730Project1.entities.base.EntityType;
import pkgCS6730Project1.events.EventType;
import pkgCS6730Project1.events.myEvent;

/**
 * class holding a task resource
 * @author john
 *
 */
public class myUAVTask extends myUAVResource{	
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
	protected void drawEntityPriv(my_procApplet pa, boolean drawMe) {
		if((!drawMe)&&(!getEntityFlags(taskInUseIDX))){return;}
		pa.sphereDetail(10);
		pa.noFill();
		pa.translate(loc);
		//don't draw at all if drawMe is false, unless occupied
		if(drawMe) {//always draw
			pa.setColorValStroke(getEntityFlags(taskInUseIDX) ? (td.isGroupTask ? IRenderInterface.gui_TransMagenta : IRenderInterface.gui_TransCyan) : IRenderInterface.gui_TransGray, 255);
			pa.sphere(rad); 
		} else if(getEntityFlags(taskInUseIDX)) {//only draw if occupied
			pa.setColorValStroke( (td.isGroupTask ? IRenderInterface.gui_TransMagenta : IRenderInterface.gui_TransCyan), 255 );
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