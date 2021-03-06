package pkgCS6730Project1;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Utils_Objects.priorityQueue.myMinQueue;
import base_Utils_Objects.priorityQueue.base.myPriorityQueue;
import pkgCS6730Project1.events.EventType;
import pkgCS6730Project1.events.myEvent;

//class to manage the functionality of the simulation executive
public class mySimExecutive {
	//ref to owning application (if papplet) or null if console
	public IRenderInterface pa;	
	//simulator 
	public mySimulator des;	
	//Priority queue holding future event list
	private myPriorityQueue<myEvent> FEL;
	//time of simulation start, in millis
	private long simStartTime;
	//time exec built, in millis - used as offset for instant to provide smaller values for timestamp
	private final long execBuiltTime;
	//current simulation time in milliseconds from simStartTime - will be scaled by calling window to manage sim speed
	//set at start of every simMe call
	private float nowTime;
	//scaling time to speed up simulation == amount to multiply modAmtMillis by
	public static float frameTimeScale = 1000.0f;		
	//flags relevant to simulator executive execution
	private int[] execFlags;	
	public static final int
					debugExecIDX 	= 0,
					conductExpIDX	= 1,		//conduct an experiment
					condSweepExpIDX	= 2,		//conduct a set of experiments where the value of UAV Opt Size changes from 2 to 9 for each set of trials
					drawVisIDX		= 3;		//draw visualization - if false should ignore all processing/papplet stuff
	
	private static final int numExecFlags = 4;
	
	//duration of an experiment = if not conducting an experiment, this is ignored, and sim will run forever
	//in millis
	private long expDurMSec;
	//# of experiments to conduct, to get multiple result sets
	private int numTrials = 1, curTrial = 1;
	//size of teams used in trials
	private int uavTeamSizeTrial, dfltTeamSizeTrl = mySimulator.uavTeamSize;
	private final int minTrlUAVSz = 2, maxTrlUAVSz = 9;
	

	//pass null for command line version - define empty class called IRenderInterface
	public mySimExecutive(IRenderInterface _pa) {
		if(_pa != null) {pa= _pa;}
		else {dispOutput("Null IRenderInterface PApplet, assuming console only");}
		Instant now = Instant.now();
		execBuiltTime = now.toEpochMilli();//milliseconds since 1/1/1970 when this exec was built.
		initExecFlags();
	}//mySimExecutive ctor
	
	//initialize once - must be called by instancing method before the executive is executed
	public void initSimWorld(mySimulator _des, boolean showMSg) {
		des = _des;
		//set team size back to original value before it would ahve been changed by sweeping trials
		mySimulator.uavTeamSize = dfltTeamSizeTrl;
		//reset all experiment values when sim world is changed - default behavior is sim will go forever, until stopped
		expDurMSec = Long.MAX_VALUE;
		numTrials = Integer.MAX_VALUE;
		curTrial = 1;
		setExecFlags(conductExpIDX, false);
		setExecFlags(condSweepExpIDX, false);
		initSimExec(showMSg);
	}//
	
	//start or restart simulation - whenever this.uavTeamSize is changed, this is called
	public void initSimExec(boolean showMsg) {
		simStartTime = getCurTime();
		//rebuild FEL
		FEL = new myMinQueue<myEvent>(50);
		//rebuild simulation environment
		des.initSim(showMsg);
		//reset Now to be 0
		nowTime = 0;		
	}//initSim
	
	public void initializeTrials(int _mins, int _numTrials) {initializeTrials(_mins, _numTrials, true);}
	//set up relevant variables for a set of experimental trials
	//if _useSetTeamSize == false, sweep from teamsize 2 to teamsize 9	
	public void initializeTrials(int _mins, int _numTrials, boolean _useSetTeamSize) {
		expDurMSec = _mins * 60000;
		numTrials = _numTrials;
		curTrial = 1;
		if(!_useSetTeamSize) {
			//save current value of team size for when trials are finished
			dfltTeamSizeTrl = mySimulator.uavTeamSize;
			uavTeamSizeTrial = minTrlUAVSz;
			mySimulator.uavTeamSize = uavTeamSizeTrial;
		}
		
		des.initTrials(numTrials);
		startExperiment();
		setExecFlags(conductExpIDX, true);
		setExecFlags(condSweepExpIDX, !_useSetTeamSize);		
	}//initializeExperiment	

	//entry point for experiments, either window based or command line
	private void startExperiment() {
		//set/reset anything that needs to be addressed when starting a new trial
		initSimExec(false); 		
	}//conductExp	
	
	//end current experiment, if one is running. 
	private void endExperiment() {		
		des.endExperiment(curTrial, numTrials, expDurMSec);	
	}//endExperiment
	
	//call to end final experiment
	private void endAllTrials() {
		des.endTrials(curTrial,numTrials,expDurMSec);	
		//if finished with all trials, reset values
		initSimWorld(des, false);
	}//endTrials	
	
	// end a set of trials for a specific team size, set to next team size, restart experimenting
	private void endTrialsForTmSz() {
		des.endTrials(curTrial,numTrials,expDurMSec);	
		++uavTeamSizeTrial;
		mySimulator.uavTeamSize = uavTeamSizeTrial;
		curTrial = 1;
		des.initTrials(numTrials);
		startExperiment();
		
	}//endTrialsForTmSz
	
	protected void initExecFlags(){execFlags = new int[1 + numExecFlags/32]; for(int i = 0; i<numExecFlags; ++i){setExecFlags(i,false);}}
	public boolean getExecFlags(int idx){int bitLoc = 1<<(idx%32);return (execFlags[idx/32] & bitLoc) == bitLoc;}	
	public void setExecFlags(int idx, boolean val) {
		boolean curVal = getExecFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		execFlags[flIDX] = (val ?  execFlags[flIDX] | mask : execFlags[flIDX] & ~mask);
		switch(idx){
			case debugExecIDX 			: {
				des.setSimFlags(mySimulator.debugSimIDX, val);
				break;}
			case conductExpIDX			: {//if true then conducting an experiment.  reset simulation to beginning with current settings and then run until # of minutes have passed	
				break;}			
			case drawVisIDX				: {//draw visualization - if false should ignore all processing/papplet stuff
				des.setSimFlags(mySimulator.drawVisIDX, val);				
				break;}
		}			
	}//setExecFlags
	
	//add an event to FEL - verifies not null
	public void addEvent(myEvent resEv) {if(resEv != null) {		FEL.insert(resEv);}	}//addEvent
	
	//public static int eventsProcced=0;
	//to simulate 12 hours is 12 * 3600000 == 43,200,000 milliseconds which would be approx 1,309,090 frames with multiplier set to 1.
	//			with speed up set to 1000, this would be 1309 frames ~= 40 seconds at 30 fps
	//private static int numExecs = 0;
	//advance current sim time by modAmtMillis * multiplier (for speed of simulation increase or decrease relative to realtime)
	//modAmtMillis is milliseconds elapsed since last frame  * multiplier (for speed of simulation increase or decrease relative to realtime)
	public boolean simMe(float modAmtMillis) {
		float scaledModAmtMillis = modAmtMillis * frameTimeScale;// * _fixedScale;	//use _fixedScale to change to milliminutes?		
		nowTime += scaledModAmtMillis;
		//move objects in visualization - ignored if not using vis (checked in des)
		des.visSimMe((long)(Math.round(scaledModAmtMillis)));		

		boolean expDoneNow = false;
		if(getExecFlags(conductExpIDX) && (nowTime >= expDurMSec)){//conducting experiments			
			//make sure to cover last run, up to expDurMSec
			nowTime = expDurMSec;
			expDoneNow = true;
		}		
		myEvent ev = FEL.peekFirst();			//peek at first time stamp in FEL
		if(ev == null) {//no event waiting to process - start a UAV team in the process
			ev = des.buildInitialEvent(nowTime);
			addEvent(ev);
		}		
		//eventsProcced++;
		while ((ev != null) && (ev.getTimestamp() <= nowTime)) {	//"now" has evolved to be later than most recent event, so pop off events from PQ in order
			dispOutput("Frame Time : "+String.format("%08d", (int)nowTime)+" Frame Size : " +  ((int)frameTimeScale) + " | NowTime : Current Event TS : " + ev.getTimestamp() + "| Ev Name : " + ev.name);
			//ev == null means no events on FEL
			ev = FEL.removeFirst();
			//eventsProcced++;
			myEvent resEv = des.handleEvent(ev);
			addEvent(resEv);
			//peek at next event to check if it should be executed now
			ev = FEL.peekFirst();
		}	
		if(expDoneNow) {//we're done
			String nowDispTime = String.format("%08d", (long)nowTime);
			long expDurMin= (expDurMSec/60000), expDirHour = expDurMin/60;
			//either done with all trials or ready to move on to next trial
			if(curTrial >= numTrials) {//performed enough trials to check if done				
				if (!getExecFlags(condSweepExpIDX)) {//done with all trials, and not sweeping
					dispOutput("NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials of experiments of duration : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
					endAllTrials();
					return true;//if done with experimental trials then stop sim
				} else {//finished with set of trials for current uav team size
					if(uavTeamSizeTrial >= maxTrlUAVSz) {//team size == max team size, then end and exit
						dispOutput("NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials for all team sizes, of experiments of duration : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
						endAllTrials();
						return true;//if done with experimental trials then stop sim						
					} else {//save current trials, increment team size, restart set of trials with new team size
						dispOutput("NowTime : "+nowDispTime+ " | Finished with all " +numTrials +" trials for team size " +uavTeamSizeTrial +", each of duration  : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
						endTrialsForTmSz();
						return false;
					}
				}
			}
			//otherwise move on to next trial - reset environment and go again 
			dispOutput("NowTime : "+nowDispTime+ " | Finished with trial " + curTrial + " of " +numTrials +" total trials of experiments, each of duration  : " + expDurMSec +" ms -> " +expDurMin+ " min -> " + expDirHour + " hours");	
			endExperiment();			
			++curTrial;		
			startExperiment();
		}	
		return false;
	}//simMe
	
	//draw simulation results for visualization
	//animTimeMod is in seconds
	public void drawMe(float animTimeMod, DESSimWindow win) {
		if(!getExecFlags(drawVisIDX)) {return;}//not drawing, return
		//call simulator to render sim world
		des.drawMe(pa,animTimeMod* frameTimeScale, win);
	}//drawMe	
	
	//display message and time now
	protected void showTimeMsgNow(String _str, long stTime) {
		dispOutput(_str+" Time Now : "+(getCurTime() - stTime));
	}
	
	//get time from "start time"
	//1518700615691 is epoch instant @ 8:17 am 2/15/18
	//public long getCurTime() {return getCurTime(1518700615691L);}
	protected long getCurTime() {			
		Instant instant = Instant.now();
		long millis = instant.toEpochMilli() - execBuiltTime;//milliseconds since 1/1/1970, subtracting when this sim exec was built to keep millis low			
		return millis;
	}//getCurTime() 
	
	//returns a positive int value in millis of current wall time since sim start
	protected long getCurSimTime() {	return getCurTime() - simStartTime;}
	//returns current simNow time - now is appropriately scaled time of sim, and incremented at each simMe call based on render time
	protected float getNowTime() {	return nowTime;}
	//return a positive value in minutes from beginning of simulation
	protected float getCurSimTimeMinutes() { return getCurSimTime()/60000.0f;}	
	
	protected void setTimeScale(float _ts) {		frameTimeScale = _ts;	}
	protected float getTimeScale() {		return frameTimeScale;}	
	
	//split up newline-parsed strings into an array of strings, for display on screen
	protected String[] getInfoStrAra(String str){return str.split("\n",-1);}
	
	//will display output to console and screen if using graphical simulation
	public void dispOutput(String str) {
		if((null == pa) || (!getExecFlags(drawVisIDX))) {
			System.out.println(str);
		} else {
			pa.outStr2Scr(str,false);
		}		
	}//dispOutput
	//will return working directory 
	public String getCWD() {
		Path currentRelativePath = Paths.get("");
		return currentRelativePath.toAbsolutePath().toString();	
	}// getCWD()
	
	
	///////////////////
	/// proc report output
	
	//save string array of data to file filename
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
			  dispOutput("Error saving report : "+ fileName+":\n"+e.getMessage());			  
		  } 		
	}//saveReport
	
	public boolean createRptDir(String dName) {
		File dir = new File(dName);	
		if (!dir.exists()) {
			dispOutput("Create directory: " + dName);	
		    try{dir.mkdir();	    	return true;	    } 
		    catch(SecurityException se){
		    	dispOutput("failed to create directory : " + dName+":\n"+se.getMessage());
		    	return false;	    }  
		}
		return true;
	}//__createDir


	///////////////
	/////DEBUG AND TESTING
	//test the distributions of the diminishing returns functionality for the task time to complete
	public void TEST_taskDists() {
		dispOutput("\nTesting Task Diminishing returns functions.  Results will be saved to file.");
		String saveRes = des.testTaskTimeVals();
		dispOutput("Test of Task Diminishing returns functions Complete.  Results saved to "+ saveRes);
	}	
	
	public void TEST_simulator() {
		String res = "\nSimulator Current State : \n";
		res += des.toString();
		dispOutput(res);
	}
	
	//verify current priority queue's heapness
	public void TEST_verifyFEL() {
		String res = TEST_verifyFELHeap();
		dispOutput("\nFEL Test 1 : Verifying FEL integrity and state.");
		dispOutput("\t"+res);
		if(null==FEL) {return;}
		dispOutput("\nFEL Test 2 : Showing Raw contents of FEL heap : ");
		@SuppressWarnings("rawtypes")
		Comparable[] heap = FEL.getHeap();
		dispOutput("heap idx: 0 elem is always null/unused ");
		for(int i=1;i<heap.length;++i) {
			if(null == heap[i]) {dispOutput("heap idx: " + i +" elem is null/unused ");}
			else {dispOutput("heap idx: " + i +" elem : " +((myEvent)heap[i]).toStrBrf());}
		}
		dispOutput("\nFEL Test 3 : Verifying FEL Contents and access.");
		TEST_PQShowElemsReAdd(FEL, "FEL");
		dispOutput("\nFEL Test 4 : Heap Sort of FEL (elements in descending order)."); 	
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
	
	//verify functionality of priority queue
	public void TEST_verifyPriorityQueueFunctionality() {
		int numTestElems = 20;
		myPriorityQueue<myEvent> tmpPQ = new myMinQueue<myEvent>(50);
		
		//build array of test data
		myEvent[] tmpAra = TEST_buildTestAra(numTestElems);
		dispOutput("_________________________________________________________________________");
		dispOutput("\nPQ Test 1 : adding random elements, removing first element in order\n");
		//add elements in tmpAra to tmpPQ
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ", true);
		dispOutput("\nNow removing top elements :");
		TEST_dequeAndShowElems(tmpPQ,  "\t");
		dispOutput("");
		dispOutput("Test 2 done : After removing elements in order, tmpPQ has : " + tmpPQ.size() + " Elements.\n");
		dispOutput("_________________________________________________________________________");
		dispOutput("\nPQ Test 2 : adding random elements, removing element in order of addition (randomly accessed in PQ)\n");
		dispOutput("Now testing remove elements in added order (not dequeing) -- i.e. removing random elements.");
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ", true);
		//remove requested element, then remove and display remaining elements in top-first order
		for (int i=0;i<tmpAra.length;++i) {
			dispOutput("\tRemove Elem "+tmpAra[i]+" in tmpPQ of Size " +tmpPQ.size() + " : returned : " + tmpPQ.removeElem(tmpAra[i])+" : remaining elements :" );
			TEST_dequeAndShowElems(tmpPQ,  "\t\t");
			dispOutput("");
			//re-add into tmpPQ
			for(int j=(i+1);j<tmpAra.length;++j) {		tmpPQ.insert(tmpAra[j]);	}
		}
		
		dispOutput("Test 2 done : Finished removing random elements.  Heap now has : " + tmpPQ.size() + " elements.\n");
		dispOutput("_________________________________________________________________________");
		dispOutput("\nPQ Test 3 : adding random elements, sorting via HeapSort without corrupting pq\n");
		dispOutput("Rebuilding pq");		
		TEST_buildPQWithAra(tmpPQ, tmpAra, "tmpPQ",  true);
		//test retrieving sorted list directly from heap - be sure to maintain heapness
		TEST_heapSortAndShowContents(tmpPQ, "tmpPQ");		
		dispOutput("Test 3 done : Finished testing heap sort.  Heap now has : " + tmpPQ.size() + " elements.\n");		
		
	}//verifyQueue
	//build array of test data
	private myEvent[] TEST_buildTestAra(int numTestElems){
		myEvent[] tmpAra = new myEvent[numTestElems];
		for (int i =0;i<numTestElems;++i) {
			tmpAra[i] = new myEvent(ThreadLocalRandom.current().nextInt(), EventType.EnterQueue, null, null, null);//no entity attached to these events
		}
		return tmpAra;
	}
	//add test data to passed PQ
	private void TEST_buildPQWithAra(myPriorityQueue<myEvent> tmpPQ, myEvent[] tmpAra, String pqName, boolean showMsgs) {
		if(showMsgs) {dispOutput("Before loading "+tmpAra.length+" events, "+pqName+" has : " + tmpPQ.size() + " Elements.");}
		for (int i =0;i<tmpAra.length;++i) {
			tmpPQ.insert(tmpAra[i]);
			if(showMsgs) {dispOutput("\tAdding Elem # " + i + " : "+ tmpAra[i] + " to pq :"+pqName);}
		}
		if(showMsgs) {dispOutput("\nAfter adding "+tmpAra.length+" events, "+pqName+" has : " + tmpPQ.size() + " Elements.");}		
	}//TEST_buildPQWithAra
	
	private void TEST_PQShowElemsReAdd(myPriorityQueue<myEvent> tmpPQ, String lPfx) {
		myEvent[] tmpAra = new myEvent[tmpPQ.get_numElems()];	
		int idx=0;
		while (!tmpPQ.isEmpty()){
			tmpAra[idx] = tmpPQ.removeFirst();
			dispOutput("Elem # "+idx+" in " +lPfx + " of Size " +tmpAra.length + " : " + tmpAra[idx].toStrBrf() );
			idx++;
		}
		for (int i=0;i<idx;++i) {
			tmpPQ.insert(tmpAra[i]);
		}
	}//TEST_dequeAndShowElems
	
	private void TEST_dequeAndShowElems(myPriorityQueue<myEvent> tmpPQ, String lPfx) {
		int num = tmpPQ.get_numElems();
		for (int i=0;i<num;++i) {
			dispOutput(lPfx + "Elem # "+i+" in tmpPQ of Size " +tmpPQ.size() + " : " + tmpPQ.removeFirst() );
		}
	}//TEST_dequeAndShowElems
	private void TEST_heapSortAndShowContents(myPriorityQueue<myEvent> tmpPQ, String pqName) {
		@SuppressWarnings("rawtypes")
		Comparable[] tmpSortedAra = tmpPQ.getSortedElems();
		if(tmpSortedAra.length == 0) {
			dispOutput("No Elements in "+pqName+" currently " );
			return;
		}
		dispOutput("\nNow Displaying elements in "+pqName+" in heap sort order : ");
		for(int i=0;i<tmpSortedAra.length;++i) {
			dispOutput("\tElem # " + i + " in sorted results : " + ((myEvent)tmpSortedAra[i]).toStrBrf() );
		}
	}//TEST_heapSortAndShowContents

}//class mySimExecutive
//ENUMS


