package pkgCS6730Project1;

import java.util.*;

public class DESSimWindow extends myDispWindow {
	//simulation executive
	private mySimExecutive simExec;
	private mySimulator smplSim, cmplxSim;
	//motion
	///////////
	//ui vals
	//list of values for dropdown list of team size
	private String[] uavTeamSizeVals = new String[] {"2","3","4","5","6","7","8","9"};
	//idxs - need one per object
	//add ui objects for controlling the task stdevmult and other criteria?
	//for tasks : opt size, optsize mean TTC, stdDev Mult
	//for lanes : lane speed
	public final static int
		gIDX_FrameTimeScale 		= 0,
		gIDX_UAVTeamSize			= 1, 
		gIDX_ExpLength				= 2,			//length of time for experiment, in minutes
		gIDX_NumExpTrials			= 3; 
	//initial values - need one per object
	public float[] uiVals = new float[]{
			mySimExecutive.frameTimeScale,
			mySimulator.uavTeamSize - Integer.parseInt(uavTeamSizeVals[0]),					//since using a list here, need to do this - setting default in only one location
			720,																				//720 minutes == 12 hours default value
			1
			//
	};			//values of 8 ui-controlled quantities
	public final int numGUIObjs = uiVals.length;											//# of gui objects for ui	
	
	//display variables
	private float[] UIrectBox;	//box holding x,y,w,h values of black rectangle to hold UI sim display values
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[] menuDbgBtnNames = new String[] {"Verify PQ", "Verify FEL", "Show Sim", "Test Tasks"};//must have literals for every button or this is ignored
	public String[] menuFuncBtnNames = new String[] {"Use Main Sim", "Use Huge Sim"};//must have literals for every button or ignored
	
	//private child-class flags - window specific
	public static final int 
			debugAnimIDX 		= 0,						//debug
			resetSimIDX			= 1,						//whether or not to reset sim	
			drawVisIDX 			= 2,						//draw visualization - if false SIM exec and sim should ignore all processing/papplet stuff
			drawBoatsIDX		= 3,						//whether to draw animated boats or simple spheres for consumer UAVs
			drawUAVTeamsIDX		= 4,						//yes/no draw UAV teams
			drawTaskLocsIDX		= 5,						//yes/no draw task spheres
			drawTLanesIDX		= 6,						//yes/no draw transit lanes and queues
			dispTaskLblsIDX		= 7,						//show labels over tasks...
			dispTLnsLblsIDX		= 8,						//over transit lanes...
			dispUAVLblsIDX		= 9,						//and/or over teams			
			showSimValsIDX		= 10,						//yes/no show right sidebar menu of simulation outputs
			conductExpIDX		= 11,						//conduct experiment with current settings
			condUAVSweepExpIDX  = 12;						//sweep through UAV Team Sizes

	public static final int numPrivFlags = 13;
		
	public DESSimWindow(UAV_DESSim _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;
		trajFillClrCnst = UAV_DESSim.gui_DarkCyan;		
		trajStrkClrCnst = UAV_DESSim.gui_Cyan;
		super.initThisWin(_canDrawTraj, true, false);
	}//DancingBallWin
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of privModFlgIdxs
				"Visualization Debug","Resetting Simulation", "Drawing Vis", 
				"Drawing UAV Teams", "Drawing Task Locs", "Drawing Lanes", 
				"Showing Task Lbls", "Showing TLane Lbls", "Showing Team Lbls",
				"Drawing UAV Boats", "Showing Sim Outputs", "Experimenting",
				"Team SweepSize Experiment"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Enable Debug","Reset Simulation", "Render Visualization",
				"Draw UAV Teams", "Draw Task Locs", "Draw Transit Lanes",
				"Show Task Lbls", "Show TLane Lbls", "Show Team Lbls",
				"Drawing UAV Spheres", "Show Sim Outputs", "Conduct Experiment",
				"Conduct Team Sweep Experiment"
		};
		privModFlgIdxs = new int[]{
				debugAnimIDX, resetSimIDX, drawVisIDX, drawUAVTeamsIDX,	drawTaskLocsIDX,drawTLanesIDX,
				dispTaskLblsIDX, dispTLnsLblsIDX, dispUAVLblsIDX, drawBoatsIDX, showSimValsIDX,conductExpIDX,
				condUAVSweepExpIDX
		};
		numClickBools = privModFlgIdxs.length;	
		initPrivBtnRects(0,numClickBools);
	}//initAllPrivBtns
	//set labels of boolean buttons 
//	private void setLabel(int idx, String tLbl, String fLbl) {truePrivFlagNames[idx] = tLbl;falsePrivFlagNames[idx] = fLbl;}//	
	
	//reset initial flag states for each map type so that each sim world mirrors UI state
	private void resetDesFlags() {		
		simExec.setExecFlags(mySimExecutive.drawVisIDX, getPrivFlags(drawVisIDX));
		simExec.des.setSimFlags(mySimulator.drawBoatsIDX, getPrivFlags(drawBoatsIDX));				
		simExec.des.setSimFlags(mySimulator.drawUAVTeamsIDX, getPrivFlags(drawUAVTeamsIDX));				
		simExec.des.setSimFlags(mySimulator.drawTaskLocsIDX, getPrivFlags(drawTaskLocsIDX));				
		simExec.des.setSimFlags(mySimulator.drawTLanesIDX, getPrivFlags(drawTLanesIDX));				
		simExec.des.setSimFlags(mySimulator.dispTaskLblsIDX, getPrivFlags(dispTaskLblsIDX));				
		simExec.des.setSimFlags(mySimulator.dispTLnsLblsIDX, getPrivFlags(dispTLnsLblsIDX));				
		simExec.des.setSimFlags(mySimulator.dispUAVLblsIDX, getPrivFlags(dispUAVLblsIDX));				
	}//setInitFlags	

	private void setComplexSim() {		
		if(cmplxSim == null) {cmplxSim = new complexDesSim(simExec, 5000);	} 
		simExec.initSimWorld(cmplxSim, true);
		boolean showVis =  (pa != null);
		setPrivFlags(drawVisIDX, showVis);		
		setPrivFlags(drawUAVTeamsIDX, showVis);	
		setPrivFlags(drawBoatsIDX, showVis);	
		setPrivFlags(drawTaskLocsIDX, showVis);	
		setPrivFlags(drawTLanesIDX, false);	
		setPrivFlags(showSimValsIDX, false);			
		setPrivFlags(dispTaskLblsIDX, false);	
		setPrivFlags(dispTLnsLblsIDX, false);	
		setPrivFlags(dispUAVLblsIDX, false);	
		resetDesFlags();
		pa.setFlags(pa.runSim, false);
		
	}//initComplexSim
	
	private void setSimpleSim() {
		if(smplSim == null) {smplSim = new simpleDesSim(simExec, 100);	} 
		simExec.initSimWorld(smplSim, true);
		boolean showVis = (pa != null);
		setPrivFlags(drawVisIDX, showVis);		
		setPrivFlags(drawUAVTeamsIDX, showVis);	
		setPrivFlags(drawBoatsIDX, showVis);	
		setPrivFlags(drawTaskLocsIDX, showVis);	
		setPrivFlags(drawTLanesIDX, showVis);	
		setPrivFlags(showSimValsIDX, showVis);			
		setPrivFlags(dispTaskLblsIDX, showVis);	
		setPrivFlags(dispTLnsLblsIDX, showVis);	
		setPrivFlags(dispUAVLblsIDX, showVis);	
		resetDesFlags();
		//turn off simulation if running
		pa.setFlags(pa.runSim, false);
		
	}//initSimpleSim	
	
	@Override
	protected void initMe() {//all ui objects set by here
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//called once
		initPrivFlags(numPrivFlags);
		//initialize sim exec to simple world sim
		simExec = new mySimExecutive(pa);
		
		setSimpleSim();

		custMenuOffset = uiClkCoords[3];	//495	
		//UIrectBox = new float[] {0,0,1.25f*rectDim[0], rectDim[3]};
		float boxWidth = 1.2f*rectDim[0];
		UIrectBox = new float[] {rectDim[2]-boxWidth,0,boxWidth, rectDim[3]};
	}//initMe	
		
	@Override
	//set flag values and execute special functionality for this sequencer
	//skipKnown will allow settings to be reset if passed redundantly
	public void setPrivFlags(int idx, boolean val){	
		boolean curVal = getPrivFlags(idx);
		if(val == curVal){return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case debugAnimIDX 			: {
				simExec.setExecFlags(mySimExecutive.debugExecIDX,val);
				break;}
			case resetSimIDX			: {
				if(val) {simExec.initSimExec(true); addPrivBtnToClear(resetSimIDX);}break;}
			case drawVisIDX				:{
				simExec.setExecFlags(mySimExecutive.drawVisIDX, val);break;}
			case drawBoatsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(mySimulator.drawBoatsIDX, val);			break;}
			case drawUAVTeamsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(mySimulator.drawUAVTeamsIDX, val);		break;}
			case drawTaskLocsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(mySimulator.drawTaskLocsIDX, val);		break;}
			case drawTLanesIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(mySimulator.drawTLanesIDX, val);		break;}
			case dispTaskLblsIDX		: {				
				simExec.des.setSimFlags(mySimulator.dispTaskLblsIDX, val);		break;}
			case dispTLnsLblsIDX		: {				
				simExec.des.setSimFlags(mySimulator.dispTLnsLblsIDX, val);		break;}
			case dispUAVLblsIDX			: {				
				simExec.des.setSimFlags(mySimulator.dispUAVLblsIDX, val);		break;}				
			case showSimValsIDX			:{//show simulation values in window on right side				
				break;}
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], true);
					pa.setFlags(pa.runSim, true);
					addPrivBtnToClear(conductExpIDX);
				} 
				break;}
			case condUAVSweepExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], false);
					pa.setFlags(pa.runSim, true);
					addPrivBtnToClear(condUAVSweepExpIDX);
				} 
				break;}
			
			default:					{}
		}		
	}//setPrivFlags	
		
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		//pa.outStr2Scr("setupGUIObjsAras start");
		guiMinMaxModVals = new double [][]{
			{1.0f,10000.0f,1.0f},						//time scaling - 1 is real time, 1000 is 1000x speedup           		gIDX_FrameTimeScale 
			{0,uavTeamSizeVals.length-1, 1.0f},							//min/max team size
			{1.0f, 1440, 1.0f},								//experiment length
			{1.0f, 100, 1.0f}								//# of experimental trials
		};		//min max mod values for each modifiable UI comp	

		guiStVals = new double[]{
			uiVals[gIDX_FrameTimeScale],
			uiVals[gIDX_UAVTeamSize],
			uiVals[gIDX_ExpLength],
			uiVals[gIDX_NumExpTrials],
			
		};								//starting value
		
		guiObjNames = new String[]{
				"Sim Speed Multiplier",
				"UAV Team Size",
				"Experiment Duration",
				"# Experimental Trials"
		};								//name/label of component	
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{false, false, true},	
			{true, true, true},
			{true, false, true},
			{true, false, true}			
		};						//per-object  list of boolean flags
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
		
//		setupGUI_XtraObjs();
	}//setupGUIObjsAras
	
//	//setup UI object for song slider
//	private void setupGUI_XtraObjs() {
//		double stClkY = uiClkCoords[3], sizeClkY = 3*yOff;
//		guiObjs[songTransIDX] = new myGUIBar(pa, this, songTransIDX, "MP3 Transport for ", 
//				new myVector(0, stClkY,0), new myVector(uiClkCoords[2], stClkY+sizeClkY,0),
//				new double[] {0.0, 1.0,0.1}, 0.0, new boolean[]{false, false, true}, new double[]{xOff,yOff});	
//		
//		//setup space for ui interaction with song bar
//		stClkY += sizeClkY;				
//		uiClkCoords[3] = stClkY;
//	}

	
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		if(val != uiVals[UIidx]){//if value has changed...
			uiVals[UIidx] = val;
			switch(UIidx){		
			case gIDX_FrameTimeScale 			:{
				simExec.setTimeScale(val);
				break;}
			case gIDX_UAVTeamSize : {
				mySimulator.uavTeamSize = (int)val + Integer.parseInt(uavTeamSizeVals[0]);//add idx 0 as min size
				pa.outStr2Scr("uav team size desired is : " + mySimulator.uavTeamSize);
				//rebuild sim exec and sim environment whenever team size changes
				simExec.initSimExec(true);				
				break;}
			case gIDX_ExpLength : {//determines experiment length				
				break;}
			case gIDX_NumExpTrials : {//# of trials for experiments
				
			}

			default : {break;}
			}
		}
	}
	//if any ui values have a string behind them for display
	@Override
	protected String getUIListValStr(int UIidx, int validx) {			
		switch(UIidx){
			case gIDX_UAVTeamSize : {return uavTeamSizeVals[(validx % uavTeamSizeVals.length)];}
			default : {break;}
		}
		return "";
	}
	
	@Override
	public void initDrwnTrajIndiv(){}
	
//	public void setLights(){
//		pa.ambientLight(102, 102, 102);
//		pa.lightSpecular(204, 204, 204);
//		pa.directionalLight(180, 180, 180, 0, 1, -1);	
//	}	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv

	
	@Override
	//modAmtMillis is time passed per frame in milliseconds
	protected boolean simMe(float modAmtMillis) {//run simulation
		//pa.outStr2Scr("took : " + (pa.millis() - stVal) + " millis to simulate");
		boolean done = simExec.simMe(modAmtMillis);
		if(done) {setPrivFlags(conductExpIDX, false);}
		return done;	
	}//simMe
	
	//draw output values over screen in right sidebar
	private void drawSimOutputs(float modAmtMillis) {
		pa.pushMatrix();pa.pushStyle();
		//display current simulation variables
		simExec.des.drawResultBar(pa, UIrectBox,  yOff);
		pa.popStyle();pa.popMatrix();				
	}//drawSimOutputs
	
	private void drawClosedSimOutputs(float modAmtMillis) {
		pa.pushMatrix();pa.pushStyle();
		//put black Box for variable output display on right side of screen
		pa.translate(rectDim[2]-50,0,0);
		pa.setFill(new int[] {0,0,0,200});
		pa.rect(new float[] {0,0,50,rectDim[3]});
		pa.popStyle();pa.popMatrix();	
	}//drawClosedSimOutputs
	
	@Override
	//draw 2d constructs over 3d area on screen - draws behind menu section
	//modAmtMillis is in milliseconds
	protected void drawOnScreenStuff(float modAmtMillis) {
		pa.pushMatrix();pa.pushStyle();
		//move to upper right corner of sidebar menu - cannot draw over menu, use drawCustMenuObjs() instead 
		pa.translate(rectDim[0],0,0);
		if(getPrivFlags(showSimValsIDX)) {	drawSimOutputs(modAmtMillis);}
		else {								drawClosedSimOutputs(modAmtMillis);}
		pa.popStyle();pa.popMatrix();				
	}//drawOnScreenStuff
	
	@Override
	//animTimeMod is in seconds.
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//need to be here
//		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
		simExec.drawMe(animTimeMod);
	}//drawMe	
	
	
	//draw custom 2d constructs below interactive component of menu
	@Override
	public void drawCustMenuObjs(){
		pa.pushMatrix();				pa.pushStyle();		
		//all sub menu drawing within push mat call
		pa.translate(0,custMenuOffset+yOff);		
		//draw any custom menu stuff here
		pa.popStyle();					pa.popMatrix();		
	}//drawCustMenuObjs

	
	@Override
	protected void closeMe() {
		//things to do when swapping this window out for another window - release objects that take up a lot of memory, for example.
	}	
	
	@Override
	protected void showMe() {
		//things to do when swapping into this window - reinstance released objects, for example.
		pa.setMenuDbgBtnNames(menuDbgBtnNames);
		pa.setMenuFuncBtnNames(menuFuncBtnNames);
	}
	
	@Override
	//stopping simulation
	protected void stopMe() {
		System.out.println("Simulation Finished");	
	}
	
	//custom functions launched by UI input
	//if launching threads for custom functions, need to remove clearFuncBtnState call in function below and call clearFuncBtnState when thread ends
	private void custFunc0(){
		clearFuncBtnState(1,false);
		setSimpleSim();
		//clearFuncBtnState(0,false);
	}			
	private void custFunc1(){
		clearFuncBtnState(0,false);
		setComplexSim();
		//clearFuncBtnState(1,false);
	}	
	
	private void custFunc2(){	
		//custom function code here
		clearFuncBtnState(2,false);
	}			
	private void custFunc3(){	
		//custom function code here
		clearFuncBtnState(3,false);
	}			
	private void custFunc4(){	
		//custom function code here
		clearFuncBtnState(4,false);
	}		
	@Override
	public void clickFunction(int btnNum) {
		//pa.outStr2Scr("click cust function in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {	custFunc0();	break;}
			case 1 : {	custFunc1();	break;}
			case 2 : {	custFunc2();	break;}
			case 3 : {	custFunc3();	break;}
			case 4 : {	custFunc4();	break;}
			default : {break;}
		}	
	}		//only for display windows
	
	//debug function
	//if launching threads for debugging, need to remove clearDBGState call in function below and call clearDBGState when thread ends
	private void dbgFunc0(){		
		simExec.TEST_verifyPriorityQueueFunctionality();
		clearDBGBtnState(0,false);
	}	
	private void dbgFunc1(){		
		simExec.TEST_verifyFEL();
		clearDBGBtnState(1,false);
	}	
	private void dbgFunc2(){
		simExec.TEST_simulator();
		//dbg code here
		clearDBGBtnState(2,false);
	}	
	private void dbgFunc3(){	
		simExec.TEST_taskDists();
		//dbg code here
		clearDBGBtnState(3,false);
	}	

	@Override
	public void clickDebug(int btnNum){
		pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {	dbgFunc0();	break;}//verify priority queue functionality
			case 1 : {	dbgFunc1();	break;}//verify FEL pq integrity
			case 2 : {	dbgFunc2();	break;}
			case 3 : {	dbgFunc3();	break;}
			default : {break;}
		}		
	}//clickDebug
	
	@Override
	public void hndlFileLoadIndiv(String[] vals, int[] stIdx) {}
	@Override
	public List<String> hndlFileSaveIndiv() {List<String> res = new ArrayList<String>();return res;}
	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	}
	@Override
	protected myPoint getMsePtAs3DPt(int mouseX, int mouseY){return pa.P(mouseX,mouseY,0);}
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	//alt key pressed handles trajectory
	
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		boolean res = checkUIButtons(mouseX, mouseY);	
		return res;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		return res;}	
	@Override
	protected void hndlMouseRelIndiv() {	}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) { }
}//DESSimWindow

