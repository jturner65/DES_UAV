package pkgCS6730Project1;

import java.util.concurrent.*;

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
	public void drawEntity(UAV_DESSim pa, float delT, boolean drawMe, boolean drawLbls){
		pa.pushMatrix();pa.pushStyle();
		pa.translate(loc);
			boolean debugAnim = sim.getDebug();
			pa.pushMatrix();pa.pushStyle();
			pa.setColorValStroke(pa.gui_Black);
			pa.strokeWeight(2.0f);
			if(debugAnim) {pa.line(new myPointf(), motionTraj);}//motion trajectory vector
			pa.popStyle();pa.popMatrix();
			//individual UAVs are relative to loc
			if(sim.getDrawBoats()){//broken apart to minimize if checks - only potentially 2 per team per frame instead of thousands
				if(debugAnim){		for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeDbgFrame(pa,delT);}}
				else {				for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMe(pa,delT);}}	  					
			} else {
				for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeBall(pa,debugAnim);  }
			}
		if(drawLbls) {	dispEntityLabel(pa);		}
		pa.popStyle();pa.popMatrix();
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


//class referencing a single uav object
class myUAVObj {
	public myUAVTeam f;
	public int ID;
	private static int IDcount = 0;
	//# of animation frames to be used to cycle 1 motion by render objects
	//graphics and animation controlling variables
	public static final int numAnimFrames = 90;
	private myVectorf scaleBt;													//scale of rendered object
	private float animCntr;
	public float animPhase;
	//
	public int animAraIDX;//index in numAnimFrames-sized array of current animation state
	
	public static final float maxAnimCntr = 1000.0f, baseAnimSpd = 1.0f;
	
	//location and orientation variables	
	public float[] O_axisAngle;															//axis angle orientation of this UAV
	public static final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
	public static final float fsqrt2 = (float)(Math.sqrt(2.0));
	private static final float rt2 = .5f * fsqrt2;

	public float oldRotAngle;	
	public final myVectorf scMult = new myVectorf(.5f,.5f,.5f);				//multiplier for scale based on mass
	private myVectorf rotVec;													//rotational vector, 
	
	public myPointf coords;														//com coords
	public myVectorf velocity;
	public myVectorf[] orientation;												//Rot matrix - 3x3 orthonormal basis matrix - cols are bases for body frame orientation in world frame
					
	public myUAVObj(myUAVTeam _f, myPointf _coords){
		ID = IDcount++;		
		//p = _p;		
		f = _f; 		
		//preCalcAnimSpd = (float) ThreadLocalRandom.current().nextDouble(.5f,2.0);		
		animPhase = (float) ThreadLocalRandom.current().nextDouble(.25f, .75f ) ;//keep initial phase between .25 and .75 so that cyclic-force UAVs start moving right away
		animCntr = animPhase * maxAnimCntr;
		animAraIDX = (int)(animPhase * numAnimFrames);	

		rotVec = myVectorf.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVectorf[3];
		orientation[O_FWD] = myVectorf.FORWARD.cloneMe();
		orientation[O_RHT] = myVectorf.RIGHT.cloneMe();
		orientation[O_UP] = myVectorf.UP.cloneMe();
		
		coords = new myPointf(_coords);	//new myPointf[2]; 
		velocity = new myVectorf();
		O_axisAngle=new float[]{0,1,0,0};
		oldRotAngle = 0;
		scaleBt = new myVectorf(scMult);					//for rendering different sized UAVs
		
	}//constructor
	
	//align the UAV along the current orientation matrix
	private void alignUAV(UAV_DESSim pa, float delT){
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		float rotAngle = (float) (oldRotAngle + ((O_axisAngle[0]-oldRotAngle) * delT));
		pa.rotate(rotAngle,rotVec.x, rotVec.y, rotVec.z);
		oldRotAngle = rotAngle;
	}//alignUAV	
	
	private static float epsValCalc = mySimulator.epsValCalc, epsValCalcSq = epsValCalc * epsValCalc;
	private float[] toAxisAngle() {
		float angle,x=rt2,y=rt2,z=rt2,s;
		float fyrx = -orientation[O_FWD].y+orientation[O_RHT].x,
			uxfz = -orientation[O_UP].x+orientation[O_FWD].z,
			rzuy = -orientation[O_RHT].z+orientation[O_UP].y;
			
		if (((fyrx*fyrx) < epsValCalcSq) && ((uxfz*uxfz) < epsValCalcSq) && ((rzuy*rzuy) < epsValCalcSq)) {			//checking for rotational singularity
			// angle == 0
			float fyrx2 = orientation[O_FWD].y+orientation[O_RHT].x,
				fzux2 = orientation[O_FWD].z+orientation[O_UP].x,
				rzuy2 = orientation[O_RHT].z+orientation[O_UP].y,
				fxryuz3 = orientation[O_FWD].x+orientation[O_RHT].y+orientation[O_UP].z-3;
			if (((fyrx2*fyrx2) < 1)	&& (fzux2*fzux2 < 1) && ((rzuy2*rzuy2) < 1) && ((fxryuz3*fxryuz3) < 1)) {	return new float[]{0,1,0,0}; }
			// angle == pi
			angle = (float) Math.PI;
			float fwd2x = (orientation[O_FWD].x+1)/2.0f,rht2y = (orientation[O_RHT].y+1)/2.0f,up2z = (orientation[O_UP].z+1)/2.0f,
				fwd2y = fyrx2/4.0f, fwd2z = fzux2/4.0f, rht2z = rzuy2/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< epsValCalc) {	x = 0;} else {			x = (float) Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;} 
			} else if (rht2y > up2z) { 		// orientation[O_RHT].y is the largest diagonal term
				if (rht2y< epsValCalc) {	y = 0;} else {			y = (float) Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< epsValCalc) {	z = 0;} else {			z = (float) Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new float[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		myVectorf tmp = new myVectorf(rzuy, uxfz, fyrx);
		s = tmp.magn;
		if (s < epsValCalc){ s=1; }
		tmp._scale(s);//changes mag to s
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = (float) -Math.acos(( orientation[O_FWD].x + orientation[O_RHT].y + orientation[O_UP].z - 1)/2.0);
	   return new float[]{angle,tmp.x,tmp.y,tmp.z};
	}//toAxisAngle
	
	private myVectorf getFwdVec( float delT){
		if(velocity.magn < epsValCalc){			return orientation[O_FWD]._normalize();		}
		else {		
			myVectorf tmp = velocity.cloneMe()._normalize();			
			return new myVectorf(orientation[O_FWD], delT, tmp);		
		}
	}//getFwdVec
	
	private myVectorf getUpVec(){	
		float fwdUpDotm1 = orientation[O_FWD]._dot(myVectorf.UP);
		if (1.0 - (fwdUpDotm1 * fwdUpDotm1) < epsValCalcSq){
			return myVectorf._cross(orientation[O_RHT], orientation[O_FWD]);
		}
		return myVectorf.UP.cloneMe();
	}	
	
	private void setOrientation(float delT){
		//find new orientation at new coords - 
		orientation[O_FWD].set(getFwdVec(delT));
		orientation[O_UP].set(getUpVec());	
		orientation[O_RHT] = orientation[O_UP]._cross(orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		//orientation[O_RHT] = orientation[O_FWD]._cross(orientation[O_UP]); //sideways is cross of up and forward - backwards(righthanded)
		orientation[O_RHT]._normalize();
		//orientation[O_RHT].set(orientation[O_RHT]._normalize());
		//need to recalc up?  may not be perp to normal
		if(Math.abs(orientation[O_FWD]._dot(orientation[O_UP])) > epsValCalc){
			orientation[O_UP] = orientation[O_FWD]._cross(orientation[O_RHT]); //sideways is cross of up and forward
			//orientation[O_UP].set(orientation[O_UP]._normalize());
			orientation[O_RHT]._normalize();
		}
		O_axisAngle = toAxisAngle();
	}
	
	//move uav in direction and magnitude of vector _vel, using delT, and aligning along vector _vel
	public void moveUAV(myVectorf _vel, float delT) {
		velocity.set(_vel);			//velocity vector only used to determine orientation
		setOrientation(delT);
	}//moveUAV

	
	//draw this body on mesh
	public void drawMe(UAV_DESSim p, float delT){
		p.pushMatrix();p.pushStyle();
			p.translate(coords.x,coords.y,coords.z);		//move to location
			alignUAV(p, delT);
			p.rotate(p.PI/2.0f,1,0,0);
			p.rotate(p.PI/2.0f,0,1,0);
			p.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			p.pushStyle();
			f.tmpl.drawMe(animAraIDX, ID);
			p.popStyle();			
		p.popStyle();p.popMatrix();
		animIncr();
	}//drawme	
	
	public void drawMeDbgFrame(UAV_DESSim p, float delT){
		p.pushMatrix();p.pushStyle();
			p.translate(coords.x,coords.y,coords.z);		//move to location
			drawMyVec(p, rotVec, UAV_DESSim.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);
			alignUAV(p, delT);
			p.rotate(p.PI/2.0f,1,0,0);
			p.rotate(p.PI/2.0f,0,1,0);
			p.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			p.pushStyle();
			f.tmpl.drawMe(animAraIDX, ID);	
			p.popStyle();			
		p.popStyle();p.popMatrix();
		animIncr();		
	}
	
	//draw this UAV as a ball - replace with sphere render obj 
	public void drawMeBall(UAV_DESSim p, boolean debugAnim){
		p.pushMatrix();p.pushStyle();
			p.translate(coords.x,coords.y,coords.z);		//move to location
			if(debugAnim){drawMyVec(p,rotVec, UAV_DESSim.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			p.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			f.sphTmpl.drawMe(animAraIDX, ID);
		p.popStyle();p.popMatrix();
		//animIncr();
	}//drawme 
	
	public void drawMyVec(UAV_DESSim p, myVectorf v, int clr, float sw){
		p.pushMatrix();
			p.pushStyle();
			p.setColorValStroke(clr);
			p.strokeWeight(sw);
			p.line(new myPointf(0,0,0),v);
			p.popStyle();
		p.popMatrix();		
	}
	
	private void animIncr(){
		animCntr += (baseAnimSpd + (velocity.magn *.1f));//*preCalcAnimSpd;						//set animMod based on velocity -> 1 + mag of velocity	
		animPhase = ((animCntr % maxAnimCntr)/maxAnimCntr);									//phase of animation cycle
		animAraIDX = (int)(animPhase * numAnimFrames);	
	}//animIncr		
	
	public String toString(){
		String result = "ID : " + ID;
		return result;
	}	
}//myUAVObj class
