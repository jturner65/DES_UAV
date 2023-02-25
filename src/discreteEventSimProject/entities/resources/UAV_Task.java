package discreteEventSimProject.entities.resources;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.entities.consumers.UAV_Team;
import discreteEventSimProject.entities.resources.base.Base_Resource;
import discreteEventSimProject.events.DES_EventType;
import discreteEventSimProject.events.DES_Event;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.sim.task.DES_TaskDesc;

/**
 * class holding a task resource
 * @author john
 *
 */
public class UAV_Task extends Base_Resource{	
	//all task related values held in taskDesc object for reporting purposes - any changes to values require this task to be rebuilt
	public DES_TaskDesc td;
	//all teams currently being served, if group task
	protected ConcurrentSkipListMap<String, UAV_Team> teamsBeingServed;

	public UAV_Task(DES_Simulator _sim, DES_TaskDesc _td) {
		super(_sim, _td.name, _td.taskLoc, (_td.isGroupTask ?  new EntityType[] {EntityType.Resource, EntityType.Group} : new EntityType[] {EntityType.Resource}),_td.rad, "task");
		td = _td;
		lblColors = new int[] {150,0,20,255};
		//consumer = null;
		finalInit();
	}//ctor

	/**
	 * Build a display string for this task description
	 * @return
	 */
	public final String getDispTaskDescStr() {
		return "Opt Size : " + td.optUAVTeamSize + " | Opt TTC : " 
				+ String.format("%4d",((int)(td.timeForOptToCmp/1000.0f)))
				+ " s | StdDev : "+ String.format("%3.2f",td.stdDev);
	}
	
	
	/**
	 * finalize initialization of this resource, after all connections are set
	 */
	@Override
	public void finalInit() {
		teamsBeingServed = new ConcurrentSkipListMap<String, UAV_Team>();
		setEntityFlags(reqValsSet, true);		
	}//finalInit
		
	/**
	 * generate time for this event to complete and returns event to perform when calling task is done 	
	 * @param timeProc the "now" time that the arrival event was processed - time since beginning of simulation
	 * @param team consumer UAV Team w/event
	 * @param ev event
	 * @return new event
	 */
	private DES_Event _arriveAtRes(long timeProc, UAV_Team team, DES_Event ev) {
		long cmpTime = td.getCompletionTime();
		//inform team for metric recording
		team.addTimeInTask(name, cmpTime);
		taskCompleteTS = timeProc + cmpTime;
		//add time to complete
		timeVals[ttlRunTime] += cmpTime;
		//make new event at timeProc + compTime from now and return
		return new DES_Event(taskCompleteTS, DES_EventType.LeaveResource, team, this, this);		
	}//_arriveAtRes
	
	/**
	 * returns event to perform when this task is done - consumer will wait at task while this is performed
	 * @param timeProc the "now" time that the arrival event was processed - time since beginning of simulation
	 * @param team consumer UAV Team w/event
	 * @param ev event
	 * @return
	 */
	private DES_Event arriveAtResSingle(long timeProc, UAV_Team team, DES_Event ev) {
		DES_Event res = null;	
		//when uav team arrives, check if occupied or not
		if(getEntityFlags(taskIsFullIDX)) {//task is currently executing - any team arriving needs to come back later, so modify event to be after current task finishes
			res = new DES_Event(taskCompleteTS + 1, ev.type, ev.consumer, ev.resource, ev.parent);			
		} else {
			setEntityFlags(taskIsFullIDX, true);
			setEntityFlags(taskInUseIDX, true);
			res = _arriveAtRes(timeProc, team, ev);
		}
		return res;
	}//arriveAtTaskSingle
	
	/**
	 * doesn't block so no checking of task in use
	 * @param timeProc
	 * @param team
	 * @param ev
	 * @return
	 */
	private DES_Event arriveAtResGroup(long timeProc, UAV_Team team, DES_Event ev) {
		setEntityFlags(taskInUseIDX, true);
		for (Base_Resource parent : parentResources.values()) {
			sim.addEvent(new DES_Event(timeProc, DES_EventType.LeaveResource, team, parent, this));
		}			
		//generate time for this event to complete		
		teamsBeingServed.put(team.name, team);
		DES_Event res = _arriveAtRes(timeProc, team, ev);
		return res;
	}//arriveAtTask
	
	@Override
	/**
	 * doesn't block so no checking of task in use
	 */
	public DES_Event arriveAtRes(DES_Event ev) {
		if(!td.isGroupTask) {
			return arriveAtResSingle(ev.getTimestamp(), ev.consumer, ev);
		} else {
			return arriveAtResGroup(ev.getTimestamp(), ev.consumer, ev);
		}
	}//arriveAtTask	
	
	/**
	 * return an event requesting the first UAV with appropriate (lowest) timestamp from one of the parents of this task
	 * @param timeNow
	 * @return
	 */
	private DES_Event getFirstParentUAV(long timeNow) {
		long minTS = Long.MAX_VALUE-1;
		Base_Resource minP = null;
		for (Base_Resource p : parentResources.values()) {
			long tsVal = ((UAV_TransitLane)p).getFirstUAVTeamTime();
			if(tsVal < minTS) {minTS = tsVal;    minP = p;}
		}
		if(minTS > timeNow) {return null;}//if least time is in the future - shouldn't be possible
		return new DES_Event(timeNow,DES_EventType.LeaveResource, null, minP, this);		
	}//getFirstParentUAV
	
	/**
	 * if this task is ready then will build a LeaveResource event for parent queue  
	 * otherwise will set a flag so that leave resource can build an LeaveResource event for parent queue
	 * @param ev
	 * @return
	 */
	public DES_Event consumerReady(DES_Event ev) {
		long timeProc = ev.getTimestamp();
		if(getEntityFlags(taskIsFullIDX)) {//this is full, return a null event - when this task is done it will generate a "ready for input" event
			sim.exec.dispOutput("myUAVTask","consumerReady","\t" + name +" : consumerReady : " + timeProc + " | Not Ready for Consumer : Wait."); 
			return null;
		} else {//task is ready, generate immediate LeaveResource event for right now
			sim.exec.dispOutput("myUAVTask","consumerReady","\t" + name +" : consumerReady : " + timeProc + " | Ready for Consumer : Generating Leave Queue event."); 
			//find parent with lowest arrival time team and generate a "LeaveResource" event targeting it to pull the next object from it
			return getFirstParentUAV(timeProc);
		}		
	}//consumerReady
	
	/**
	 * returns event used to determine where to go when leaving this task. 
	 * TimeAhead is the "now" time that the arrival event was processed. 
	 * Draws randomly for which transit lane to take, should always have lane at 0 key	
	 * @param timeAhead
	 * @param team
	 * @param ev
	 * @return
	 */
	private DES_Event _leaveResEnd(long timeAhead, UAV_Team team, DES_Event ev) {		
		float _draw = ThreadLocalRandom.current().nextFloat();
		//get biggest key less than or equal to _draw - with only 1 entry, will always return same entry
		Entry<Float, Base_Resource> nextResource = childResources.floorEntry(_draw);
		if(null == nextResource) {sim.exec.dispOutput("myUAVTask","_leaveResEnd","ERROR : _leaveResEnd : null entry as next resource (transit lane) "+ name); return null;}	
		
		//generate event that this task is available for to deque any teams in parent queue
		if(parentResources.firstEntry().getValue().name.equals(sim.holdingLane.name)) {
			team.addCompletedProcess();
			if (((UAV_TransitLane)sim.holdingLane).queueIsEmpty()) {//holding lane is empty and we're in first task
				UAV_Team t = sim.addNewTeam(timeAhead);
				if(t != null) {//if null then no more UAVs available for teams
					sim.addEvent(new DES_Event(timeAhead,DES_EventType.ArriveResource, t, sim.holdingLane, this));
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
		DES_Event res = new DES_Event(timeAhead,DES_EventType.ArriveResource, team, nextResource.getValue(),this);		
		return res;		
	}//_leaveResEnd
	
	/**
	 * returns event used to determine where to go when leaving this task 
	 * timeProc is the "now" time that the arrival event was processed
	 */
	@Override
	public DES_Event leaveRes( DES_Event ev) {
		long timeProc = ev.getTimestamp();
		UAV_Team team = ev.consumer;
		if(!td.isGroupTask) {		//single uav served at a time	
			//as of now, consumer is leaving, task is unoccupied and not in use	
			setTaskIsEmpty();
			//consumer = null;
		} else {
			//as of now, consumer is leaving, task is unoccupied - verify teams are the same	
			UAV_Team _team = teamsBeingServed.remove(team.name);
			if(null == _team) {	sim.exec.dispOutput("myUAVTask","leaveRes","ERROR : leaveRes : attempting to remove team not present in Group Resource Task "+ name); return null;}		
			if(teamsBeingServed.size() == 0) {setTaskIsEmpty();}
		}		
		//time to disengage this task		
		return _leaveResEnd( timeProc, team, ev);

	}//leaveTask	
	
	protected void setTaskIsEmpty() {
		setEntityFlags(taskIsFullIDX, false);
		setEntityFlags(taskInUseIDX, false);		
	}//setTaskIsEmpty
	
	/**
	 * get location to put team when entering this resource - if group resource, find random spot within sphere, otw use center
	 * @return
	 */
	public myPointf getTargetLoc() {
		if(!td.isGroupTask) {return loc;}
		else {	
			myPointf offset = sim.getRandPosInSphere(.75f*rad);
			return myPointf._add(offset, loc);}
	}//getTargetLoc	
	
	/**
	 * put together list of values regarding this UAV team to show on screen
	 */
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
	protected void drawEntityPriv(IRenderInterface ri, boolean drawMe) {
		if((!drawMe)&&(!getEntityFlags(taskInUseIDX))){return;}
		ri.setSphereDetail(10);
		ri.noFill();
		ri.translate(loc);
		//don't draw at all if drawMe is false, unless occupied
		if(drawMe) {//always draw
			ri.setColorValStroke(getEntityFlags(taskInUseIDX) ? (td.isGroupTask ? IRenderInterface.gui_TransMagenta : IRenderInterface.gui_TransCyan) : IRenderInterface.gui_TransGray, 255);
			ri.drawSphere(rad); 
		} else if(getEntityFlags(taskInUseIDX)) {//only draw if occupied
			ri.setColorValStroke( (td.isGroupTask ? IRenderInterface.gui_TransMagenta : IRenderInterface.gui_TransCyan), 255 );
			ri.drawSphere(rad); 
		}
	}//drawEntityPriv
	
	/**
	 * Draw the instance-class specific descirption for this entity
	 * @param ri
	 * @param win
	 * @param hLiteIDX
	 */
	protected final void _drawRsrcsDescrStr_Indiv(IRenderInterface ri, float yVal) {
		ri.showText(getDispTaskDescStr(),65, yVal);		
	}
	
	
	/**
	 * Draw the instance-class specific description for this resource
	 * @param ri
	 * @param yValyOff
	 * @return
	 */
	protected final float _drawRsrcsDescrPost_Indiv(IRenderInterface ri, float xVal, float yValyOff) {return yValyOff;}
		
	public String toString(){
		String res = (td.isGroupTask ? "Group Task "  : "Task ")  + super.toString();
		if(parentResources.size()  == 0) {
			res += "\nNo Parent Transit Lanes\n";			
		} else {
			res += "\nParent Transit Lanes : \n";
			for(Base_Resource tl : parentResources.values()) {res+= "\t"+tl.name+"\n";}
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