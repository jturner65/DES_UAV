package discreteEventSimProject.entities;


import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.entities.base.myEntity;
import discreteEventSimProject.renderedObjs.base.myRenderObj;
import discreteEventSimProject.sim.base.mySimulator;
import discreteEventSimProject.ui.DESSimWindow;

/**
 * class holding the graphical and simulation parameters for a UAV team entity of a certain size.  
 * @author john
 *
 */
public class myUAVTeam extends myEntity {	
	public myUAVObj[] uavTeam;
	
	public static final float rad = 5;			//radius of sphere upon which team members will be placed
	
	public static final float teamSpeed = 1;	//speed of UAV team moving from task to task m/sec (assume constant) :: 5 m/s is ~11 mph
												//if ever not made final need to update value held in myUAVTransitLane when changed (referenced for precalc)			
	public myRenderObj tmpl, sphTmpl;				//template to render boid; simplified sphere template
	
	//////////
	//metrics of team performance
	private String curJob;					//name of current resource along with current activity (traveling, in queue, working)	
	
	//////////
	//team movement variables
	public myPointf stLoc, endLoc, initLoc; 
	private myVectorf motionTraj, 						//start and end targets for team motion,  and trajectory of motion to follow 
			uavVelVec;							//uav velocity vector - along motionTraj
	private long motionDur;						//how long the motion should take to follow the trajectory in milliseconds
	private float curMotionTime;				//current time elapsed since motion began in milliseconds
	
	/////////
	//entity flags structure idxs
	public static final int 
			dbgEntityIDX  = 0,
			inTransitLane = 1;				//this team is in transit lane and should be moved
	
	public int numEntityFlags = 2;
	
	private int teamID; 
	private static int teamIncr = 0;
	private int curType;
	
	public myUAVTeam(mySimulator _sim, String _name, int _teamSize, myPointf _initLoc){
		super(_sim, _name, _initLoc, new EntityType[] {EntityType.Consumer});
		//set so always remembers where it started
		teamID = teamIncr++;
		curType = teamID % sim.NumUniqueTeams;//what type of boat to show
		initLoc = new myPointf(_initLoc);
		initTeam(_teamSize);
		labelVals = new float[] {-name.length() * 3.0f, -(2.0f*rad + 70), 0};
		lblColors = new int[] {0,0,0,255};
	}//myBoidFlock constructor
	
	//make UAV team - initialize/reinitialize teams
	public void initTeam(int _teamSize){
		//base location of UAV team - UAV individual units drawn relative to this location
		loc = new myPointf(initLoc);
		uavTeam = new myUAVObj[_teamSize];
		//sim.dispOutput("\tmyUAVTeam : make UAV team of size : "+ _teamSize+ " name : " + name);
		//2nd idx : 0 is normal, 1 is location
		myPointf[][] teamLocs = sim.getRegularSphereList(rad, _teamSize, 1.0f);
		for(int c = 0; c < uavTeam.length; ++c){uavTeam[c] =  new myUAVObj(this,teamLocs[c][1]);}
		motionTraj = new myVectorf();
		uavVelVec = new myVectorf();
		stLoc = new myPointf(initLoc);
		endLoc = new myPointf(initLoc);
		curJob = "Waiting";
		setEntityFlags(inTransitLane, false);
	}//initTeam - run after each flock has been constructed	
	
	//called by super at end of ctor
	protected void initEntity() {
		//initialize the flags for this entity
		initEntityFlags(numEntityFlags);		
	}//initEntity

	public void setEntityFlags(int idx, boolean val) {
		boolean curVal = getEntityFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		entityFlags[flIDX] = (val ?  entityFlags[flIDX] | mask : entityFlags[flIDX] & ~mask);
		switch(idx){
			case dbgEntityIDX 			: {
				break;}
			case inTransitLane 			: {
				break;}
		}		
	}//setEntityFlags

	//set the template of this UAV team
	public void setTemplate(myRenderObj[] _tmpl, myRenderObj[] _sphrTmpl){
		tmpl = _tmpl[curType];
		sphTmpl = _sphrTmpl[curType];
	}//set after init - all flocks should be made
	
//	//finds valid coordinates if torroidal walls, but doesn't change coords
//	public myPointf findValidWrapCoordsForDraw(myPointf _coords){return new myPointf(((_coords.x+pa.gridDimX) % pa.gridDimX),((_coords.y+pa.gridDimY) % pa.gridDimY),((_coords.z+pa.gridDimZ) % pa.gridDimZ));	}//findValidWrapCoords	
//	//sets coords to be valid if torroidal walls
//	public void setValidWrapCoordsForDraw(myPointf _coords){_coords.set(((_coords.x+pa.gridDimX) % pa.gridDimX),((_coords.y+pa.gridDimY) % pa.gridDimY),((_coords.z+pa.gridDimZ) % pa.gridDimZ));	}//findValidWrapCoords	
	
	//move creatures to random start positions
	public void drawEntity(IRenderInterface pa, DESSimWindow win, float delT, boolean drawMe, boolean drawLbls){
		pa.pushMatState();
		pa.translate(loc);
			boolean debugAnim = sim.getDebug();
			pa.pushMatState();
			pa.setColorValStroke(IRenderInterface.gui_Black, 255);
			pa.setStrokeWt(2.0f);
			if(debugAnim) {pa.drawLine(new myPointf(), motionTraj);}//motion trajectory vector
			pa.popMatState();
			//individual UAVs are relative to loc
			if(sim.getDrawBoats()){//broken apart to minimize if checks - only potentially 2 per team per frame instead of thousands
				if(debugAnim){		for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeDbgFrame(Base_DispWindow.AppMgr, pa,delT);}}
				else {				for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMe(pa,delT);}}	  					
			} else {
				for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeBall(Base_DispWindow.AppMgr, pa,debugAnim);  }
			}
		if(drawLbls) {	dispEntityLabel(pa, win);		}
		pa.popMatState();
	}//drawTeam
	
	//public void leaveTransitLane() {setEntityFlags(inTransitLane, false);}
	//set the destination, trajectory and duration of the motion this team should follow - called upon entry into transit lane
	//ending location will be _stLoc + _traj
	//TODO _dur should be a factor of speed of team, so should be superfluous
	//call this initially when entering transit lane
	public void setTrajAndDur( myPointf _stLoc, myPointf _endLoc, Long _dur) {
		stLoc.set(_stLoc);endLoc.set(_endLoc);
		motionTraj.set(new myVectorf (stLoc, endLoc));
		uavVelVec.set(motionTraj);
		uavVelVec._normalize()._mult(teamSpeed);
		motionDur = _dur;
		curMotionTime = 0;
		setEntityFlags(inTransitLane,true);
	}//setTrajAndDur
	
	//move this team some incremental amount toward the destination - call every sim step
	public void moveUAVTeam(long deltaT) {
		if(!getEntityFlags(inTransitLane)) {return;}
		//only move if in transit lane
		curMotionTime += deltaT;
		float interp = curMotionTime/(1.0f*motionDur); 
		if(interp > 1.0f) {interp = 1.0f;}
		loc.set(myPointf._add(stLoc, interp, motionTraj));
		//moveUAV
		float delT = 1.0f;//approx 30 fps - don't go too high or boat gets lost
		for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].moveUAV(uavVelVec, delT);}//set orientation
	}//
	
	//call when forcing to move to specific location
	//clear means to get rid of all trajectory stuff
	public void moveUAVTeamToDest(myPointf _dest) {
		setEntityFlags(inTransitLane,false);
		loc.set(_dest);
		float delT = .033f;//approx 30 fps
		for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].moveUAV(uavVelVec, delT);}	//sets orientations of UAVs	
	}
	
	//distance between UAV teams to be preserved when visualizing in queue
	public float getNoflyDist() {return 2.0f * rad;}
	
	//put together list of values regarding this UAV team to show on screen
	protected String[] showStatus() {
		String[] res = {
			"Name : " + name ,
			"Cur Job : " + curJob,
			"Cur time elapsed : " + timeVals[curTimeInProc],
			"TTL Runtime : " + (timeVals[ttlRunTime] + timeVals[curTimeInProc]) ,
			"TTL Time Working : " + timeVals[ttlTaskTime],
			"TTL Time Traveling : " + timeVals[ttlTravelTime],
			"TTL Time In Q : " + timeVals[ttlQueueTime],
			"TTL # Completed Procs : " + timeVals[ttlNumTeamsProc]
		};
		return res;
	}//showStatus
		
	//call at end of process (in final task) - reset current time, add
	public void finishedProcess() {
		timeVals[ttlRunTime] += timeVals[curTimeInProc];
		//reset values
		timeVals[timeEnterQueue] = 0L;		
		timeVals[curTimeInProc] = 0L;
		curJob = "None";
	}//finishedProcess
	
	//called when task time is computed in task arrival -NOTE this will be done before current task is actually finished
	//also set task name upon arrival
	public void addTimeInTask(String _name, long _t) {
		curJob = _name;
		timeVals[curTimeInProc] += _t;
		timeVals[ttlTaskTime] += _t;		
	}//addTimeInTask
	
	//increment the complete process counter
	public void addCompletedProcess() {	++timeVals[ttlNumTeamsProc];}
	
	//called when travel time is computed in transit lane arrival -NOTE this will be done before current transit is actually finished
	public void addTimeInTransit(String _name, long _t, long _enterQueue) {
		curJob = _name;
		timeVals[curTimeInProc] += _t;
		timeVals[ttlTravelTime] += _t;	
		//record the time when this team will enter the queue
		timeVals[timeEnterQueue] = _enterQueue;
	}//addTimeInTransit
	
	//called when leaving a queue - pass simulation time at exit, since don't know when that will be until after we leave
	//returns time in queue this team just experienced
	public long leaveQueueAddQTime(long _timeLeavingQ) {
		long tInQueue = _timeLeavingQ - timeVals[timeEnterQueue];
		timeVals[curTimeInProc] += tInQueue;
		timeVals[ttlQueueTime] += tInQueue;
		timeVals[timeEnterQueue] = 0L;		
		//no longer in transit lane queue
		setEntityFlags(inTransitLane, false);
		return tInQueue;
	}//addTimeInTransit	
		
	public String toString(){
		String res = super.toString();
		res += "\tTeam Size " + uavTeam.length + "\n";
		res +="cur motion time : " + curMotionTime + " stLoc :  " + stLoc.toStrBrf() + " end loc : " + endLoc.toStrBrf() + " | Motion Dur : " + motionDur + " | motionTraj : " + motionTraj.toString() +"\n";
		for(myUAVObj bd : uavTeam){			res+="\t     UAV "+bd.toString(); res+="\n";	}
		return res;
	}

}//myUAVTeam class


