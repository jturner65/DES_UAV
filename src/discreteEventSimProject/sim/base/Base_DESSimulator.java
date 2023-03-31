package discreteEventSimProject.sim.base;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.renderedObjs.base.Base_RenderObj;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.simulation.sim.Base_UISimulator;
import discreteEventSimProject.entities.base.Base_Entity;
import discreteEventSimProject.entities.consumers.UAV_Team;
import discreteEventSimProject.entities.resources.UAV_Task;
import discreteEventSimProject.entities.resources.UAV_TransitLane;
import discreteEventSimProject.events.DES_EventType;
import discreteEventSimProject.events.DES_Event;
import discreteEventSimProject.sim.task.DES_TaskDesc;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

/**
 * base class to manage specific simulation environment and event handling for DES simulation - owned by and called from sim executive
 * Specifics of descrete event simulation specified in child class
 * @author john
 */
public abstract class Base_DESSimulator extends Base_UISimulator {
	/**
	 * fixed structure holding task resources in this simulation
	 */
	protected UAV_Task[] tasks;	
	/**
	 * fixed structure holding transit lane resource queues in this simulation
	 */
	protected UAV_TransitLane[] transitLanes;
	/**
	 * variable structure holding collection of uavteams in play in this simulation
	 */
	protected ArrayList<UAV_Team> teams;		
	/**
	 * single transition lane from last task to first task, to provide recycling of teams
	 */
	protected UAV_TransitLane holdingLane;
	/**
	 * maximum # of UAV units available - can't put more than this many into play.
	 * must specify in constructor. Need to rebuild simulator if this changes
	 */
	protected final int maxNumUAVs;
	/**
	 * locations of tasks TODO make this random?  might be interesting to see how it impacts sim
	 */
	protected myPointf[] taskLocs;
	/**
	 * optimal team size for each task
	 */
	protected int[] taskOptSize;
	/**
	 * time required for optimal team size to complete task, in minutes.(mult by 60000 for milliseconds)
	 */
	protected float[] optTeamTTCMins;
	/**
	 * stdev multiplier (if != 0 then uses TTC as mean of gaussian with stdev == mult * optTeamSize/teamSize)
	 */
	protected float[] stdDevTTCMult;
	/**
	 * transit lane : idxs of tasks connecting each lane (parent, child)
	 */
	protected int[][] TL_taskIdxs;
	/**
	 * whether a particular task is a group task or not
	 */
	protected boolean [] isGrpTask;		
	/**
	 * size of UAV teams - modified by UI or command-line entry
	 */
	protected int uavTeamSize = 4;	
	
	////////////////////////
	// reporting stuff

	/**
	 * arrays to hold each trial's results for each type of metric.
	 * At end of experiment, save total results as avgs of specific results in these arrays
	 */
	/**
	 * per trial totals of all uavs' performance
	 */
	private long[][] uavRes;
	/**
	 * per trial, per task or per transit lane, per measured result value array of all exp data
	 */
	private long[][][] taskRes,tlRes;
	
	/**
	 * 
	 * @param _exec
	 * @param _maxNumUAVs
	 */
	public Base_DESSimulator(Base_DESSimExec _exec, String _name, int _maxNumUAVs, int _simLayoutToUse) {
		super(_exec, _name, _simLayoutToUse);
		maxNumUAVs = _maxNumUAVs;
	}//ctor
	
	/**
	 * called 1 time for all simulations from concrete class constructor
	 */
	protected final void initSim_Indiv() {
		initSim_Concrete();
		isGrpTask = getIsGroupAra();
	}//initOnce
	
	/** 
	 * Build sequential list of locations for tasks in a densely connected regular DAG grid and return the ArrayList of the
	 * and the 3D array of idxs for each point as well.
	 * 
	 * @param locIdxs predefined location idxs array for 3 d locations of each task. Populated here and returned
	 * @param size floating point size in the world of the square grid. The locations will be at +/- size/2 in each of x,y,z
	 * @return Array of points of all location points in dense cubic grid for tasks.  locIdxs will also be populated with 
	 * the idxs into this array for each task. 
	 */ 
	protected final myPointf[] buildDenseGridTaskLocs(int[][][] locIdxs, float size){
		int numPerSide = locIdxs.length;		
		float mult = size/(1.0f * (numPerSide-1));
		float ctr = size/2.0f;
		//dimension value for start and end location
		float locDim = ctr + .5f * mult;
		//start location and end location are outside the cube, at opposite corners
		myPointf stLoc = new myPointf(-locDim, -locDim, -locDim);
		myPointf endLoc = new myPointf(locDim, locDim, locDim);
		msgObj.dispInfoMessage("DES_Simulator("+name+")", "buildDenseGridTaskLocs", "Size : "+ size+ " St loc : "+stLoc.toStrBrf());
		
		ArrayList<myPointf> tmpListLocs = new ArrayList<myPointf>();
		int idx = 1;
		tmpListLocs.add(stLoc);

		int[][] locs_y = new int[numPerSide][];
		int[] locs_z = new int[numPerSide];
		//have a chance to make a task at each location
		for(int i=0; i<numPerSide; ++i) {	
			float xCoord = i*mult - ctr;
			locs_y = new int[numPerSide][];
			for(int j=0; j<numPerSide; ++j) {
				float yCoord = j*mult - ctr;
				locs_z = new int[numPerSide];
				for(int k=0; k<numPerSide; ++k) {
					float zCoord = k*mult - ctr;
					locs_z[k] = idx++; 
					tmpListLocs.add(new myPointf(xCoord, yCoord, zCoord));
				}
				locs_y[j]=locs_z;
			}			
			locIdxs[i]=locs_y;
		}
		tmpListLocs.add(endLoc);		
		return tmpListLocs.toArray(new myPointf[0]);		
	}//buildDenseGridTaskLocs
	
	/**
	 * Build a 2d array of begin->end point locations for each transit lane for the dense cube dag
	 * @param locIdxs 3d array of idxs in task location array for each point in dag
	 * @param lastObjIDX the idx in the task location array of the last object
	 * @return 2d array of every transit lane's beginning and ending point locations.
	 */
	protected final int[][] buildDenseGridTaskLanePts(int[][][] locIdxs, int lastObjIDX) {
		int xLast = locIdxs.length-1;
		int yLast = locIdxs[0].length-1;
		int zLast = locIdxs[0][0].length-1;
		// binomial theorem to calculate total # of lanes
		// connected to up to 7 forward nodes.
		int numIdxs = 8 + xLast *(3 + xLast *((3* 3) + (xLast * 7)));
		
		int[][] tLaneIdxs = new int[numIdxs][];
		int idx_TL = 0;
		//connectstart to first 4
		tLaneIdxs[idx_TL++] = new int[] {0,locIdxs[0][0][0]};					
		tLaneIdxs[idx_TL++] = new int[] {0,locIdxs[1][0][0]};					
		tLaneIdxs[idx_TL++] = new int[] {0,locIdxs[0][1][0]};					
		tLaneIdxs[idx_TL++] = new int[] {0,locIdxs[0][0][1]};				
		
		//make links to all +1 coords (7 per node not counting bounds)
		for(int i=0; i<xLast; ++i) {	
			int ip1 = i+1;
			for(int j=0; j<yLast; ++j) {
				int jp1 = j+1;
				for(int k=0; k<zLast; ++k) {
					int kp1 = k+1;
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[i][j][kp1]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[i][jp1][k]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[i][jp1][kp1]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[ip1][j][k]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[ip1][j][kp1]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[ip1][jp1][k]}; 
					tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][k],locIdxs[ip1][jp1][kp1]}; 
				}
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][zLast],locIdxs[i][jp1][zLast]}; 
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][zLast],locIdxs[ip1][j][zLast]}; 
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][j][zLast],locIdxs[ip1][jp1][zLast]}; 				
			}	
			tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][yLast][zLast],locIdxs[ip1][yLast][zLast]};			
		}
		for(int j=0; j<yLast; ++j) {
			int jp1 = j+1;
			for(int k=0; k<zLast; ++k) {
				int kp1 = k+1;
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][j][k],locIdxs[xLast][j][kp1]};	
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][j][k],locIdxs[xLast][jp1][k]};	
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][j][k],locIdxs[xLast][jp1][kp1]};	
			}
			tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][j][zLast],locIdxs[xLast][jp1][zLast]};	
		}
		for(int k=0; k<zLast; ++k) {
			int kp1 = k+1;
			for(int i=0;i<xLast;++i) {
				int ip1 = i+1;
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][yLast][k],locIdxs[i][yLast][kp1]};				
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][yLast][k],locIdxs[ip1][yLast][k]};				
				tLaneIdxs[idx_TL++] = new int[] {locIdxs[i][yLast][k],locIdxs[ip1][yLast][kp1]};				
			}
			tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][yLast][k],locIdxs[xLast][yLast][kp1]};	
		}		
		
		//connect last 4 task locs to exit
		tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][yLast][zLast], lastObjIDX};					
		tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][yLast][zLast-1], lastObjIDX};					
		tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast][yLast-1][zLast], lastObjIDX};					
		tLaneIdxs[idx_TL++] = new int[] {locIdxs[xLast-1][yLast][zLast], lastObjIDX};
		
		return tLaneIdxs;
	}

	/**
	 * Consume the newly set data values from the UI by way of the sim exec.
	 */
	@Override
	protected final void useUIDataUpdateVals() {
		//update local values from dataUpdate adapter		
	}

	/**
	 * 
	 * @param _tasks
	 * @param stIdx
	 * @param endIdx
	 * @param laneVel
	 * @param showMsg
	 * @return
	 */
	protected UAV_TransitLane buildTransitLane(UAV_Task[] _tasks, int stIdx, int endIdx, float laneVel, boolean showMsg) {
		String TLName =  "TransitLane_From_" + tasks[stIdx].name+"_to_"+ tasks[endIdx].name;
		//set location halfway between the two tasks this lane connects
		myPointf loc = new myPointf(_tasks[stIdx].loc, .5f, _tasks[endIdx].loc);
		UAV_TransitLane tl = new UAV_TransitLane(this, TLName, loc, 30.0f, laneVel);
		//set all parent and child tasks for each transit lane (always has 1 parent and 1 child)
		//add to transit
		tl.setTransitLaneConnections(_tasks[stIdx], _tasks[endIdx]);
		return tl;
	}//buildTransitLane
	
	/**
	 * build all tasks for this simulation
	 * @param tDesc
	 * @param showMsg
	 * @return
	 */
	protected UAV_Task[] buildTasks(DES_TaskDesc[] tDesc, boolean showMsg) {
		//set # of tasks based on passed location array tasks exist in simulation
		UAV_Task[] _ts = new UAV_Task[tDesc.length];				
		//first build tasks 
		for(int i=0;i<_ts.length;++i) {	_ts[i]=new UAV_Task(this, tDesc[i]);}
		if(showMsg) {
			msgObj.dispInfoMessage("DES_Simulator("+name+")", "buildTasks", "All " + _ts.length + " Tasks initialized.");	
		}
		return _ts;
	}//buildTasks
	
	/**
	 * build transit lanes after tasks have been built - set transit lanes to be child and parent task's parent and child, respectively
	 * @param _tasks
	 * @param _TL_taskIdxs
	 * @param _taskParentIDXs
	 * @param _taskChildIDXs
	 * @param showMsg
	 * @return
	 */
	protected UAV_TransitLane[] buildTransitLanes(UAV_Task[] _tasks, int[][] _TL_taskIdxs, ArrayList<Integer>[] _taskParentIDXs, ArrayList<Integer>[] _taskChildIDXs, boolean showMsg) {
		//set # transit lanes exist in simulation to be # of entries in parent/child task mapping
		UAV_TransitLane[] _tl = new UAV_TransitLane[_TL_taskIdxs.length];		
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
			msgObj.dispInfoMessage("DES_Simulator("+name+")", "buildTransitLanes","All " + _tl.length + " transitLanes initialized and connected to parents and children, along with holding lane.");
		}
		return _tl;
	}//buildTransitLanes
	
	/**
	 * Check if the passed resource name equals the holding lane's name
	 * @param _name
	 * @return
	 */
	public boolean resourceIsHoldingLane(String _name) {return _name.equals(holdingLane.name);}
	
	/**
	 * Check whether the holding lane's queue is empty
	 * @return
	 */
	public boolean holdingLaneQueueIsEmpty() {return holdingLane.queueIsEmpty();}
	
	/**
	 * Add an ArriveResource event for the passed team to the holding lane
	 * @param timeAhead
	 * @param team
	 * @param task
	 */
	public void addArriveHoldingLaneEvent(long timeAhead, UAV_Team team, UAV_Task task) {
		addEvent(new DES_Event(timeAhead,DES_EventType.ArriveResource, team, holdingLane, task));
	}
	
	/**
	 * these task descriptions will drive the task construction and consumption
	 * @param _tlocs
	 * @param _isGrp
	 * @param _tOptSize
	 * @param _optTeamTTCMins
	 * @param _stdDevTTCMult
	 * @param showMsg
	 * @return
	 */
	protected DES_TaskDesc[] buildTaskDesc(myPointf[] _tlocs, boolean[] _isGrp, int[] _tOptSize, float[] _optTeamTTCMins, float[] _stdDevTTCMult, boolean showMsg) {
		DES_TaskDesc[] tDesc = new DES_TaskDesc[_tlocs.length];
		//radius of rendered sphere
		float rad = 30.0f;
		for (int i=0;i<tDesc.length;++i) {tDesc[i] = new DES_TaskDesc(i, _tlocs[i], _isGrp[i], _tOptSize[i],_optTeamTTCMins[i]*60000,_stdDevTTCMult[i], (_isGrp[i]?2.0f:1.0f)*rad, uavTeamSize, showMsg);}
		return tDesc;
	}//buildTaskDesc
	
	/**
	 * Set parent and child transit lanes for each task
	 * @param _tasks
	 * @param _tl
	 * @param _taskParentIDXs
	 * @param _taskChildIDXs
	 * @param showMsg
	 */
	protected void setTaskParentChild(UAV_Task[] _tasks, UAV_TransitLane[] _tl, ArrayList<Integer>[] _taskParentIDXs, ArrayList<Integer>[] _taskChildIDXs, boolean showMsg) {
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
			msgObj.dispInfoMessage("DES_Simulator("+name+")", "setTaskParentChild","All " + _tasks.length + " tasks connected to parent and children transitlanes.");
		}
	}//setTaskParentChild	

	protected abstract void initSim_Concrete();
	protected abstract boolean[] getIsGroupAra();//determine which tasks are group tasks - sim dependent

	/**
	 * called on ever simulation reset - remakes entire sim, even on huge map is still very fast
	 * @param showMsg
	 */
	public void createSimAndLayout(boolean showMsg) {
		//reset world variables	
		//init arrays used to manage task parent and task child transit lane idx's
		//each task has 1 or more parents and 1 or more children - first task has no parents, last task has no children (special cases)
		//start named timer
		String timerName = this.name+"_createSimAndLayout";
		exec.setNamedTimerStartNow(timerName);
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] taskParentIDXs = new ArrayList[taskLocs.length];
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] taskChildIDXs = new ArrayList[taskLocs.length];
		//every task has 1 or more parents and 1 or more children, which are transit lanes
		//these arrays are populated when transit lanes are made and then used to set parents and children in tasks
		for(int i=0;i<taskParentIDXs.length;++i) {taskParentIDXs[i] = new ArrayList<Integer>();taskChildIDXs[i] = new ArrayList<Integer>();	}				
		//first build task descriptions
		DES_TaskDesc[] tDesc = buildTaskDesc(taskLocs,isGrpTask,taskOptSize,optTeamTTCMins,stdDevTTCMult, showMsg);
		//then build tasks from descriptions
		tasks = buildTasks(tDesc, showMsg);		
		//next build transit lanes, using centerpoint of tasks as location
		//set # transit lanes exist in simulation to be # of entries in parent/child task mapping
		transitLanes = buildTransitLanes(tasks, TL_taskIdxs, taskParentIDXs, taskChildIDXs, showMsg);
		//set parent and child Transit lanes for each task - some tasks have multiple parents or children.  if multiple children, need to send probability structure for 
		//cumulative prob dist
		setTaskParentChild(tasks, transitLanes, taskParentIDXs, taskChildIDXs, showMsg);
		exec.showTimeMsgNow("DES_Simulator","initSim","Millis to build map", timerName);		
		
		//teams is dynamic - depends on how large team size is specified to be and how many UAVs exist to draw from
		teams = new ArrayList<UAV_Team>();		
	}//initSim

	@Override
	protected void handlePrivFlagsDebugMode_Indiv(boolean val) {
		msgObj.dispDebugMessage("DES_Simulator("+name+")", "handlePrivFlagsDebugMode_Indiv", "Start DES_Simulator Debug, called from App-specific Debug flags with value "+ val +".");
		
		msgObj.dispDebugMessage("DES_Simulator("+name+")",  "handlePrivFlagsDebugMode_Indiv", "End DES_Simulator Debug, called from App-specific Debug flags with value "+ val +".");
	}//handlePrivFlagsDebugMode_Indiv


	@Override
	protected final boolean handlePrivSimFlags_Indiv(int idx, boolean val, boolean oldVal) {
		switch(idx){
			//case debugSimIDX 			: {break;}		//idx 0 is debug already anyway		
			case Base_DESSimExec.drawBoatsIDX			: {return true;}		//either draw boats or draw spheres for consumer UAV team members				
			case Base_DESSimExec.drawUAVTeamsIDX		: {return true;}		//draw UAV teams				
			case Base_DESSimExec.drawTaskLocsIDX		: {return true;}		//draw task locations at end of task lanes
			case Base_DESSimExec.drawTLanesIDX			: {return true;}		//draw task lanes between task locations								
			case Base_DESSimExec.dispTaskLblsIDX		: {return true;}
			case Base_DESSimExec.dispTLnsLblsIDX		: {return true;}
			case Base_DESSimExec.dispUAVLblsIDX			: {return true;}			
			default :{return handlePrivDesSimFlags_Indiv(idx, val, oldVal);
			}			
		}			
	}//handlePrivFlags_Indiv

	protected abstract boolean handlePrivDesSimFlags_Indiv(int idx, boolean val, boolean oldVal);

	/**
	 * Build event to represent initial process - this will be called when FEL is empty
	 * @param nowTime
	 * @return
	 */
	public final DES_Event buildInitialEvent(double nowTime) {
		long longNowTime = (long)MyMathUtils.floor(nowTime);
		UAV_Team newTeam = addNewTeam(longNowTime);
		//myEvent(long _ts, String _name, myEntity _c_ent, myEntity _r_ent)
		DES_Event ev = new DES_Event(longNowTime, DES_EventType.ArriveResource, newTeam, tasks[0], holdingLane);		
		return ev;
	}//buildInitialEvent
	
	/**
	 * Add a team to the simulation and put them in task 1 - can only occur when task 1 is unoccupied
	 * @param nowTime
	 * @return
	 */
	public final UAV_Team addNewTeam(long nowTime) {
		int nextTeamNum = teams.size()+1;
		if((nextTeamNum * uavTeamSize) > this.maxNumUAVs){//too many UAVs in play
			return null;
		}		
		String name = "UAVTeam_" + nextTeamNum + "_Sz_"+uavTeamSize;
		msgObj.dispInfoMessage("DES_Simulator("+name+")", "addNewTeam","Adding Team @TS : "+String.format("%08d", (int)nowTime)+" | Name of UAV Team : " + name + " Size of UAV Team "+uavTeamSize);
		UAV_Team team = new UAV_Team(this, name, uavTeamSize, new myPointf(tasks[0].loc));//always start at initial task's location
		if(exec.hasRenderInterface()) {			
			Base_RenderObj[][] rndrTmplAra = ((Base_DESSimExec) exec).getRenderTemplates();			
			team.setTemplate(rndrTmplAra[0], rndrTmplAra[1]);
		}
		team.initTeam();
		teams.add(team);
		return team;
	}//addNewTeam()
	
	public boolean getDrawBoats() {return getSimFlag(Base_DESSimExec.drawBoatsIDX);}
	
	/**
	 * @return the uavTeamSize
	 */
	public int getUavTeamSize() {return uavTeamSize;}
	/**
	 * Set the uavTeamSize
	 * @param _uavTeamSize
	 */
	public void setUavTeamSize(int _uavTeamSize) { uavTeamSize = _uavTeamSize;}
	
	/**
	 * called from Base_UISimExec - evolve visualization
	 * @param scaledMillisSinceLastFrame
	 */
	@Override
	public final void simStepVisualization(float scaledMillisSinceLastFrame) {
		for(UAV_Team team : teams) {
			team.moveUAVTeam(scaledMillisSinceLastFrame, exec.getTimeStep());
		}		
	}//visSimMe
		
	/**
	 * animTimeMod is in seconds, time that has passed since last draw call
	 * @param pa
	 * @param animTimeMod
	 * @param win
	 */
	@Override
	public final void drawMe(IRenderInterface ri, float animTimeMod, Base_DispWindow win) {
		//draw all transit lanes
		boolean drawLanes = getSimFlag(Base_DESSimExec.drawTLanesIDX);
		for(UAV_TransitLane tl : transitLanes) {						tl.drawEntity(ri, animTimeMod, drawLanes);}
		holdingLane.drawEntity(ri, animTimeMod, drawLanes);
		if(getSimFlag(Base_DESSimExec.dispTLnsLblsIDX)) {
			for(UAV_TransitLane tl : transitLanes) {					tl.dispEntityLabel(ri, win);}
			holdingLane.dispEntityLabel(ri, win);
		}
		//draw all tasks
		boolean drawTasks = getSimFlag(Base_DESSimExec.drawTaskLocsIDX);
		for(UAV_Task task : tasks) {									task.drawEntity(ri, animTimeMod, drawTasks);}
		if (getSimFlag(Base_DESSimExec.dispTaskLblsIDX)){for(UAV_Task task : tasks) {	task.dispEntityLabel(ri, win);}}
		
		//draw all UAV teams
		float delT = Math.min(animTimeMod, 1.0f);
		if(getSimFlag(Base_DESSimExec.drawUAVTeamsIDX)) {	for(UAV_Team team : teams) {team.drawEntity(ri, delT, true);}}	
		if(getSimFlag(Base_DESSimExec.dispUAVLblsIDX)) {	for(UAV_Team team : teams) {team.dispEntityLabel(ri, win);}}	
		
	}//drawMe
	
	/**
	 * return idx of max value of total run time amongst all tasks or travel lanes
	 * @param ents
	 * @param idxToIgnore
	 * @return
	 */
	private int findMaxIDX(Base_Entity[] ents, int idxToIgnore) {
		long maxVal = -1;
		int maxIDX = -1;
		for(int i=0;i<idxToIgnore;++i) {
			long val = ents[i].getTTLRunTime();
			if(maxVal < val) {maxVal = val;maxIDX = i;}
		}		
		return maxIDX;
	}
	/**
	 * draw sim-specific result information on right sidebar, if gui-based sim
	 * @param ri
	 * @param yVals float array holding : 
	 * 		idx 0 : start y value for text
	 * 		idx 1 : per-line y offset for grouped text
	 * 		idx 2 : per-line y offset for title-to-group text (small space)
	 * 		idx 3 : per-line y offset for text that is not grouped (slightly larger)
	 * @return next yValue to draw text at
	 */
	@Override
	protected final float drawResultBar_Indiv(IRenderInterface ri, float[] yVals) {
		ri.pushMatState();
			//TEAM RES - summary
			int tmSize = teams.size();
			ri.setFill(255,155,20,255);
			ri.showText("Teams Summary : (for "+ String.format("%2d", tmSize) + " teams = "+String.format("%3d", (tmSize * uavTeamSize)) + " UAVs out of " + maxNumUAVs + " ttl)" , 0, yVals[0]);yVals[0] += yVals[1];
			ri.setFill(255,255,255,255);
			ri.showText("Team size : "+uavTeamSize , 0, yVals[0]);yVals[0] += yVals[1];
			//
			long ttlTask=0, ttlTravel=0, ttlQueue=0, ttlRun=0,ttlProcsDone=0;
			for(int i=0;i<tmSize;++i) {
				UAV_Team tm = teams.get(i);
				ttlTask += tm.getTTLTaskTime();
				ttlTravel += tm.getTTLTravelTime();
				ttlQueue += tm.getTTLQueueTime();
				ttlRun += tm.getTTLRunTime()+tm.getCurTimeInProc();
				ttlProcsDone += tm.getTTLNumTeamsProc();
			}//	
			ri.showText("Procs Done : ", 0, yVals[0]);ri.showText(""+String.format("%07d", ttlProcsDone), 90, yVals[0]);yVals[0] += yVals[1];
			ri.showText("TTL Work time : ", 0, yVals[0]);ri.showText(""+String.format("%07d", ttlTask/1000) + " sec", 90, yVals[0]);yVals[0] += yVals[1];
			ri.showText("TTL Travel time : ", 0, yVals[0]);ri.showText(""+String.format("%07d",ttlTravel/1000) + " sec",90, yVals[0]);yVals[0] += yVals[1];
			ri.showText("TTL Queue time : ", 0, yVals[0]);ri.showText(""+String.format("%07d", ttlQueue/1000) + " sec", 90, yVals[0]);yVals[0] += yVals[1];
			ri.showText("TTL Uptime : ", 0, yVals[0]);ri.showText(""+String.format("%07d", ttlRun/1000) + " sec", 90, yVals[0]);yVals[0] += yVals[3];
			
			//task res
			ri.setFill(255,88,255,255);
			ri.showText("Task Totals : (" + tasks.length+ " tasks) (red is max time so far)", 0, yVals[0]);yVals[0] +=  yVals[2];
			ri.setFill(255,255,255,255);

			int hLiteIDX = findMaxIDX(tasks,tasks.length-2);
			for(int i=0;i<tasks.length;++i) {
				yVals[0] = tasks[i].drawResourceDescr(ri, hLiteIDX, i, 102, yVals[0], yVals[1]) + yVals[2];
			}//for every task
			yVals[0] += (yVals[3] - yVals[2]);//offset by same amount as other groupings
			
			//transit lane res
			ri.setFill(255,150,99,255);
			ri.showText("Lane Totals : (" + transitLanes.length+ " Lanes) (red is max Q time (bottleneck))", 0, yVals[0]);yVals[0] += yVals[2];
			ri.setFill(255,255,255,255);
			
			hLiteIDX = findMaxIDX(transitLanes, transitLanes.length);		
			for(int i=0;i<transitLanes.length;++i) {				
				yVals[0] = transitLanes[i].drawResourceDescr(ri, hLiteIDX, i, 152, yVals[0], yVals[1]) + yVals[2];
			}//for every tl		
		ri.popMatState();
		return yVals[0];
	}//drawResultBar	
	
	/**
	 * add an event to the FEL queue
	 * @param resEv
	 */
	public void addEvent(DES_Event resEv) {((Base_DESSimExec) exec).addEvent(resEv);}
	
	public int getNumUniqueTeams() {return ((Base_DESSimExec) exec).numUniqueTeams;}
	
	/////////////////////
	// experimenting and reporting functions
	
	/**
	 * test diminishing returns functionality for tasks with different optimal UAV team sizes. 
	 * Builds dummy task descriptions, uses these to test how performance changes when sweeping power value
	 * @return
	 */
	public String testTaskTimeVals() {
		//build report base dir, in case it doesn't exist yet
		String taskResDir = buildRptExpDir() + File.separatorChar + "Task_DistTest_Results";
		exec.createRptDir(taskResDir);
		int minSz = 2, maxSz = 9;
		double minP = .2, maxP=2.0, pwrIncr=.2;
		//build set of task descs, each will have 2,4,6 or 8 opt uav team size
		int numTestTaskDesc = 4;
		int[] optSzPerTask = new int[numTestTaskDesc];
		//per power array of results for a single task
		DES_TaskDesc[] tmpTasks = new DES_TaskDesc[numTestTaskDesc];
		for(int i=0;i<numTestTaskDesc;++i) {
			optSzPerTask[i] = (2+(i*2));
			tmpTasks[i] = new DES_TaskDesc(i, new myPointf(0,0,0), false, optSzPerTask[i], 100000, 0,30, 4, false);//team size for these guys will be ignored
			//file name for each task's output
			String finalResFNme = taskResDir + File.separatorChar + "Task_OptSz_"+tmpTasks[i].optUAVTeamSize+"_pwr_"+String.format("%2.2f", minP)+"_to_"+String.format("%2.2f", maxP)+"_dimRtnsTest";
			finalResFNme=finalResFNme.replace(".", "-");
			finalResFNme+=".csv";
			msgObj.dispInfoMessage("DES_Simulator("+name+")","testTaskTimeVals","final res FNAME : " + finalResFNme);
			//getTaskPerfForNDataCSV returns string array with header
			exec.saveReport(finalResFNme, tmpTasks[i].getTaskPerfForNDataCSV(minSz,maxSz, minP, maxP, pwrIncr));	
		}			
		return taskResDir;
	}//testTaskTimeVals		
	

	/**
	 * functions for experimental trials
	 * @param numTrials
	 */
	@Override
	protected final void initExperimentalTrials_Indiv(int numTrials) {
		//Build reporting aggregation data
		uavRes = new long[numTrials][];
		tlRes = new long[numTrials][][];
		taskRes = new long[numTrials][][];
	
	}//initTrials
	
	/**
	 * end a round of experiments and save this round's results
	 * @param bseFileName
	 * @param curTrial
	 */
	@Override
	protected final HashMap<String, String[]> endExperiment_Indiv(String bseFileName, int curTrial) {
		//finish up individual experiment - save results at they are now, with appropriate timestamp, uav count, and other appropriate values for file name
		bseFileName += "_Sz_"+uavTeamSize;
		int idx = curTrial-1;
		//save values to use for aggregation arrays of arrays
		uavRes[idx] = buildUAVDataVals();		
		tlRes[idx] = buildTLDataVals();		
		taskRes[idx] = buildTaskDataVals();
		
		HashMap<String, String[]> reportRes = new HashMap<String, String[]>();
		//individual trial runs		
		reportRes.put(bseFileName + "_UAVReport.csv", buildCSVUAVData());
		reportRes.put(bseFileName + "_TransitLaneReport.csv", buildCSVTLData());
		reportRes.put(bseFileName + "_TasksReport.csv", buildCSVTaskData(2,9,0,1.0f));
		
		return reportRes;
	}//endExperiment
	
	/**
	 * Implementation-specific finish entire set of trials, save last trial's data and then calculate and save aggregate/average data
	 * @param finalResFNmeBase
	 * @param numTrials
	 */
	@Override
	protected final HashMap<String, String[]> endTrials_Indiv(String finalResFNmeBase, int numTrials) {
		finalResFNmeBase += "_Sz_"+uavTeamSize;
		HashMap<String, String[]> reportRes = new HashMap<String, String[]>();
		reportRes.put(finalResFNmeBase + "_UAVReport.csv", buildFinalResUAV());
		reportRes.put(finalResFNmeBase + "_TransitLaneReport.csv", buildFinalResTL());
		reportRes.put(finalResFNmeBase + "_TasksReport.csv", buildFinalResTask());	
		//Reinit arrays of data
		uavRes = new long[numTrials][];
		tlRes = new long[numTrials][][];
		taskRes = new long[numTrials][][];
		return reportRes;
	}//	
	
	/**
	 * build final results for UAV data - find total values in each string array, average over all arrays
	 * @return
	 */
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
	
	/**
	 * build final results for transit lane data - find total values in each string array, average over all arrays
	 * @return
	 */
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
	
	/**
	 * build final results for Task data - find total values in each data array, average over all arrays, build per-task string array to hold csv-seped answers
	 * @return
	 */
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
	
	/**
	 * different # of teams every time, so need to just have team totals for this trial
	 * @return
	 */
	private long[] buildUAVDataVals() {
		long[] res = new long[5];
		for(int i=0;i<teams.size();++i) {
			UAV_Team tm = teams.get(i);
			res[0] += tm.getTTLNumTeamsProc();				
			res[1] += tm.getTTLTaskTime();							
			res[2] += tm.getTTLTravelTime();						
			res[3] += tm.getTTLQueueTime();						
			res[4] += tm.getTTLRunTime()+tm.getCurTimeInProc();		
		}//	
		return res;
	}//buildUAVDataVals
	
	/**
	 * need per lane per value array of arrays of longs
	 * @return
	 */
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
	
	/**
	 * first idx is task, 2nd idx is value idx  holds per task # of teams proced and ttl run time
	 * @return
	 */
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
			UAV_Team tm = teams.get(i);
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
		res.add(UAV_TransitLane.getTLResCSV_Hdr());
		for(int i=0;i<transitLanes.length;++i) {res.add(transitLanes[i].getTLResCSV()) ;}
		return res.toArray(new String[0]);
	}//buildCSVTLData	
	
	/**
	 * build csv data for all tasks' descriptions performance metrics. 
	 * Include sweeping through sclFact to see the effect of changing stdev
	 * @param minSize
	 * @param maxSize
	 * @param sclFact
	 * @param eqPwr
	 * @return
	 */
	private String[] buildCSVTaskData(int minSize, int maxSize, float sclFact, float eqPwr) {
		ArrayList<String> res = new ArrayList<String>();
		//add header
		res.add(DES_TaskDesc.getTaskCompHeader_CSV(minSize, maxSize) + ", # Teams Proc, TTL Task Time(ms)");
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
	public myVectorf[][] getRegularSphereList(float rad, int N, float scaleZ) {
		ArrayList<myVectorf[]> res = new ArrayList<myVectorf[]>();
		//choose 1 point per dArea, where dArea is area of sphere parsed into N equal portions
		double lclA = 4*Math.PI/N, lclD = Math.sqrt(lclA);
		int Mthet = (int) Math.round(Math.PI/lclD), Mphi;
		double dThet = MyMathUtils.PI/Mthet, dPhi = lclA/dThet, thet, phi, twoPiOvDPhi = MyMathUtils.TWO_PI/dPhi;
		for(int i=0;i<Mthet;++i) {
			thet = dThet * (i + 0.5f);
			Mphi = (int) Math.round(twoPiOvDPhi * Math.sin(thet));
			for (int j=0;j<Mphi; ++j) { 
				phi = (MyMathUtils.TWO_PI*j)/Mphi;		
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
					phi = ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI);
			pos.set(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi),cosTheta);
			pos._mult(r);
			pos._add(ctr);
		} while (pos.z < ctr.z);
		return pos;
	}	
	
	/**
	 * handle passed event - dispatch event to appropriate entity with appropriate values
	 * @param _ev
	 * @return
	 */
	public DES_Event handleEvent(DES_Event _ev) {
		//exec.dispOutput("DES_Simulator", "handleEvent","\tHandling event : " + _ev.name);
		switch (_ev.type) {
			case ArriveResource : {	return _ev.resource.arriveAtRes(_ev);}
			case LeaveResource : { 	return _ev.resource.leaveRes(_ev);	}
			//events to enter transit lane queues
			case EnterQueue : {		return ((UAV_TransitLane)_ev.resource).enterQueue(_ev);}
			//will always be a task - this will either instance an arriveresource event immediately 
			//or will set a flag that will cause leave resource to call a leave resource event on transit lane queue
			//on parent lane
			case ConsumerWaiting : {return ((UAV_Task)_ev.resource).consumerReady(_ev);}
			
			default : {
				msgObj.dispErrorMessage("DES_Simulator("+name+")", "handleEvent","\tmyDESSimulator::handleEvent : Unknown/unhandled event type :  " + _ev.type);
				return null;
			}
		}
	}//handleEvent
	
	
	/**
	 * Display output to console and screen if using graphical simulation
	 * @param className
	 * @param callMethod
	 * @param dataStr
	 */
	public void dispOutput(String className, String callMethod, String dataStr) {
		msgObj.dispInfoMessage(className, callMethod, dataStr);
	}
	
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
		for(UAV_Team team : teams) {res += team.toString()+"\n";}
		res +="___________________________________________________________\n";		
		return res;
	}

}//myDESSimulator


