package discreteEventSimProject.sim.base;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.my_procApplet;
import discreteEventSimProject.entities.myUAVTask;
import discreteEventSimProject.entities.myUAVTeam;
import discreteEventSimProject.entities.myUAVTransitLane;
import discreteEventSimProject.entities.base.myEntity;
import discreteEventSimProject.events.EventType;
import discreteEventSimProject.events.myEvent;
import discreteEventSimProject.renderedObjs.myBoatRndrObj;
import discreteEventSimProject.renderedObjs.mySphereRndrObj;
import discreteEventSimProject.renderedObjs.base.myRenderObj;
import discreteEventSimProject.sim.mySimExecutive;
import discreteEventSimProject.sim.task.taskDesc;
import discreteEventSimProject.ui.DESSimWindow;
import processing.core.*;

/**
 * concrete class to manage specific simulation environment and event handling for DES simulation - owned by and called from sim executive
 * @author john
 */
public abstract class mySimulator {
	//owning executor
	public mySimExecutive exec;
	//fixed structure holding task resources in this simulation
	public myUAVTask[] tasks;	
	//fixed structure holding transit lane resource queues in this simulation
	public myUAVTransitLane[] transitLanes;
	//variable structure holding collection of uavteams in play in this simulation
	public ArrayList<myUAVTeam> teams;		
	//single transitionlane from last task to first task, to provide recycling of teams
	public myUAVTransitLane holdingLane;
	//maximum # of UAV units available - can't put more than this many into play - can specify in constructor
	protected final int maxNumUAVs;	

	//locations of tasks TODO make this random?  might be interesting to see how it impacts sim
	protected myPointf[] taskLocs;
	//optimal team size for each task
	protected int[] taskOptSize;
	//time required for optimal team size to complete task, in minutes.(mult by 60000 for milliseconds)
	protected float[] optTeamTTCMins;
	//stdev multiplier (if != 0 then uses TTC as mean of gaussian with stdev == mult * optTeamSize/teamSize)
	protected float[] stdDevTTCMult;
	//transit lane : idxs of tasks connecting each lane (parent, child)
	protected int[][] TL_taskIdxs;
	//whether a particular task is a group task or not
	protected boolean [] isGrpTask; 
		
	//size of UAV teams - modified by UI or commandline entry TODO
	public static int uavTeamSize = 4;
	
	public static final float epsValCalc = .00000001f;
	
	//flags relevant to simulator executive execution
	protected int[] simFlags;	
	public static final int
					debugSimIDX 		= 0,
					buildVisObjsIDX		= 1,						//TODO whether or not to build visualization objects - turn off to bypass unnecessary stuff when using console only
					drawVisIDX			= 2,						//draw visualization - if false should ignore all processing/papplet stuff
					drawBoatsIDX 		= 3,						//either draw boats or draw spheres for consumer UAV team members
					drawUAVTeamsIDX		= 4,						//yes/no draw UAV teams
					drawTaskLocsIDX		= 5,						//yes/no draw task spheres
					drawTLanesIDX		= 6,						//yes/no draw transit lanes and queues
					dispTaskLblsIDX		= 7,						//show labels over tasks
					dispTLnsLblsIDX		= 8,
					dispUAVLblsIDX		= 9;
	
	protected static final int numSimFlags = 10;
	////////////////////////
	// reporting stuff
	//string representation of date for report data
	private String rptDateNowPrfx;
	private String rptExpDir;
	//main directory to put experiments
	private String baseDirStr;
	//arrays of each trial's results for each type of metric - at end of experiment, save total results as avgs of specific results in these arrays
	//private String[][] uavResStrs, tlResStrs, taskResStrs;
	//arrays to hold each trial's results for each type of metric - at end of experiment, save total results as avgs of specific results in these arrays
	//per trial totals of all uavs' performance
	private long[][] uavRes;
	//per trial, per task or per transit lane, per measured result value array of all exp data
	private long[][][] taskRes,tlRes;
	
	
	/////////////////////////
	// rendering stuff
//	// structure holding UAV teams and the rendered versions of them - move to myRenderObj? - from boids project
	//only 5 different flocks will display nicely on side menu
	public String[] UAVTeamNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters"};
	public PImage[] UAVBoatSails;						//image sigils for sails	
	public String[] UAVTypeNames = new String[]{"Boats"};
	public final int NumUniqueTeams = UAVTeamNames.length, numBoidTypes = UAVTypeNames.length;			
	//array of template objects to render
	//need individual array for each type of object, sphere (simplified) render object
	protected myRenderObj[] rndrTmpl,//set depending on UI choice for complex rndr obj 
		boatRndrTmpl,
		//add more rendr obj arrays here
		sphrRndrTmpl;//simplified rndr obj (sphere)	
	protected ConcurrentSkipListMap<String, myRenderObj[]> cmplxRndrTmpls;	
	
	public mySimulator(mySimExecutive _exec, int _maxNumUAVs) {
		exec=_exec;
		maxNumUAVs = _maxNumUAVs;
		initOnce();
	}//ctor
	
	//called 1 time for all simulations
	private void initOnce() {
		Instant now = Instant.now();
		rptDateNowPrfx = "ExpDate_"+now.toString()+"_";
		rptDateNowPrfx=rptDateNowPrfx.replace(":", "-");
		rptDateNowPrfx=rptDateNowPrfx.replace(".", "-");
		//root directory to put experimental results = add to all derived exp res directories
		baseDirStr = exec.getCWD() + File.separatorChar + "experiments";
		@SuppressWarnings("unused")
		boolean baseDirMade = exec.createRptDir(baseDirStr);
		
		//setup flag array
		initSimFlags();
		//set up render object templates for different UAV Teams
		if(exec.pa != null) {	
			my_procApplet pa = (my_procApplet) exec.pa;
			sphrRndrTmpl = new mySphereRndrObj[NumUniqueTeams];
			for(int i=0; i<NumUniqueTeams; ++i){		sphrRndrTmpl[i] = new mySphereRndrObj(exec.pa, this, i);	}	
			cmplxRndrTmpls = new ConcurrentSkipListMap<String, myRenderObj[]> (); 
			UAVBoatSails = new PImage[NumUniqueTeams];
			boatRndrTmpl = new myBoatRndrObj[NumUniqueTeams];
			for(int i=0; i<NumUniqueTeams; ++i){	
				UAVBoatSails[i] = pa.loadImage(UAVTeamNames[i]+".jpg");
				//build boat render object for each individual flock type
				boatRndrTmpl[i] = new myBoatRndrObj(pa, this, i);		
			}		
			cmplxRndrTmpls.put(UAVTypeNames[0], boatRndrTmpl);
			rndrTmpl = cmplxRndrTmpls.get(UAVTypeNames[0]);//start by rendering boats
		}
		initSimPriv();
		isGrpTask = getIsGroupAra();
	}//initOnce
	
	protected myUAVTransitLane buildTransitLane(myUAVTask[] _tasks, int stIdx, int endIdx, float laneVel, boolean showMsg) {
		String TLName =  "TransitLane_From_" + tasks[stIdx].name+"_to_"+ tasks[endIdx].name;
		//set location halfway between the two tasks this lane connects
		myPointf loc = new myPointf(_tasks[stIdx].loc, .5f, _tasks[endIdx].loc);
		myUAVTransitLane tl = new myUAVTransitLane(this, TLName, loc, 30.0f, laneVel);
		//set all parent and child tasks for each transit lane (always has 1 parent and 1 child)
		//add to transit
		tl.setTransitLaneConnections(_tasks[stIdx], _tasks[endIdx]);
		return tl;
	}//buildTransitLane
	
	//build all tasks for this simulation
	protected myUAVTask[] buildTasks(taskDesc[] tDesc, boolean showMsg) {
		//set # of tasks based on passed location array tasks exist in simulation
		myUAVTask[] _ts = new myUAVTask[tDesc.length];				
		//first build tasks 
		for(int i=0;i<_ts.length;++i) {	_ts[i]=new myUAVTask(this, tDesc[i]);}
		if(showMsg) {
			exec.dispOutput("mySimulator", "buildTasks", "All " + _ts.length + " Tasks initialized.");	
		}
		return _ts;
	}//buildTasks
	
	//build transit lanes after tasks have been built - set transit lanes to be child and parent task's parent and child, respectively
	protected myUAVTransitLane[] buildTransitLanes(myUAVTask[] _tasks, int[][] _TL_taskIdxs, ArrayList<Integer>[] _taskParentIDXs, ArrayList<Integer>[] _taskChildIDXs, boolean showMsg) {
		//set # transit lanes exist in simulation to be # of entries in parent/child task mapping
		myUAVTransitLane[] _tl = new myUAVTransitLane[_TL_taskIdxs.length];		
		for(int i=0;i<_tl.length;++i) {
			_tl[i] = buildTransitLane(_tasks, _TL_taskIdxs[i][0], _TL_taskIdxs[i][1], 1.0f, showMsg);
			//add this idx to parent task's child list
			_taskChildIDXs[_TL_taskIdxs[i][0]].add(i);
			//add this idx to child task's parent list
			_taskParentIDXs[_TL_taskIdxs[i][1]].add(i);			
		}
		//holding lane is lane from final task to initial task - in all simulations
		holdingLane = buildTransitLane(_tasks, _tasks.length-1, 0, 10.0f, showMsg);
		if(showMsg) {
			exec.dispOutput("mySimulator", "buildTransitLanes","All " + _tl.length + " transitLanes initialized and connected to parents and children, along with holding lane.");
		}
		return _tl;
	}//buildTransitLanes	
	
	//these task descriptions will drive the task construction and consumption
	protected taskDesc[] buildTaskDesc(myPointf[] _tlocs, boolean[] _isGrp, int[] _tOptSize, float[] _optTeamTTCMins, float[] _stdDevTTCMult, boolean showMsg) {
		taskDesc[] tDesc = new taskDesc[_tlocs.length];
		//radius of rendered sphere
		float rad = 30.0f;
		for (int i=0;i<tDesc.length;++i) {tDesc[i] = new taskDesc(i, _tlocs[i], _isGrp[i], _tOptSize[i],_optTeamTTCMins[i]*60000,_stdDevTTCMult[i], (_isGrp[i]?2.0f:1.0f)*rad, uavTeamSize, showMsg);}
		return tDesc;
	}//buildTaskDesc
	
	//set parent and child transit lanes for each task
	protected void setTaskParentChild(myUAVTask[] _tasks, myUAVTransitLane[] _tl, ArrayList<Integer>[] _taskParentIDXs, ArrayList<Integer>[] _taskChildIDXs, boolean showMsg) {
		//set parent and child Transit lanes for each task - some tasks have multiple parents or children.  if multiple children, need to send probability structure for 
		//cumulative prob dist
		for(int i=0;i<_tasks.length;++i) {
			//add all children - need specific probability for cumulative list
			int numChildren = _taskChildIDXs[i].size();
			for(int j=0;j<numChildren;++j) {
				//set key to be uniform among all children.  TODO change this if want to favor one path over another
				_tasks[i].addChild(j/(1.0f * numChildren), _tl[_taskChildIDXs[i].get(j)]);
			}
			//all all parents - key value is irrelevant, just must be unique
			for(int j=0;j<_taskParentIDXs[i].size();++j) {_tasks[i].addParent(j, _tl[_taskParentIDXs[i].get(j)]);}
		}
		//handle holding lane child/parent tasks
		_tasks[0].addParent(0.0f, holdingLane);
		_tasks[_tasks.length-1].addChild(0.0f, holdingLane);
		if(showMsg) {
			exec.dispOutput("mySimulator", "setTaskParentChild","All " + _tasks.length + " tasks connected to parent and children transitlanes.");
		}
	}//setTaskParentChild	
	
	//initialize DES Simulation- set up all resources - re call this every time new sim is being set up
	//@Override
	protected abstract void initSimPriv();
	protected abstract boolean[] getIsGroupAra();//determine which tasks are group tasks - sim dependent
	
	//called on ever simulation reset - remakes entire sim, even on huge map is still very fast
	//public void initSim() {initSim(true);}
		
	public void initSim(boolean showMsg) {
		//setup arrays of values that are used to build map
		//initSimPriv();
		//reset world variables	
		//init arrays used to manage task parent and task child transit lane idx's
		//each task has 1 or more parents and 1 or more children - first task has no parents, last task has no children (special cases)
		long stTime = exec.getCurTime();
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] taskParentIDXs = new ArrayList[taskLocs.length];
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] taskChildIDXs = new ArrayList[taskLocs.length];
		//every task has 1 or more parents and 1 or more children, which are transit lanes
		//these arrays are populated when transit lanes are made and then used to set parents and children in tasks
		for(int i=0;i<taskParentIDXs.length;++i) {taskParentIDXs[i] = new ArrayList<Integer>();taskChildIDXs[i] = new ArrayList<Integer>();	}				
		//first build task descriptions
		taskDesc[] tDesc = buildTaskDesc(taskLocs,isGrpTask,taskOptSize,optTeamTTCMins,stdDevTTCMult, showMsg);
		//then build tasks from descriptions
		tasks = buildTasks(tDesc, showMsg);		
		//next build transit lanes, using centerpoint of tasks as location
		//set # transit lanes exist in simulation to be # of entries in parent/child task mapping
		transitLanes = buildTransitLanes(tasks, TL_taskIdxs, taskParentIDXs, taskChildIDXs, showMsg);
		//set parent and child Transit lanes for each task - some tasks have multiple parents or children.  if multiple children, need to send probability structure for 
		//cumulative prob dist
		setTaskParentChild(tasks, transitLanes, taskParentIDXs, taskChildIDXs, showMsg);
		exec.showTimeMsgNow("mySimulator","initSim","Millis to build map", stTime);		
		
		//teams is dynamic - depends on how large team size is specified to be and how many UAVs exist to draw from
		teams = new ArrayList<myUAVTeam>();		
	}//initSim
		
	//boolean flag handling
	protected void initSimFlags(){simFlags = new int[1 + numSimFlags/32]; for(int i = 0; i<numSimFlags; ++i){setSimFlags(i,false);}}
	public boolean getSimFlags(int idx){int bitLoc = 1<<(idx%32);return (simFlags[idx/32] & bitLoc) == bitLoc;}	
	public void setSimFlags(int idx, boolean val) {
		boolean curVal = getSimFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		simFlags[flIDX] = (val ?  simFlags[flIDX] | mask : simFlags[flIDX] & ~mask);
		switch(idx){
			case debugSimIDX 			: {break;}		
			case drawVisIDX				: {break;}		//draw visualization - if false should ignore all processing/papplet stuff				
			case drawBoatsIDX			: {break;}		//either draw boats or draw spheres for consumer UAV team members				
			case drawUAVTeamsIDX		: {break;}		//either draw boats or draw spheres for consumer UAV team members				
			case drawTaskLocsIDX		: {break;}		//either draw boats or draw spheres for consumer UAV team members				
			case drawTLanesIDX			: {break;}		//either draw boats or draw spheres for consumer UAV team members								
			case dispTaskLblsIDX		: {break;}
			case dispTLnsLblsIDX		: {break;}
			case dispUAVLblsIDX			: {break;}			
			default :{}			
		}			
	}//setExecFlags
	
	
	//build event to represent initial process - this will be called when FEL is empty
	public myEvent buildInitialEvent(float nowTime) {
		long longNowTime = (long)Math.round(nowTime);
		myUAVTeam newTeam = addNewTeam(longNowTime);
		//myEvent(long _ts, String _name, myEntity _c_ent, myEntity _r_ent)
		myEvent ev = new myEvent(longNowTime, EventType.ArriveResource, newTeam, tasks[0], holdingLane);		
		return ev;
	}//buildInitialEvent
	
	//add a team to the simulation and put them in task 1 - can only occur when task 1 is unoccupied
	public myUAVTeam addNewTeam(long nowTime) {
		int nextTeamNum = teams.size()+1;
		if((nextTeamNum * uavTeamSize) > this.maxNumUAVs){//too many UAVs in play
			return null;
		}		
		//myUAVTeam(IRenderInterface _p, mySimulator _sim, String _name, int _uavTeamSize, myPointf _initLoc)
		String name = "UAVTeam_" + nextTeamNum + "_Sz_"+uavTeamSize;
		exec.dispOutput("mySimulator", "addNewTeam","Adding Team @TS : "+String.format("%08d", (int)nowTime)+" | Name of UAV Team : " + name + " Size of UAV Team "+uavTeamSize);
		myUAVTeam team = new myUAVTeam(this, name, uavTeamSize, new myPointf(tasks[0].loc));//always start at initial task's location
		if(exec.pa != null) {
			team.setTemplate(rndrTmpl, sphrRndrTmpl);
		}
		teams.add(team);
		return team;
	}//addNewTeam()
	
	public boolean getDrawBoats() {return getSimFlags(drawBoatsIDX);}
	public boolean getDebug() {return getSimFlags(debugSimIDX);}
	
	//called in exec.simMe - evolve visualization
	//deltaT should be in milliseconds, change
	public void visSimMe(long deltaT) {
		if(!getSimFlags(drawVisIDX)) {			return;}
		for(myUAVTeam team : teams) {
			team.moveUAVTeam(deltaT);
		}		
	}//visSimMe
		
	//animTimeMod is in seconds, time that has passed since last draw call
	public void drawMe(IRenderInterface pa, float animTimeMod, DESSimWindow win) {
		//draw all transit lanes
		//if(getSimFlags(drawTLanesIDX)) {
		for(myUAVTransitLane tl : transitLanes) {tl.drawEntity(pa, win, animTimeMod, getSimFlags(drawTLanesIDX), getSimFlags(dispTLnsLblsIDX));}
		holdingLane.drawEntity(pa, win, animTimeMod, getSimFlags(drawTLanesIDX), getSimFlags(dispTLnsLblsIDX));
		//}
		//draw all tasks
		//if(getSimFlags(drawTaskLocsIDX)) {
		for(myUAVTask task : tasks) {task.drawEntity(pa, win, animTimeMod, getSimFlags(drawTaskLocsIDX), getSimFlags(dispTaskLblsIDX));}
		//draw all UAV teams
		float delT = Math.min(animTimeMod, 1.0f);
		if(getSimFlags(drawUAVTeamsIDX)) {for(myUAVTeam team : teams) {team.drawEntity(pa, win, delT, true, getSimFlags(dispUAVLblsIDX));}}	
		
	}//drawMe
	
	//return idx of max value of total run time amongst all tasks or travel lanes
	private int findMaxIDX(myEntity[] ents, int idxToIgnore) {
		long maxVal = -1;
		int maxIDX = -1;
		for(int i=0;i<idxToIgnore;++i) {
			long val = ents[i].getTTLRunTime();
			if(maxVal < val) {maxVal = val;maxIDX = i;}
		}		
		return maxIDX;
	}
	//draw result information on right sidebar
	//UIRectBox is dims of enclosing box 
	public void drawResultBar(IRenderInterface pa, float yOff) {
		yOff-=4;
		float sbrMult = 1.2f, lbrMult = 1.5f;//offsets multiplier for barriers between contextual ui elements
		pa.pushMatState();
			int curTime = (Math.round(exec.getNowTime()/1000.0f));
			float yVal = 0;
			pa.setFill(255,255,0,255);	
			pa.showText("SIMULATION OUTPUT", 0, yVal);yVal += sbrMult * yOff;
			pa.setFill(255,255,255,255);
			pa.showText("Sim Time : " + String.format("%08d", curTime) + " secs ", 0, yVal);//yVal += yOff;
			pa.showText("Sim Clock Time : " + String.format("%04d", curTime/3600) + " : " + String.format("%02d", (curTime/60)%60 )+ " : " + String.format("%02d", (curTime%60)), 150, yVal);
//			yVal += yOff;
//			pa.showText("Wall Clock Time : " + String.format("%04d", curTime/3600) + " : " + String.format("%02d", (curTime/60)%60 )+ " : " + String.format("%02d", (curTime%60)), 0, yVal);
			yVal += lbrMult *yOff;
			//TEAM RES - summary
			int tmSize = teams.size();
			pa.setFill(255,155,20,255);
			pa.showText("Teams Summary : (for "+ String.format("%2d", tmSize) + " teams = "+String.format("%3d", (tmSize * uavTeamSize)) + " UAVs out of " + maxNumUAVs + " ttl)" , 0, yVal);yVal += yOff;
			pa.setFill(255,255,255,255);
			pa.showText("Team size : "+uavTeamSize , 0, yVal);yVal += yOff;
			//
			long ttlTask=0, ttlTravel=0, ttlQueue=0, ttlRun=0,ttlProcsDone=0;
			for(int i=0;i<tmSize;++i) {
				myUAVTeam tm = teams.get(i);
				ttlTask += tm.getTTLTaskTime();
				ttlTravel += tm.getTTLTravelTime();
				ttlQueue += tm.getTTLQueueTime();
				ttlRun += tm.getTTLRunTime()+tm.getCurTimeInProc();
				ttlProcsDone += tm.getTTLNumTeamsProc();
			}//	
			pa.showText("Procs Done : ", 0, yVal);pa.showText(""+String.format("%07d", ttlProcsDone), 90, yVal);yVal += yOff;
			pa.showText("TTL Work time : ", 0, yVal);pa.showText(""+String.format("%07d", ttlTask/1000) + " sec", 90, yVal);yVal += yOff;
			pa.showText("TTL Travel time : ", 0, yVal);pa.showText(""+String.format("%07d",ttlTravel/1000) + " sec",90, yVal);yVal += yOff;
			pa.showText("TTL Queue time : ", 0, yVal);pa.showText(""+String.format("%07d", ttlQueue/1000) + " sec", 90, yVal);yVal += yOff;
			pa.showText("TTL Uptime : ", 0, yVal);pa.showText(""+String.format("%07d", ttlRun/1000) + " sec", 90, yVal);yVal += lbrMult* yOff;
			//task res
			pa.setFill(255,88,255,255);
			pa.showText("Task Totals : (" + tasks.length+ " tasks) (red is max time so far)", 0, yVal);yVal +=  sbrMult *yOff;
			pa.setFill(255,255,255,255);

			int hLiteIDX = findMaxIDX(tasks,tasks.length-2);
			for(int i=0;i<tasks.length;++i) {		
				taskDesc td = tasks[i].td;
				pa.setFill(0,255,255,255);
				pa.showText(""+(i+1) +" : "+tasks[i].name,0, yVal);
				pa.setFill(255,255,255,255);
				pa.showText("Opt Size : " + td.optUAVTeamSize + " | Opt TTC : " 
							+ String.format("%4d",((int)(td.timeForOptToCmp/1000.0f)))+" s | StdDev : "+ String.format("%3.2f",td.stdDev),65, yVal);
				yVal += yOff; 
				pa.showText("#Teams Proc: " + String.format("%3d", tasks[i].getTTLNumTeamsProc()), 0, yVal);
				if(hLiteIDX==i) {pa.setFill(255,44,80,255);}
				pa.showText("TTL Task Time: " + String.format("%07d", tasks[i].getTTLRunTime()/1000) + " s",102,yVal);
				pa.setFill(255,255,255,255);
				yVal += sbrMult *yOff;
			}//for every task
			yVal += (lbrMult - sbrMult) *yOff;//offset by same amount as other groupings
			//transit lane res	
			
			hLiteIDX = findMaxIDX(transitLanes, transitLanes.length);		
			pa.setFill(255,150,99,255);
			pa.showText("Lane Totals : (" + transitLanes.length+ " Lanes) (red is max Q time (bottleneck))", 0, yVal);yVal += sbrMult *yOff;
			pa.setFill(255,255,255,255);
			for(int i=0;i<this.transitLanes.length;++i) {
				pa.setFill(0,255,255,255);
				pa.showText(""+(i+1) +" : "+transitLanes[i].name,0, yVal);yVal += yOff; 
				pa.setFill(255,255,255,255);	
				pa.showText("#Teams Proc: " + String.format("%3d", transitLanes[i].getTTLNumTeamsProc()), 0, yVal);
				if(hLiteIDX==i) {pa.setFill(255,44,80,255);	}
				pa.showText("TTL Lane Time: " + String.format("%07d", transitLanes[i].getTTLRunTime()/1000) + " s",152,yVal);yVal += yOff;
				pa.setFill(255,255,255,255);			
				pa.showText("Travel Time : "+String.format("%07d", transitLanes[i].getTTLTravelTime()/1000) + " s",0, yVal);//yVal += yOff; 
				pa.showText("Q Time : " + String.format("%07d", transitLanes[i].getTTLQueueTime()/1000) + " s",152, yVal);
				yVal += sbrMult *yOff;
			}//for every tl		
		pa.popMatState();	
	}//drawResultBar	

	//add an event to the FEL queue
	public void addEvent(myEvent resEv) {exec.addEvent(resEv);}
	
	/////////////////////
	// experimenting and reporting functions
	
	//test diminishing returns functionality for tasks with different optimal UAV team sizes.
	//build dummy task descriptions, use these to test how performance changes when sweeping power value
	public String testTaskTimeVals() {
		//build report base dir, in case it doesn't exist yet
		rptExpDir = baseDirStr + File.separatorChar + rptDateNowPrfx + "dir";
		exec.createRptDir(rptExpDir);		
		String taskResDir = rptExpDir + File.separatorChar + "Task_DistTest_Results";
		exec.createRptDir(taskResDir);
		int minSz = 2, maxSz = 9;
		double minP = .2, maxP=2.0, pwrIncr=.2;
		//build set of task descs, each will have 2,4,6 or 8 opt uav team size
		int numTestTaskDesc = 4;
		int[] optSzPerTask = new int[numTestTaskDesc];
		//per power array of results for a single task
		taskDesc[] tmpTasks = new taskDesc[numTestTaskDesc];
		for(int i=0;i<numTestTaskDesc;++i) {
			optSzPerTask[i] = (2+(i*2));
			tmpTasks[i] = new taskDesc(i, new myPointf(0,0,0), false, optSzPerTask[i], 100000, 0,30, 4, false);//team size for these guys will be ignored
			//file name for each task's output
			String finalResFNme = taskResDir + File.separatorChar + "Task_OptSz_"+tmpTasks[i].optUAVTeamSize+"_pwr_"+String.format("%2.2f", minP)+"_to_"+String.format("%2.2f", maxP)+"_dimRtnsTest";
			finalResFNme=finalResFNme.replace(".", "-");
			finalResFNme+=".csv";
			System.out.println("final res FNAME : " + finalResFNme);
			//getTaskPerfForNDataCSV returns string array with header
			exec.saveReport(finalResFNme, tmpTasks[i].getTaskPerfForNDataCSV(minSz,maxSz, minP, maxP, pwrIncr));	
		}			
		return taskResDir;
	}//testTaskTimeVals		
	
	//functions for experimental trials
	public void initTrials(int numTrials) {
		uavRes = new long[numTrials][];
		tlRes = new long[numTrials][][];
		taskRes = new long[numTrials][][];
	
		//build experimental output directory
		rptExpDir = baseDirStr + File.separatorChar + rptDateNowPrfx + "dir";
		exec.createRptDir(rptExpDir);		
	}//initTrials
	
	//end a round of experiments and save this round's results
	public void endExperiment(int curTrial, int numTrials, long expDurMSec) {
		//finish up individual experiment - save results at they are now, with appropriate timestamp, uav count, and other appropriate values for file name
		//record results
		//build base file name
		String bseFileName = rptExpDir + File.separatorChar + rptDateNowPrfx +"trl_"+curTrial+"_of_"+numTrials+"_dur_"+expDurMSec+"_Sz_"+uavTeamSize ;
		int idx = curTrial-1;
		//individual trial runs
		String[] uavResStrs = buildCSVUAVData();
		String[] tlResStrs = buildCSVTLData();
		String[] taskResStrs = buildCSVTaskData(2,9,0,1.0f);	
		//save values to use for aggregation arrays of arrays
		uavRes[idx] = buildUAVDataVals();		
		tlRes[idx] = buildTLDataVals();		
		taskRes[idx] = buildTaskDataVals();
		
		exec.saveReport(bseFileName + "_UAVReport.csv", uavResStrs);
		exec.saveReport(bseFileName + "_TransitLaneReport.csv", tlResStrs);
		exec.saveReport(bseFileName + "_TasksReport.csv", taskResStrs);	
	}//endExperiment
	
	//finish entire set of trials, save last trial's data and then calculate and save aggregate/average data
	public void endTrials(int curTrial, int numTrials, long expDurMSec) {
		endExperiment(curTrial,numTrials,expDurMSec);		
		//aggregate results and save to special files/directories
		String finalResDir = rptExpDir + File.separatorChar + "Exp_Final_Results";
		exec.createRptDir(finalResDir);
		
		//process aggregate aras of aras of data and build 
		String finalResFNmeBase = finalResDir + File.separatorChar + "FinalRes_Trls_"+numTrials+"_dur_"+expDurMSec+"_Sz_"+uavTeamSize ;

		exec.saveReport(finalResFNmeBase + "_UAVReport.csv", buildFinalResUAV());
		exec.saveReport(finalResFNmeBase + "_TransitLaneReport.csv", buildFinalResTL());
		exec.saveReport(finalResFNmeBase + "_TasksReport.csv", buildFinalResTask());	
		
		uavRes = new long[numTrials][];
		tlRes = new long[numTrials][][];
		taskRes = new long[numTrials][][];		
	}//	
	
	//build final results for UAV data - find total values in each string array, average over all arrays
	private String[] buildFinalResUAV() {
		ArrayList<String> res = new ArrayList<String>();
		int numTrials = uavRes.length;			
		float[] avgValsAllUAVs = new float[uavRes[0].length];
		res.add("Trial#, TTL Procs Done, TTL Work Time(ms), TTL Travel Time(ms), TTL Queue Time(ms), TTL Uptime(ms)");
		String resStr = "";
		
		for(int i=0;i<uavRes.length;++i) {//for each trial
			resStr = ""+(i+1);
			for(int j=0;j<uavRes[i].length;++j) {
				avgValsAllUAVs[j]+=uavRes[i][j];
				resStr += ","+uavRes[i][j];
			}	
			res.add(resStr);
		}		
		for(int j=0;j<avgValsAllUAVs.length;++j) {	avgValsAllUAVs[j]/=numTrials;	}	
		res.add(",Avg TTL Procs Done, Avg TTL Work Time(ms), Avg TTL Travel Time(ms), Avg TTL Queue Time(ms), Avg TTL Uptime(ms)");
		resStr = "";
		
		for(int j=0;j<avgValsAllUAVs.length;++j) {resStr += ","+String.format("%07d", (long)avgValsAllUAVs[j]);	}	
		res.add(resStr);
		return res.toArray(new String[0]);		
	}//buildFinalResUAV
	
	//build final results for transit lane data - find total values in each string array, average over all arrays
	private String[] buildFinalResTL() {
		ArrayList<String> res = new ArrayList<String>();
		int numTrials = tlRes.length;
		float[][] avgValsAllLanes = new float[tlRes[0].length][];
		for(int i=0;i<avgValsAllLanes.length;++i) {
			avgValsAllLanes[i] = new float[tlRes[0][0].length];
		}
		for(int i=0;i<tlRes.length;++i) {//for each trial
			for(int j=0;j<tlRes[i].length;++j) {//for each task,
				for(int k=0;k<tlRes[i][j].length;++k) {avgValsAllLanes[j][k]+=tlRes[i][j][k];}//for each value			
			}	
		}
		for(int j=0;j<avgValsAllLanes.length;++j) {for(int k=0;k<avgValsAllLanes[j].length;++k) {	avgValsAllLanes[j][k]/=numTrials;	}}	
		//by here we have all averaged values for all tasks (first idx) for each value(2nd idx)
		// timeVals[ttlNumTeamsProc]+"," + timeVals[ttlRunTime]+"," + timeVals[ttlTravelTime]+"," + timeVals[ttlQueueTime];
		res.add("Travel Lane IDX, TL Name, Avg # Teams Proc, Avg TTL Run Time(ms), Avg TTL Travel Time, Avg TTL Queue Time");
		for(int j=0;j<avgValsAllLanes.length;++j) {
			String resStr = ""+j+","+transitLanes[j].name;
			for(int k=0;k<avgValsAllLanes[j].length;++k) {	
				resStr += ","+String.format("%07d", (long)avgValsAllLanes[j][k]);	
			}
			res.add(resStr);
		}	
		return res.toArray(new String[0]);		
	}//buildFinalResUAV
	
	//build final results for Task data - find total values in each data array, average over all arrays, build per-task string array to hold csv-seped answers
	private String[] buildFinalResTask() {
		ArrayList<String> res = new ArrayList<String>();		
		float numTrials = 1.0f*taskRes.length;
		//array of avg values per task
		float[][] avgValsAllTasks = new float[taskRes[0].length][];
		for(int i=0;i<avgValsAllTasks.length;++i) {
			avgValsAllTasks[i] = new float[taskRes[0][0].length];
		}
		for(int i=0;i<taskRes.length;++i) {//for each trial
			for(int j=0;j<taskRes[i].length;++j) {//for each task,
				for(int k=0;k<taskRes[i][j].length;++k) {avgValsAllTasks[j][k]+=taskRes[i][j][k];}//for each value			
			}	
		}
		for(int j=0;j<avgValsAllTasks.length;++j) {for(int k=0;k<avgValsAllTasks[j].length;++k) {	avgValsAllTasks[j][k]/=numTrials;	}}	
		//by here we have all averaged values for all tasks (first idx) for each value(2nd idx)
		
		res.add("Task IDX, Task Name, Avg # Teams Proc, Avg TTL Task Time(ms)");
		for(int j=0;j<avgValsAllTasks.length;++j) {
			String resStr = ""+j+","+this.tasks[j].name;
			for(int k=0;k<avgValsAllTasks[j].length;++k) {	
				resStr += ","+String.format("%07d", (long)avgValsAllTasks[j][k]);	
			}
			res.add(resStr);
		}			
		return res.toArray(new String[0]);		
	}//buildFinalResUAV
	
	//different # of teams every time, so need to just have team totals for this trial
	private long[] buildUAVDataVals() {
		long[] res = new long[5];
		for(int i=0;i<teams.size();++i) {
			myUAVTeam tm = teams.get(i);
			res[0] += tm.getTTLNumTeamsProc();				
			res[1] += tm.getTTLTaskTime();							
			res[2] += tm.getTTLTravelTime();						
			res[3] += tm.getTTLQueueTime();						
			res[4] += tm.getTTLRunTime()+tm.getCurTimeInProc();		
		}//	
		return res;
	}//buildUAVDataVals
	
	//need per lane per value array of arrays of longs
	private long[][] buildTLDataVals() {
		long[][] res = new long[transitLanes.length][];
		for(int i=0;i<transitLanes.length;++i) {
			res[i] = new long[4];
			res[i][0] = transitLanes[i].getTTLNumTeamsProc();
			res[i][1] = transitLanes[i].getTTLRunTime();
			res[i][2] = transitLanes[i].getTTLTravelTime();
			res[i][3] = transitLanes[i].getTTLQueueTime();
		}	
		return res;
	}//buildTLDataVals
	
	//first idx is task, 2nd idx is value idx 
	//holds per task # of teams proced and ttl run time
	private long[][] buildTaskDataVals() {
		long[][] res = new long[tasks.length][];		
		for(int i=0;i<tasks.length;++i) {
			res[i]=new long[2];
			res[i][0] = tasks[i].getTTLNumTeamsProc();
			res[i][1] = tasks[i].getTTLRunTime();
		}		
		return res;
	}	

	private String[] buildCSVUAVData() {
		ArrayList<String> res = new ArrayList<String>();
		int tmSize = teams.size();
		res.add("Teams Summary, for "+ String.format("%2d", tmSize) + " teams, = "+String.format("%3d", (tmSize * uavTeamSize)) + " UAVs, out of " + maxNumUAVs + " ttl,");
		res.add(",,,,,,,");
		res.add(",,,,,,,");
		res.add(",,,,,,,");
		res.add("Team IDX,Procs Done, Work Time(ms), Travel Time(ms), Queue Time(ms), Uptime(ms)");
		//
		long ttlTask=0, ttlTravel=0, ttlQueue=0, ttlRun=0,ttlProcsDone=0, tmp;
		String line;
		for(int i=0;i<tmSize;++i) {
			line = ""+i;
			myUAVTeam tm = teams.get(i);
			tmp = tm.getTTLNumTeamsProc();						line+=","+tmp;		ttlProcsDone += tmp;			
			tmp = tm.getTTLTaskTime();							line+=","+tmp;		ttlTask += tmp;			
			tmp = tm.getTTLTravelTime();						line+=","+tmp;		ttlTravel += tmp;			
			tmp = tm.getTTLQueueTime();							line+=","+tmp;		ttlQueue += tmp;			
			tmp = tm.getTTLRunTime()+tm.getCurTimeInProc();		line+=","+tmp;		ttlRun += tmp;			
			res.add(line);			
		}//	
		res.add(",,,,,,,");
		res.add("__,TTL Procs Done, TTL Work Time(ms), TTL Travel Time(ms), TTL Queue Time(ms), TTL Uptime(ms)");
		res.add(","+String.format("%07d", ttlProcsDone)+","+String.format("%07d", ttlTask)+","+String.format("%07d",ttlTravel)+","+String.format("%07d", ttlQueue)+","+String.format("%07d", ttlRun));
		return res.toArray(new String[0]);		
	}//buildCSVUAVData	
	
	private String[] buildCSVTLData() {
		ArrayList<String> res = new ArrayList<String>();
		res.add(myUAVTransitLane.getTLResCSV_Hdr());
		for(int i=0;i<transitLanes.length;++i) {res.add(transitLanes[i].getTLResCSV()) ;}
		return res.toArray(new String[0]);
	}//buildCSVTLData	
	
	//build csv data for all tasks' descriptions performance metrics
	//include sweeping through sclFact to see the effect of changing stdev
	private String[] buildCSVTaskData(int minSize, int maxSize, float sclFact, float eqPwr) {
		ArrayList<String> res = new ArrayList<String>();
		//add header
		res.add(taskDesc.getTaskCompHeader_CSV(minSize, maxSize) + ", # Teams Proc, TTL Task Time(ms)");
		for(int i=0;i<tasks.length;++i) {res.add(tasks[i].td.getTaskCompTimeDataCSV(minSize, maxSize, sclFact,eqPwr)+", "+tasks[i].getTTLNumTeamsProc()+", "+tasks[i].getTTLRunTime());}		
		return res.toArray(new String[0]);
	}//buildCSVTaskData

		
	/** 
	 * convert from spherical coords to cartesian
	 * @param rad
	 * @param thet
	 * @param phi
	 * @return ara : norm, surface point == x,y,z of coords passed
	 */
	public myVectorf[] getXYZFromRThetPhi(double rad, double thet, double phi, double scaleZ) {
		double sinThet = Math.sin(thet);	
		myVectorf[] res = new myVectorf[2];
		res[1] = new myVectorf(sinThet * Math.cos(phi) * rad, sinThet * Math.sin(phi) * rad,Math.cos(thet)*rad*scaleZ);
		res[0] = myVectorf._normalize(res[1]);
		return res;
	}//
	
	/**
	 * builds a list of N regularly placed vertices and normals for a sphere of radius rad centered at ctr
	 */	
	private static final double twoPi = 2.0*Math.PI;
	public myVectorf[][] getRegularSphereList(float rad, int N, float scaleZ) {
		ArrayList<myVectorf[]> res = new ArrayList<myVectorf[]>();
		//choose 1 point per dArea, where dArea is area of sphere parsed into N equal portions
		double lclA = 4*Math.PI/N, lclD = Math.sqrt(lclA);
		int Mthet = (int) Math.round(Math.PI/lclD), Mphi;
		double dThet = Math.PI/Mthet, dPhi = lclA/dThet, thet, phi, twoPiOvDPhi = twoPi/dPhi;
		for(int i=0;i<Mthet;++i) {
			thet = dThet * (i + 0.5f);
			Mphi = (int) Math.round(twoPiOvDPhi * Math.sin(thet));
			for (int j=0;j<Mphi; ++j) { 
				phi = (twoPi*j)/Mphi;		
				res.add(getXYZFromRThetPhi(rad, thet, phi, scaleZ));
			}
		}
		return res.toArray(new myVectorf[0][]);
	}//getRegularSphereList	
	
	private static final double lcl_third = 1.0/3.0;
	//return a random position within a sphere
	public myVectorf getRandPosInSphere(double rad){ return getRandPosInSphere(rad, new myPointf());}
	public myVectorf getRandPosInSphere(double rad, myPointf ctr){
		myVectorf pos = new myVectorf();
		do{
			double u = ThreadLocalRandom.current().nextDouble(0,1), r = rad * Math.pow(u, lcl_third),
					cosTheta = ThreadLocalRandom.current().nextDouble(-1,1), sinTheta =  Math.sin(Math.acos(cosTheta)),
					phi = ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI);
			pos.set(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi),cosTheta);
			pos._mult(r);
			pos._add(ctr);
		} while (pos.z < ctr.z);
		return pos;
	}	
	
	
	//handle passed event - dispatch event to appropriate entity with appropriate values
	public myEvent handleEvent(myEvent _ev) {
		//dispOutput("\tHandling event : " + _ev.name);
		//handle event - event has following types : StartTask(0), FinishTask(1), EnterQueue(2), LeaveQueue(3);	
		switch (_ev.type) {
			case ArriveResource : {		return _ev.resource.arriveAtRes(_ev);}
			case LeaveResource : { 	return _ev.resource.leaveRes(_ev);	}
			//events to enter transit lane queues
			case EnterQueue : {		return ((myUAVTransitLane)_ev.resource).enterQueue(_ev);}
			//will always be a task - this will either instance an arriveresource event immediately 
			//or will set a flag that will cause leave resource to call a leave resource event on transit lane queue
			//on parent lane
			case ConsumerWaiting : {return ((myUAVTask)_ev.resource).consumerReady(_ev);}
			
			default : {
				exec.dispOutput("mySimulator", "handleEvent","\tmyDESSimulator::handleEvent : Unknown/unhandled event type :  " + _ev.type);
				return null;
			}
		}
	}//handleEvent
	
	public String toString() {
		String res = "\nDES Simulator :\n";
		res +="___________________________________________________________\n";
		res += "# of tasks made : " + tasks.length + " : \n";
		for(int i=0;i<tasks.length;++i) {res += tasks[i].toString()+"\n";}
		res +="___________________________________________________________\n";
		res += "# of transit lanes made : " + transitLanes.length + " : \n";
		for(int i=0;i<transitLanes.length;++i) {res += transitLanes[i].toString()+"\n";}
		res += "Holding lane : \n"+holdingLane.toString();
		res +="___________________________________________________________\n";
		res += "# of teams currently in play : " + teams.size() + " : \n";
		for(myUAVTeam team : teams) {res += team.toString()+"\n";}
		res +="___________________________________________________________\n";		
		return res;
	}

}//myDESSimulator


