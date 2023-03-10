package discreteEventSimProject.simExec.base;
import java.io.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import base_UI_Objects.renderedObjs.Boat_RenderObj;
import base_UI_Objects.renderedObjs.Sphere_RenderObj;
import base_UI_Objects.renderedObjs.base.Base_RenderObj;
import base_UI_Objects.renderedObjs.base.RenderObj_ClrPalette;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.simulation.simExec.Base_UISimExec;
import base_Utils_Objects.priorityQueue.myMinQueue;
import base_Utils_Objects.priorityQueue.base.myPriorityQueue;
import base_Utils_Objects.sim.Base_SimDataAdapter;
import discreteEventSimProject.events.DES_EventType;
import discreteEventSimProject.events.DES_Event;
import discreteEventSimProject.sim.base.DES_SimDataUpdater;
import discreteEventSimProject.sim.base.DES_Simulator;

/**
 * class to manage the functionality of the simulation executive
 * @author John Turner
 *
 */
public abstract class Base_DESSimExec extends Base_UISimExec{
	/**
	 * Priority queue holding future event list
	 */
	private myPriorityQueue<DES_Event> FEL;	
	
	//duration of an experiment = if not conducting an experiment, this is ignored, and sim will run forever
	//in millis
	private long expDurMSec;
	//# of experiments to conduct, to get multiple result sets
	private int numTrials = 1, curTrial = 1;
	/**
	 * size of teams used in trials
	 */
	private int uavTeamSizeTrial;
	private int dfltTeamSizeTrl;
	
	private final int minTrlUAVSz = 2, maxTrlUAVSz = 9;
	
	/**
	 * flags relevant to managed simulator execution - idxs in SimPrivStateFlags
	 */
	public static final int
					//debug is idx 0
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
	
	
	/////////////////////////
	// rendering stuff
	/**
	 * This was taken from boids project
	 */
	private String[] UAVTeamNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters"};
	private String[] UAVTypeNames = new String[]{"Boats"};
	public final int numUniqueTeams = UAVTeamNames.length;			
	//array of template objects to render
	//need individual array for each type of object, sphere (simplified) render object
	private Base_RenderObj[] rndrTmpl,//set depending on UI choice for complex rndr obj 
		boatRndrTmpl,
		sphrRndrTmpl;//simplified rndr obj (sphere)	
	private ConcurrentSkipListMap<String, Base_RenderObj[]> cmplxRndrTmpls;	
	
	// colors for render objects
	//divisors for stroke color from fill color
	private static final int
		sphereClrIDX = 0,
		boatClrIDX = 1;
	private static final int numTeamTypes = 2;
	private static final int[][] specClr = new int[][]{
		{255,255,255,255},		//sphere
		{255,255,255,255}};		//boat
	//Divide fill color for each type by these values for stroke
	private static final float[][] strokeScaleFact = new float[][]{
		{1.25f,0.42f,1.33f,0.95f,3.3f},    			//sphere    
		{1.25f,0.42f,1.33f,0.95f,3.3f}};   			//boat
		
	//scale all fill colors by this value for emissive value
	private static final float[] emitScaleFact = new float[] {0.7f, 0.9f};
	//stroke weight for sphere, boat
	private static final float[] strkWt = new float[] {1.0f, 1.0f};
	//shininess for sphere, boat
	private static final float[] shn = new float[] {5.0f,5.0f};
	
	//per type, per flock fill colors
	private static final int[][][] objFillColors = new int[][][]{
		{{110, 65, 30,255},	{30, 30, 30,255}, {130, 22, 10,255}, {22, 188, 110,255},	{22, 10, 130,255}},		//sphere
		{{110, 65, 30,255}, {20, 20, 20,255}, {130, 22, 10,255}, {22, 128, 50,255}, {22, 10, 150,255}}				//boats
	};	
		
	/**
	 * # of animation frames per animation cycle for animating objects
	 */	
	private final int numAnimFramesPerType = 90;
	
	
	/**
	 * pass null for command line version - define empty class called IRenderInterface
	 * @param _ri
	 * @param _msgObj
	 */
	public Base_DESSimExec(Base_DispWindow _win, String _name, int _numSims) {
		super(_win,_name, _numSims);
		if(ri==null) {msgObj.dispInfoMessage("DES_SimExec","ctor","Null IRenderInterface, assuming console only");}
	}//DES_SimExec ctor
	
	/**
	 * Implementation-specific ui-based sim exec initialization
	 */
	protected final void  initUISimExec_Indiv() {};	
	
	/**
	 * Initialize/build the rendered objects to use for the simulation rendering, if they exist
	 */
	@Override
	protected final void buildRenderObjs() {		
		if (hasRenderInterface()) {			
			//set up render object templates for different UAV Teams
			RenderObj_ClrPalette[] palettes = new RenderObj_ClrPalette[numTeamTypes];
			for (int i=0;i<palettes.length;++i) {palettes[i] =  buildRenderObjPalette(i);}			
			sphrRndrTmpl = new Sphere_RenderObj[numUniqueTeams];
			for(int i=0; i<numUniqueTeams; ++i){		sphrRndrTmpl[i] = new Sphere_RenderObj(ri, i, palettes[sphereClrIDX]);	}	
			cmplxRndrTmpls = new ConcurrentSkipListMap<String, Base_RenderObj[]> (); 
			boatRndrTmpl = new Boat_RenderObj[numUniqueTeams];
			for(int i=0; i<numUniqueTeams; ++i){	
				//build boat render object for each individual boat type
				boatRndrTmpl[i] = new Boat_RenderObj(ri, i, numAnimFramesPerType, palettes[boatClrIDX]);		
			}		
			cmplxRndrTmpls.put(UAVTypeNames[0], boatRndrTmpl);
			rndrTmpl = cmplxRndrTmpls.get(UAVTypeNames[0]);//start by rendering boats
		} else {
			rndrTmpl = null;
			sphrRndrTmpl = null;
		}
	}//initRenderObjs
	
	/**
	 * Clear out structures holding rendered objects for simulation, if any exist
	 */
	@Override
	protected final void clearRenderObjs() {
		rndrTmpl = null;
		sphrRndrTmpl = null;
	}
	
	/**
	 * Build render object color palette for passed type of render object
	 * @param _type index in predefined array of colors for specific render object type
	 * @return
	 */
	protected final RenderObj_ClrPalette buildRenderObjPalette(int _type) {
		RenderObj_ClrPalette palette = new RenderObj_ClrPalette(ri, numUniqueTeams);
		//set main color
		palette.setColor(-1, objFillColors[_type][0], objFillColors[_type][0], objFillColors[_type][0], specClr[_type], new int[]{0,0,0,0}, strkWt[_type], shn[_type]);
		//scale stroke color from fill color
		palette.scaleMainStrokeColor(strokeScaleFact[_type][0]);
		//set alpha after scaling
		palette.setMainStrokeColorAlpha(objFillColors[_type][0][3]);
		//set per-flock colors
		for(int i=0; i<numUniqueTeams; ++i){	
			palette.setColor(i, objFillColors[_type][i], objFillColors[_type][i], objFillColors[_type][i], specClr[_type], new int[]{0,0,0,0}, strkWt[_type], shn[_type]);
			//scale stroke colors
			palette.scaleInstanceStrokeColor(i, strokeScaleFact[_type][i]);
			//set alpha after scaling
			palette.setInstanceStrokeColorAlpha(i, objFillColors[_type][i][3]);
		}
		//scale all emissive values - scaled from fill color
		palette.scaleAllEmissiveColors(emitScaleFact[_type]);
		//disable ambient
		palette.disableAmbient();
		return palette;
	}//buildRenderObjPalette	
	
	/**
	 * Build appropriate simulation updater for simulation types that this sim exec manages
	 * TODO: move to implementation sim execs if different execs have different data layouts
	 * @return
	 */
	@Override
	public final Base_SimDataAdapter buildSimDataUpdater() {
		return new DES_SimDataUpdater(this);
	}
	
	/**
	 * Update any appropriate owning UI or interface components owning this simulation executive with values
	 * from # masterDataUpdate.
	 */
	@Override
	protected final void updateOwnerWithSimVals() {
		if(win==null) {return;}
		//TODO copy current state of masterDataUpdate to win.uiUpdateData, 
		//appropriately mapping the fields so that the UI can change to reflect simulation values changing
		
		
		
	}//updateOwnerWithSimVals
	
	
	
	public final void setSimUAVTeamSize(int _uavTeamSize) {
		dfltTeamSizeTrl = _uavTeamSize;		
	}
	
	/**
	 * initialize the simulation world's important values.
	 */
	protected final void initSimWorld_Indiv() {
		//dfltTeamSizeTrl = ((DES_Simulator) currSim).getUavTeamSize();
		//set team size back to original value before it would have been changed by sweeping trials
		((DES_Simulator) currSim).setUavTeamSize(dfltTeamSizeTrl);
		//reset all experiment values when sim world is changed - default behavior is sim will go forever, until stopped
		expDurMSec = Long.MAX_VALUE;
		numTrials = Integer.MAX_VALUE;
		curTrial = 1;
		execFlags.setConductExp(false);
		execFlags.setConductSweepExps(false);
	}//
	
	/**
	 * start or restart current simulation - whenever this.uavTeamSize is changed, this is called
	 * @param showMsg
	 */
	@Override
	protected final void resetSimExec_Indiv(boolean showMsg) {
		//rebuild FEL
		FEL = new myMinQueue<DES_Event>(50);
		//rebuild simulation environment
		((DES_Simulator) currSim).createSimAndLayout(showMsg);
	
	}//initSim
	
	
	@Override
	protected void handlePrivFlagsDebugMode_Indiv(boolean val) {
		msgObj.dispDebugMessage("DES_SimExec", "handlePrivFlagsDebugMode_Indiv", "Start DES_SimExec Debug, called from App-specific Debug flags with value "+ val +".");
		
		msgObj.dispDebugMessage("DES_SimExec",  "handlePrivFlagsDebugMode_Indiv", "End DES_SimExec Debug, called from App-specific Debug flags with value "+ val +".");
	}//handlePrivFlagsDebugMode_Indiv
	
	
	public void initializeTrials(int _mins, int _numTrials) {initializeTrials(_mins, _numTrials, true);}
	/**
	 * set up relevant variables for a set of experimental trials.
	 * if _useSetTeamSize == false, sweep from teamsize 2 to teamsize 9	
	 * @param _mins
	 * @param _numTrials
	 * @param _useSetTeamSize
	 */
	public void initializeTrials(int _mins, int _numTrials, boolean _useSetTeamSize) {
		expDurMSec = _mins * 60000;
		numTrials = _numTrials;
		curTrial = 1;
		if(!_useSetTeamSize) {
			//save current value of team size for when trials are finished
			dfltTeamSizeTrl = ((DES_Simulator) currSim).getUavTeamSize();
			uavTeamSizeTrial = minTrlUAVSz;
			((DES_Simulator) currSim).setUavTeamSize(uavTeamSizeTrial);
		}
		
		((DES_Simulator) currSim).initTrials(numTrials);
		startExperiment();
		execFlags.setConductExp(true);
		execFlags.setConductSweepExps(!_useSetTeamSize);		
	}//initializeExperiment	
	
	/**
	 * entry point for experiments, either window based or command line
	 */
	private void startExperiment() {
		//set/reset anything that needs to be addressed when starting a new trial
		resetSimExec(false); 		
	}//conductExp	
	
	/**
	 * end current experiment, if one is running. 
	 */
	private void endExperiment() {		
		((DES_Simulator) currSim).endExperiment(curTrial, numTrials, expDurMSec);	
	}//endExperiment
	
	/**
	 * call to end final experiment
	 */
	private void endAllTrials() {
		((DES_Simulator) currSim).endTrials(curTrial,numTrials,expDurMSec);	
		//if finished with all trials, reset values
		initSimWorld(false);
	}//endTrials	
	
	/**
	 *  end a set of trials for a specific team size, set to next team size, restart experimenting
	 */
	private void endTrialsForTmSz() {
		((DES_Simulator) currSim).endTrials(curTrial,numTrials,expDurMSec);	
		++uavTeamSizeTrial;
		((DES_Simulator) currSim).setUavTeamSize(uavTeamSizeTrial);
		curTrial = 1;
		((DES_Simulator) currSim).initTrials(numTrials);
		startExperiment();
		
	}//endTrialsForTmSz
	
	/**
	 * add an event to FEL - verifies not null
	 * @param resEv
	 */
	public void addEvent(DES_Event resEv) {if(resEv != null) {		FEL.insert(resEv);}	}//addEvent
	
	//public static int eventsProcced=0;
	//to simulate 12 hours is 12 * 3600000 == 43,200,000 milliseconds which would be approx 1,309,090 frames with multiplier set to 1.
	//			with speed up set to 1000, this would be 1309 frames ~= 40 seconds at 30 fps
	//private static int numExecs = 0;
	//advance current sim time by modAmtMillis * multiplier (for speed of simulation increase or decrease relative to realtime)
	//modAmtMillis is milliseconds elapsed since last frame  * multiplier (for speed of simulation increase or decrease relative to realtime)	
	/**
	 * Advance current simulation
	 * @param modAmtMillis is milliseconds elapsed since last frame
	 * @return whether sim is complete or not
	 */
	@Override
	protected final boolean stepUISimulation_Indiv(float modAmtMillis, float scaledMillisSinceLastFrame) {
		boolean expDoneNow = false;
		if(execFlags.getConductExp() && (nowTime >= expDurMSec)){//conducting experiments			
			//make sure to cover last run, up to expDurMSec
			nowTime = expDurMSec;
			expDoneNow = true;
		}		
		DES_Event ev = FEL.peekFirst();			//peek at first time stamp in FEL
		if(ev == null) {//no event waiting to process - start a UAV team in the process
			ev = ((DES_Simulator) currSim).buildInitialEvent(nowTime);
			addEvent(ev);
		}
		//pop simulation events from event list that have timestep less than now
		while ((ev != null) && (ev.getTimestamp() <= nowTime)) {	//"now" has evolved to be later than most recent event, so pop off events from PQ in order
			msgObj.dispInfoMessage("DES_SimExec","simMe","Frame Time : "+String.format("%08d", (int)nowTime)+" Frame Size : " +  ((int)frameTimeScale) + " | Current Event TS : " + ev.getTimestamp() + "| Ev Name : " + ev.name);
			//ev == null means no events on FEL
			ev = FEL.removeFirst();
			//eventsProcced++;
			DES_Event resEv = ((DES_Simulator) currSim).handleEvent(ev);
			addEvent(resEv);
			//peek at next event to check if it should be executed now
			ev = FEL.peekFirst();
		}	
		if(expDoneNow) {//we're done
			String nowDispTime = String.format("%08d", (long)nowTime);
			long expDurMin= (expDurMSec/60000), expDirHour = expDurMin/60;
			//either done with all trials or ready to move on to next trial
			if(curTrial >= numTrials) {//performed enough trials to check if done				
				if (!execFlags.getConductSweepExps()) {//done with all trials, and not sweeping
					msgObj.dispInfoMessage("DES_SimExec","simMe","NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials of experiments of duration : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
					endAllTrials();
					return true;//if done with experimental trials then stop sim
				} else {//finished with set of trials for current uav team size
					if(uavTeamSizeTrial >= maxTrlUAVSz) {//team size == max team size, then end and exit
						msgObj.dispInfoMessage("DES_SimExec","simMe","NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials for all team sizes, of experiments of duration : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
						endAllTrials();
						return true;//if done with experimental trials then stop sim						
					} else {//save current trials, increment team size, restart set of trials with new team size
						msgObj.dispInfoMessage("DES_SimExec","simMe","NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials for team size " +uavTeamSizeTrial +", each of duration  : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
						endTrialsForTmSz();
						return false;
					}
				}
			}
			//otherwise move on to next trial - reset environment and go again 
			msgObj.dispInfoMessage("DES_SimExec","simMe","NowTime : "+nowDispTime+ " | Finished with trial " + curTrial + " of " +numTrials +" total trials of experiments, each of duration  : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
			endExperiment();			
			++curTrial;		
			startExperiment();
		}	
		return false;
	}//stepSimulation
	
	/**
	 * Get current render templates (idx 0) and sphere render templates (idx 1)
	 * @return
	 */
	public Base_RenderObj[][] getRenderTemplates(){ return new Base_RenderObj[][] {rndrTmpl, sphrRndrTmpl};}
	
	/**
	 * draw simulation results for visualization. animTimeMod is in seconds
	 * @param animTimeMod
	 * @param win
	 */
	@Override
	public final void drawMe(float animTimeMod) {
		if(!getDoDrawViz()) {return;}//not drawing, return
		//call simulator to render sim world
		((DES_Simulator) currSim).drawMe(ri,animTimeMod* frameTimeScale, win);
	}//drawMe	
	
	/**
	 * Draw the right side info bar for the currently executing simulator
	 * @param txtHeightOff
	 * @param modAmtMillis
	 */
	@Override
	public final void drawRightSideInfoBar(float txtHeightOff, float modAmtMillis) {
		if(ri != null) {((DES_Simulator) currSim).drawResultBar(ri, txtHeightOff);}
	}
	
	/**
	 * split up newline-parsed strings into an array of strings, for display on screen
	 * @param str
	 * @return
	 */
	protected String[] getInfoStrAra(String str){return str.split("\n",-1);}
	
	/**
	 * Get number of simulation flags defined for the sims managed by this sim exec
	 */
	@Override
	public final int getNumSimFlags() { return numSimFlags;}

	
	///////////////////
	/// proc report output
	
	/**
	 * save string array of data to file filename
	 * @param fileName
	 * @param data
	 */
	public void saveReport(String fileName, String[] data) {
		//every string in data array needs to have appropriate CRLF appended
		  BufferedWriter outputWriter = null;
		  try {
			  outputWriter = new BufferedWriter(new FileWriter(fileName));
			  for (int i = 0; i < data.length; i++) {
			    // Maybe:
			    outputWriter.write(data[i]);
			    outputWriter.newLine();
			  }
			  outputWriter.flush();  
			  outputWriter.close(); 		
		  } catch (Exception e) {
			  msgObj.dispErrorMessage("DES_SimExec", "saveReport","Error saving report : "+ fileName+":\n"+e.getMessage());			  
		  } 		
	}//saveReport
	
	public boolean createRptDir(String dName) {
		File dir = new File(dName);	
		if (!dir.exists()) {
			msgObj.dispInfoMessage("DES_SimExec", "createRptDir","Create directory: " + dName);	
		    try{dir.mkdir();	    	return true;	    } 
		    catch(SecurityException se){
		    	 msgObj.dispErrorMessage("DES_SimExec","createRptDir", "Security Exception : Failed to create directory : " + dName+":\n"+se.getMessage());
		    	return false;	    }  
		    catch(Exception e){
		    	 msgObj.dispErrorMessage("DES_SimExec","createRptDir", "Failed to create directory : " + dName+":\n"+e.getMessage());
		    	return false;	    }  
		}
		return true;
	}//__createDir


	///////////////
	/////DEBUG AND TESTING
	/**
	 * test the distributions of the diminishing returns functionality for the task time to complete
	 */
	public void TEST_taskDists() {
		msgObj.dispInfoMessage("DES_SimExec","TEST_taskDists","\nTesting Task Diminishing returns functions.  Results will be saved to file.");
		String saveRes = ((DES_Simulator) currSim).testTaskTimeVals();
		msgObj.dispInfoMessage("DES_SimExec","TEST_taskDists","Test of Task Diminishing returns functions Complete.  Results saved to "+ saveRes);
	}	
	
	public void TEST_simulator() {
		String res = "\nSimulator Current State : \n";
		res += currSim.toString();
		msgObj.dispInfoMessage("DES_SimExec","TEST_simulator", res);
	}
	
	/**
	 * verify current priority queue's heapness
	 */
	public void TEST_verifyFEL() {
		String res = TEST_verifyFELHeap();
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","\nFEL Test 1 : Verifying FEL integrity and state.");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","\t"+res);
		if(null==FEL) {return;}
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","\nFEL Test 2 : Showing Raw contents of FEL heap : ");
		@SuppressWarnings("rawtypes")
		Comparable[] heap = FEL.getHeap();
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","heap idx: 0 elem is always null/unused ");
		for(int i=1;i<heap.length;++i) {
			if(null == heap[i]) {msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","heap idx: " + i +" elem is null/unused ");}
			else {msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","heap idx: " + i +" elem : " +((DES_Event)heap[i]).toStrBrf());}
		}
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","\nFEL Test 3 : Verifying FEL Contents and access.");
		TEST_PQShowElemsReAdd(FEL, "FEL");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyFEL","\nFEL Test 4 : Heap Sort of FEL (elements in descending order)."); 	
		TEST_heapSortAndShowContents(FEL, "FEL");		
	}
	
	private String TEST_verifyFELHeap() {
		//first determine FEL's current state
		String res = "";
		if(null==FEL) {
			res += "FEL is NULL ";
			return res;
		} 
		else if (FEL.isEmpty()) {res += "FEL is Empty ";} 
		else if (FEL.isFull()) { res += "FEL is Full ";}
		boolean isHeap = FEL.isHeap();
		res += " | Heapness of FEL " + (isHeap ? "is currently preserved. " : "IS NOT PRESERVED!!! ");
		int numElems = FEL.size();
		res += " | FEL currently has " + numElems + " elements.";
		return res;
	}//TEST_verifyFELHeap
	
	/**
	 * verify functionality of priority queue
	 */
	public void TEST_verifyPriorityQueueFunctionality() {
		int numTestElems = 20;
		myPriorityQueue<DES_Event> tmpPQ = new myMinQueue<DES_Event>(50);
		
		//build array of test data
		DES_Event[] tmpAra = TEST_buildTestAra(numTestElems);
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","_________________________________________________________________________");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","\nPQ Test 1 : adding random elements, removing first element in order\n");
		//add elements in tmpAra to tmpPQ
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ", true);
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","\nNow removing top elements :");
		TEST_dequeAndShowElems(tmpPQ,  "\t");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","Test 2 done : After removing elements in order, tmpPQ has : " + tmpPQ.size() + " Elements.\n");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","_________________________________________________________________________");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","\nPQ Test 2 : adding random elements, removing element in order of addition (randomly accessed in PQ)\n");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","Now testing remove elements in added order (not dequeing) -- i.e. removing random elements.");
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ", true);
		//remove requested element, then remove and display remaining elements in top-first order
		for (int i=0;i<tmpAra.length;++i) {
			msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","\tRemove Elem "+tmpAra[i]+" in tmpPQ of Size " +tmpPQ.size() + " : returned : " + tmpPQ.removeElem(tmpAra[i])+" : remaining elements :" );
			TEST_dequeAndShowElems(tmpPQ,  "\t\t");
			msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","");
			//re-add into tmpPQ
			for(int j=(i+1);j<tmpAra.length;++j) {		tmpPQ.insert(tmpAra[j]);	}
		}
		
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","Test 2 done : Finished removing random elements.  Heap now has : " + tmpPQ.size() + " elements.\n");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","_________________________________________________________________________");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","\nPQ Test 3 : adding random elements, sorting via HeapSort without corrupting pq\n");
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","Rebuilding pq");		
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ",  true);
		//test retrieving sorted list directly from heap - be sure to maintain heapness
		TEST_heapSortAndShowContents(tmpPQ, "tmpPQ");		
		msgObj.dispInfoMessage("DES_SimExec","TEST_verifyPriorityQueueFunctionality","Test 3 done : Finished testing heap sort.  Heap now has : " + tmpPQ.size() + " elements.\n");		
		
	}//verifyQueue
	//build array of test data
	private DES_Event[] TEST_buildTestAra(int numTestElems){
		DES_Event[] tmpAra = new DES_Event[numTestElems];
		for (int i =0;i<numTestElems;++i) {
			tmpAra[i] = new DES_Event(ThreadLocalRandom.current().nextInt(), DES_EventType.EnterQueue, null, null, null);//no entity attached to these events
		}
		return tmpAra;
	}
	//add test data to passed PQ
	private void TEST_buildPQWithAra(myPriorityQueue<DES_Event> tmpPQ, DES_Event[] tmpAra, String pqName, boolean showMsgs) {
		if(showMsgs) {msgObj.dispInfoMessage("DES_SimExec","TEST_buildPQWithAra","Before loading "+tmpAra.length+" events, "+pqName+" has : " + tmpPQ.size() + " Elements.");}
		for (int i =0;i<tmpAra.length;++i) {
			tmpPQ.insert(tmpAra[i]);
			if(showMsgs) {msgObj.dispInfoMessage("DES_SimExec","TEST_buildPQWithAra","\tAdding Elem # " + i + " : "+ tmpAra[i] + " to pq :"+pqName);}
		}
		if(showMsgs) {msgObj.dispInfoMessage("DES_SimExec","TEST_buildPQWithAra","\nAfter adding "+tmpAra.length+" events, "+pqName+" has : " + tmpPQ.size() + " Elements.");}		
	}//TEST_buildPQWithAra
	
	private void TEST_PQShowElemsReAdd(myPriorityQueue<DES_Event> tmpPQ, String lPfx) {
		DES_Event[] tmpAra = new DES_Event[tmpPQ.get_numElems()];	
		int idx=0;
		while (!tmpPQ.isEmpty()){
			tmpAra[idx] = tmpPQ.removeFirst();
			msgObj.dispInfoMessage("DES_SimExec","TEST_PQShowElemsReAdd","Elem # "+idx+" in " +lPfx + " of Size " +tmpAra.length + " : " + tmpAra[idx].toStrBrf() );
			idx++;
		}
		for (int i=0;i<idx;++i) {
			tmpPQ.insert(tmpAra[i]);
		}
	}//TEST_dequeAndShowElems
	
	private void TEST_dequeAndShowElems(myPriorityQueue<DES_Event> tmpPQ, String lPfx) {
		int num = tmpPQ.get_numElems();
		for (int i=0;i<num;++i) {
			msgObj.dispInfoMessage("DES_SimExec","TEST_dequeAndShowElems",lPfx + "Elem # "+i+" in tmpPQ of Size " +tmpPQ.size() + " : " + tmpPQ.removeFirst() );
		}
	}//TEST_dequeAndShowElems
	private void TEST_heapSortAndShowContents(myPriorityQueue<DES_Event> tmpPQ, String pqName) {
		@SuppressWarnings("rawtypes")
		Comparable[] tmpSortedAra = tmpPQ.getSortedElems();
		if(tmpSortedAra.length == 0) {
			msgObj.dispInfoMessage("DES_SimExec","TEST_heapSortAndShowContents","No Elements in "+pqName+" currently " );
			return;
		}
		msgObj.dispInfoMessage("DES_SimExec","TEST_heapSortAndShowContents","\nNow Displaying elements in "+pqName+" in heap sort order : ");
		for(int i=0;i<tmpSortedAra.length;++i) {
			msgObj.dispInfoMessage("DES_SimExec","TEST_heapSortAndShowContents","\tElem # " + i + " in sorted results : " + ((DES_Event)tmpSortedAra[i]).toStrBrf() );
		}
	}//TEST_heapSortAndShowContents

}//class DES_SimExec