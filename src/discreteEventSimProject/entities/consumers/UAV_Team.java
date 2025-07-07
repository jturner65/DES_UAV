package discreteEventSimProject.entities.consumers;


import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.renderedObjs.base.Base_RenderObj;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import discreteEventSimProject.entities.base.Base_Entity;
import discreteEventSimProject.entities.base.EntityType;
import discreteEventSimProject.sim.base.Base_DESSimulator;

/**
 * class holding the graphical and simulation parameters for a UAV team entity of a certain size.  
 * @author john
 *
 */
public class UAV_Team extends Base_Entity {
    /**
     * The group of UAVs making up this team
     */
    public UAV_Obj[] uavTeam;
    /**
     * radius of sphere upon which team members will be placed
     */
    public static final float rad = 5;            
    
    /**
     * speed of UAV team moving from task to task m/sec (assume constant) :: 5 m/s is ~11 mph.
     * if ever not made final need to update value held in UAV_TransitLane when changed (referenced for precalc)
     */
    public static final float teamSpeed = 1;                
    private Base_RenderObj tmpl, sphTmpl;                //template to render boid; simplified sphere template
    
    //////////
    //metrics of team performance
    private String curJob;                    //name of current resource along with current activity (traveling, in queue, working)    
    
    //////////
    //team movement variables
    private myPointf stLoc, endLoc, initLoc; 
    private myVectorf motionTraj,                         //start and end targets for team motion,  and trajectory of motion to follow 
            uavVelVec;                            //uav velocity vector - along motionTraj
    private double motionDur;                        //how long the motion should take to follow the trajectory in milliseconds
    private double curMotionTime;                //current time elapsed since motion began in milliseconds
    
    /////////
    //entity flags structure idxs
    public static final int 
            dbgEntityIDX  = 0,
            inTransitLane = 1;                //this team is in transit lane and should be moved
    
    public int numEntityFlags = 2;
    
    private int teamID; 
    private static int teamIncr = 0;
    private int curType;
    
    private int teamSize;
    
    public UAV_Team(Base_DESSimulator _sim, String _name, int _teamSize, myPointf _initLoc){
        super(_sim, _name, _initLoc, new EntityType[] {EntityType.Consumer});
        //set so always remembers where it started
        teamID = teamIncr++;
        curType = teamID % sim.getNumUniqueTeams();//what type of boat to show
        initLoc = new myPointf(_initLoc);
        teamSize = _teamSize;
        labelVals = new float[] {-name.length() * 3.0f, -(2.0f*rad + 70), 0};
        lblColors = new int[] {0,0,0,255};
    }//myBoidFlock constructor
    
    /**
     * make UAV team - initialize/reinitialize teams
     */
    public void initTeam(){
        //base location of UAV team - UAV individual units drawn relative to this location
        loc = new myPointf(initLoc);
        uavTeam = new UAV_Obj[teamSize];
        //sim.dispOutput("\tUAV_Team : make UAV team of size : "+ _teamSize+ " name : " + name);
        //2nd idx : 0 is normal, 1 is location
        myPointf[][] teamLocs = MyMathUtils.getRegularSphereList(rad, teamSize, 1.0f);
        for(int c = 0; c < uavTeam.length; ++c){uavTeam[c] =  new UAV_Obj(this,teamLocs[c][1]);}
        motionTraj = new myVectorf();
        uavVelVec = new myVectorf();
        stLoc = new myPointf(initLoc);
        endLoc = new myPointf(initLoc);
        curJob = "Waiting";
        setEntityFlags(inTransitLane, false);
    }//initTeam - run after each flock has been constructed    
    
    /**
     * If this is being animated and has a template, return that template's max animation counter, otherwise return 1
     * @return
     */
    public double getMaxAnimCounter() {
        //If not rendered, template will be null so just make this 1.
        return tmpl == null ? 1.0 : tmpl.getMaxAnimCounter();
    }
    
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
            case dbgEntityIDX             : {
                break;}
            case inTransitLane             : {
                break;}
        }        
    }//setEntityFlags

    /**
     * set the template of this UAV team
     * @param _tmpl
     * @param _sphrTmpl
     */
    public void setTemplate(Base_RenderObj[] _tmpl, Base_RenderObj[] _sphrTmpl){
        tmpl = _tmpl[curType];
        sphTmpl = _sphrTmpl[curType];
    }//set after init - all flocks should be made
    
    
    /**
     * Set the render template to use for this flock
     * @param _tmpl
     */
    public final void setCurrTemplate(Base_RenderObj _tmpl) {tmpl = _tmpl;}
    
    /**
     * Retrieve the current template used for boids
     * @return
     */
    public final Base_RenderObj getCurrTemplate(){return tmpl;}
    
    /**
     * Retrieve the un-animated sphere base template
     * @return
     */
    public final Base_RenderObj getSphereTemplate(){return sphTmpl;}
    
    
    /**
     *
     */
    public void drawEntity(float delT, boolean drawMe){
        ri.pushMatState();
            ri.translate(loc);
            boolean debugAnim = sim.getSimDebug();
            ri.pushMatState();
            ri.setColorValStroke(IRenderInterface.gui_Black, 255);
            ri.setStrokeWt(2.0f);
            if(debugAnim) {ri.drawLine(new myPointf(), motionTraj);}//motion trajectory vector
            ri.popMatState();
            if(debugAnim){        for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeDbgFrame(Base_DispWindow.AppMgr, ri,delT);}}
            //individual UAVs are relative to loc
            if(sim.getDrawBoats()){//broken apart to minimize if checks - only potentially 2 per team per frame instead of thousands
                for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMe(ri,delT);}                          
            } else {
                for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].drawMeBall(Base_DispWindow.AppMgr, ri,debugAnim);  }
            }
        ri.popMatState();
    }//drawTeam
    
    /**
     * set the destination, trajectory and duration of the motion this team should follow - called upon entry into transit lane. 
     * Ending location will be _stLoc + _traj.
     * TODO _dur should be a factor of speed of team, so should be superfluous.
     * Call this initially when entering transit lane.
     * @param _stLoc
     * @param _endLoc
     * @param _dur
     */
    public void setTrajAndDur( myPointf _stLoc, myPointf _endLoc, Long _dur) {
        stLoc.set(_stLoc);endLoc.set(_endLoc);
        motionTraj.set(new myVectorf (stLoc, endLoc));
        uavVelVec.set(motionTraj);
        uavVelVec._normalize()._mult(teamSpeed);
        motionDur = _dur;
        curMotionTime = 0;
        setEntityFlags(inTransitLane,true);
    }//setTrajAndDur
    
    /**
     * move this team some incremental amount toward the destination - call every sim step
     * @param deltaT
     */
    public void moveUAVTeam(float scaledMillisSinceLastFrame, float delT) {
        if(!getEntityFlags(inTransitLane)) {return;}
        //only move if in transit lane
        curMotionTime += scaledMillisSinceLastFrame;
        float interp = (float) (curMotionTime/(1.0f*motionDur)); 
        if(interp > 1.0f) {interp = 1.0f;}
        loc.set(myPointf._add(stLoc, interp, motionTraj));
        //moveUAV
        for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].moveUAV(uavVelVec, delT);}//set orientation
    }//
    
    /**
     * call when forcing to move to specific location.clear means to get rid of all trajectory stuff
     * @param _dest
     */
    public void moveUAVTeamToDest(myPointf _dest, float delT) {
        setEntityFlags(inTransitLane,false);
        loc.set(_dest);
        for(int c = 0; c < uavTeam.length; ++c){uavTeam[c].moveUAV(uavVelVec, delT);}    //sets orientations of UAVs    
    }
    
    /**
     * distance between UAV teams to be preserved when visualizing in queue
     * @return
     */
    public float getNoflyDist() {return 2.0f * rad;}
    
    /**
     * put together list of values regarding this UAV team to show on screen
     */
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
    
    /**
     * Called when task time is computed in task arrival -NOTE this will be done before current task is 
     * actually finished. Also set task name upon arrival
     * @param _name
     * @param _t
     */
    public void addTimeInTask(String _name, long _t) {
        curJob = _name;
        timeVals[curTimeInProc] += _t;
        timeVals[ttlTaskTime] += _t;        
    }//addTimeInTask
    
    /**
     * increment the complete process counter
     */
    public void addCompletedProcess() {    ++timeVals[ttlNumTeamsProc];}
    
    /**
     * called when travel time is computed in transit lane arrival -NOTE this will be done before current transit is actually finished
     * @param _name
     * @param _t
     * @param _enterQueue
     */
    public void addTimeInTransit(String _name, long _t, long _enterQueue) {
        curJob = _name;
        timeVals[curTimeInProc] += _t;
        timeVals[ttlTravelTime] += _t;    
        //record the time when this team will enter the queue
        timeVals[timeEnterQueue] = _enterQueue;
    }//addTimeInTransit
    
    /**
     * called when leaving a queue - pass simulation time at exit, since don't know when that will be until after we leave.
     * returns time in queue this team just experienced
     * @param _timeLeavingQ
     * @return
     */
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
        res +="cur motion time : " + curMotionTime + " stLoc :  " + stLoc.toStrBrf() 
            + " end loc : " + endLoc.toStrBrf() + " | Motion Dur : " 
                + motionDur + " | motionTraj : " + motionTraj.toString() +"\n";
        for(UAV_Obj bd : uavTeam){            res+="\t     UAV "+bd.toString(); res+="\n";    }
        return res;
    }

}//UAV_Team class


