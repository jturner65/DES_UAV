package discreteEventSimProject.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Utils_Objects.tools.flags.Base_BoolFlags;
import discreteEventSimProject.sim.DES_SimExec;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.ui.DES_UIDataUpdater;

public abstract class Base_DESWindow extends Base_DispWindow {
	//simulation executive
	protected DES_SimExec simExec;
	protected DES_Simulator smplSim, cmplxSim;
	
	protected DES_Simulator[] sims;
	protected DES_Simulator currSim;
	
	public static final int maxSimLayouts = 5;
	//motion
	///////////
	//ui vals

	public final static int
		gIDX_LayoutToUse			= 0, 
		gIDX_FrameTimeScale 		= 1,
		gIDX_UAVTeamSize			= 2, 
		gIDX_ExpLength				= 3,			//length of time for experiment, in minutes
		gIDX_NumExpTrials			= 4;
		
	/**
	 * Number of gui objects defined in base window. Subsequent IDXs in child class should start here
	 */
	protected static final int numBaseGUIObjs = 5;		
	/////////
	//custom debug/function ui button names -empty will do nothing
	
	/**
	 * private child-class flags - window specific
	 */
	public static final int 
			//debugAnimIDX 		= 0,						//debug
			resetSimIDX			= 1,						//whether or not to reset sim	
			drawVisIDX 			= 2,						//draw visualization - if false SIM exec and sim should ignore all processing/papplet stuff
			drawBoatsIDX		= 3,						//whether to draw animated boats or simple spheres for consumer UAVs
			drawUAVTeamsIDX		= 4,						//yes/no draw UAV teams
			drawTaskLocsIDX		= 5,						//yes/no draw task spheres
			drawTLanesIDX		= 6,						//yes/no draw transit lanes and queues
			dispTaskLblsIDX		= 7,						//show labels over tasks...
			dispTLnsLblsIDX		= 8,						//over transit lanes...
			dispUAVLblsIDX		= 9,						//and/or over teams			
			conductExpIDX		= 10,						//conduct experiment with current settings
			condUAVSweepExpIDX  = 11;						//sweep through UAV Team Sizes

	/**
	 * Number of boolean flags defined in base window. Subsequent IDXs of boolean flags in child class should start here
	 */
	public static final int numPrivFlags = 12;
	
	/**
	 * Holds currently specified uavTeamSize for this window
	 */
	protected int uavTeamSize = 4;
	
	/**
	 * list of values for dropdown list of team size
	 */
	protected final String[] uavTeamSizeList = new String[] {"2","3","4","5","6","7","8","9"};
	
	/**
	 * List of layout idxs available
	 */
	protected final String[] simLayoutToUseList = new String[] {"0","1","2","3","4"};
	
	public Base_DESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
	}//DancingBallWin
	

	@Override
	public final int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX
		tmpBtnNamesArray.add(new Object[] {"Visualization Debug", "Enable Debug", Base_BoolFlags.debugIDX});  
		tmpBtnNamesArray.add(new Object[] {"Resetting Simulation", "Reset Simulation",   resetSimIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Vis", "Render Visualization",  drawVisIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Teams", "Draw UAV Teams",  drawUAVTeamsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Task Locs", "Draw Task Locs",  drawTaskLocsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Lanes", "Draw Transit Lanes", drawTLanesIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Task Lbls", "Show Task Lbls",  dispTaskLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing TLane Lbls", "Show TLane Lbls", dispTLnsLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Team Lbls", "Show Team Lbls",  dispUAVLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Boats", "Drawing UAV Spheres",   drawBoatsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Experimenting", "Run Experiment", conductExpIDX});  
		tmpBtnNamesArray.add(new Object[] {"Team SweepSize Experiment", "Run Team Sweep Experiment", condUAVSweepExpIDX});  
		return initAllPrivBtns_Indiv(tmpBtnNamesArray);
	}//initAllPrivBtns	
	
	protected abstract int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	
	//set labels of boolean buttons 
//	private void setLabel(int idx, String tLbl, String fLbl) {truePrivFlagNames[idx] = tLbl;falsePrivFlagNames[idx] = fLbl;}//	
	
	/**
	 * reset initial flag states for each map type so that each sim world mirrors UI state
	 */
	private void resetDesFlags() {		
		simExec.setExecFlags(DES_SimExec.drawVisIDX, privFlags.getFlag(drawVisIDX));
		simExec.des.setSimFlags(DES_Simulator.drawBoatsIDX, privFlags.getFlag(drawBoatsIDX));				
		simExec.des.setSimFlags(DES_Simulator.drawUAVTeamsIDX, privFlags.getFlag(drawUAVTeamsIDX));				
		simExec.des.setSimFlags(DES_Simulator.drawTaskLocsIDX, privFlags.getFlag(drawTaskLocsIDX));				
		simExec.des.setSimFlags(DES_Simulator.drawTLanesIDX, privFlags.getFlag(drawTLanesIDX));				
		simExec.des.setSimFlags(DES_Simulator.dispTaskLblsIDX, privFlags.getFlag(dispTaskLblsIDX));				
		simExec.des.setSimFlags(DES_Simulator.dispTLnsLblsIDX, privFlags.getFlag(dispTLnsLblsIDX));				
		simExec.des.setSimFlags(DES_Simulator.dispUAVLblsIDX, privFlags.getFlag(dispUAVLblsIDX));		
		resetDesFlags_Indiv();
	}//setInitFlags	
	
	/**
	 * Instance specific reset of flag states
	 */
	protected abstract void resetDesFlags_Indiv();
		
	protected void createAndSetSimLayout(int _type) {
		if(sims[_type] == null) {sims[_type] = buildSimOfType(_type);}
		setSimToUse(_type);
	}//setSimToUse
	
	public final void setSimToUse(int _type) {
		currSim = sims[_type];
		boolean _isSimpleSim = isSimpleSim();
		currSim.setUavTeamSize(uavTeamSize);
		simExec.initSimWorld(currSim, true);
		boolean showVis = (ri != null);
		privFlags.setFlag(drawVisIDX, showVis);		
		privFlags.setFlag(drawUAVTeamsIDX, showVis);	
		privFlags.setFlag(drawBoatsIDX, showVis);	
		privFlags.setFlag(drawTaskLocsIDX, showVis);	
		privFlags.setFlag(drawTLanesIDX, showVis && _isSimpleSim);			
		privFlags.setFlag(dispTaskLblsIDX, showVis && _isSimpleSim);	
		privFlags.setFlag(dispTLnsLblsIDX, showVis && _isSimpleSim);	
		privFlags.setFlag(dispUAVLblsIDX, showVis && _isSimpleSim);	
		resetDesFlags();
		AppMgr.setSimIsRunning(false);
	}

	protected abstract DES_Simulator buildSimOfType(int _type);
	
	protected abstract boolean isSimpleSim();
	
	/**
	 * Initialize any UI control flags appropriate for all boids window application
	 */
	protected final void initDispFlags() {
		//this window uses a customizable camera
		dispFlags.setUseCustCam(true);
		// capable of using right side menu
		dispFlags.setHasRtSideMenu(true);		
	}
	
	@Override
	protected void initMe() {//all ui objects set by here
		//called once
		//initPrivFlags(numPrivFlags);
		//initialize sim exec to simple world sim
		simExec = new DES_SimExec(ri, msgObj);
		
		sims = new DES_Simulator[maxSimLayouts];
		for (int i=0; i<maxSimLayouts; ++i) {
			createAndSetSimLayout(i); 
		}
		setSimToUse(0);
		//Instance class specifics
		initMe_Indiv();
		
		custMenuOffset = uiClkCoords[3];	//495	
	}//initMe	
	
	protected abstract void initMe_Indiv();
	
	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new DES_UIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	protected final void updateCalcObjUIVals() {}
	
	@Override
	protected int[] getFlagIDXsToInitToTrue() {return null;}
	/**
	 * UI code-level Debug mode functionality. Called only from flags structure
	 * @param val
	 */
	@Override
	protected final void handleDispFlagsDebugMode_Indiv(boolean val) {}
	
	/**
	 * Application-specific Debug mode functionality (application-specific). Called only from privflags structure
	 * @param val
	 */
	@Override
	protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {	
		simExec.setExecFlags(DES_SimExec.debugExecIDX,val);		
	}
	
	/**
	 * Handle application-specific flag setting  TODO use sim uiDataUpdater
	 */
	@Override
	public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
		switch(idx){
			case resetSimIDX			: {
				if(val) {simExec.initSimExec(true); addPrivBtnToClear(resetSimIDX);}break;}
			case drawVisIDX				:{
				simExec.setExecFlags(DES_SimExec.drawVisIDX, val);break;}
			case drawBoatsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(DES_Simulator.drawBoatsIDX, val);			break;}
			case drawUAVTeamsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(DES_Simulator.drawUAVTeamsIDX, val);		break;}
			case drawTaskLocsIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(DES_Simulator.drawTaskLocsIDX, val);		break;}
			case drawTLanesIDX			:{//set value directly in DES (bypass exec)
				simExec.des.setSimFlags(DES_Simulator.drawTLanesIDX, val);		break;}
			case dispTaskLblsIDX		: {				
				simExec.des.setSimFlags(DES_Simulator.dispTaskLblsIDX, val);		break;}
			case dispTLnsLblsIDX		: {				
				simExec.des.setSimFlags(DES_Simulator.dispTLnsLblsIDX, val);		break;}
			case dispUAVLblsIDX			: {				
				simExec.des.setSimFlags(DES_Simulator.dispUAVLblsIDX, val);		break;}				
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials(uiUpdateData.getIntValue(gIDX_ExpLength), uiUpdateData.getIntValue(gIDX_NumExpTrials), true);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(conductExpIDX);
				} 
				break;}
			case condUAVSweepExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials(uiUpdateData.getIntValue(gIDX_ExpLength), uiUpdateData.getIntValue(gIDX_NumExpTrials), false);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(condUAVSweepExpIDX);
				} 
				break;}			
			default: {
				if (!handleDesPrivFlags_Indiv(idx, val, oldVal)){
					msgObj.dispErrorMessage(className, "handlePrivFlags_Indiv", "Unknown/unhandled flag idx :"+idx+" attempting to be set to "+val+" from "+oldVal+". Aborting.");
				}				
			}
		}		
	}//handlePrivFlags_Indiv
	
	/**
	 * Instance-specific boolean flags to handle
	 * @param idx
	 * @param val
	 * @param oldVal
	 * @return
	 */
	protected abstract boolean handleDesPrivFlags_Indiv(int idx, boolean val, boolean oldVal);
	
	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	{value is sent to owning window, 
	 *           	value is sent on any modifications (while being modified, not just on release), 
	 *           	changes to value must be explicitly sent to consumer (are not automatically sent)}    
	 * @param tmpListObjVals : map of list object possible selection values
	 */
	@Override
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {
		tmpListObjVals.put(gIDX_LayoutToUse, simLayoutToUseList);	
		tmpListObjVals.put(gIDX_UAVTeamSize, uavTeamSizeList);	
		
		double initTeamSizeIDX = 1.0*uavTeamSize - Integer.parseInt(uavTeamSizeList[0]);
		
		tmpUIObjArray.put(gIDX_LayoutToUse, new Object[] { new double[]{0,simLayoutToUseList.length-1, 1.0f}, 0.0, "Sim Layout To Use", GUIObj_Type.ListVal, new boolean[]{true}});          				
		tmpUIObjArray.put(gIDX_FrameTimeScale, new Object[] { new double[]{1.0f,10000.0f,1.0f}, 1.0*DES_SimExec.frameTimeScale, "Sim Speed Multiplier", GUIObj_Type.FloatVal, new boolean[]{true}});  
		tmpUIObjArray.put(gIDX_UAVTeamSize, new Object[] { new double[]{0,uavTeamSizeList.length-1, 1.0f}, initTeamSizeIDX, "UAV Team Size", GUIObj_Type.ListVal, new boolean[]{true}});          
		tmpUIObjArray.put(gIDX_ExpLength, new Object[] { new double[]{1.0f, 1440, 1.0f}, 720.0, "Experiment Duration", GUIObj_Type.IntVal, new boolean[]{true}});    
		tmpUIObjArray.put(gIDX_NumExpTrials, new Object[] { new double[]{1.0f, 100, 1.0f}, 1.0, "# Experimental Trials", GUIObj_Type.IntVal, new boolean[]{true}});  
		
		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals);
	}//setupGUIObjsAras
	
	protected abstract void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals);
	
	/**
	 * Called if int-handling guiObjs_Numeric[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */	
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx){		
			case gIDX_LayoutToUse : {
				setSimToUse(ival);
				break;}
			case gIDX_UAVTeamSize : {
				uavTeamSize = ival + Integer.parseInt(uavTeamSizeList[0]);//add idx 0 as min size
				msgObj.dispDebugMessage("DESSimWindow", "setUIWinVals", "UAV team size desired is : " + uavTeamSize);
				currSim.setUavTeamSize(uavTeamSize);
				//rebuild sim exec and sim environment whenever team size changes
				simExec.initSimExec(true);				
				break;}
			case gIDX_ExpLength 		: {break;}//determines experiment length				
			case gIDX_NumExpTrials 		: {break;}//# of trials for experiments
			default : {
				if (!setUI_IntDESValsCustom(UIidx, ival, oldVal)) {
					msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				}
			}
		}		
	}
	
	/**
	 * Handle instance-specific integer ui value setting
	 * @param UIidx
	 * @param ival
	 * @param oldVal
	 * @return
	 */
	protected abstract boolean setUI_IntDESValsCustom(int UIidx, int ival, int oldVal);
	 
	/**
	 * Called if float-handling guiObjs_Numeric[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){		
			case gIDX_FrameTimeScale 			:{
				simExec.setTimeScale(val);
				break;}

			default : {
				if (!setUI_FloatDESValsCustom(UIidx, val, oldVal)) {
					msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				}
			}
		}		
	}
	/**
	 * Handle instance-specific float ui value setting
	 * @param UIidx
	 * @param ival
	 * @param oldVal
	 * @return
	 */
	protected abstract boolean setUI_FloatDESValsCustom(int UIidx, float val, float oldVal);
	
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCamera_Indiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		ri.setCameraWinVals(camVals);//camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		ri.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv
	
	@Override
	//modAmtMillis is time passed per frame in milliseconds
	protected boolean simMe(float modAmtMillis) {//run simulation
		boolean done = simExec.simMe(modAmtMillis);
		if(done) {privFlags.setFlag(conductExpIDX, false);}
		return done;	
	}//simMe
	
	@Override
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		ri.pushMatState();
		//display current simulation variables
		simExec.des.drawResultBar(ri, txtHeightOff);
		ri.popMatState();		
	}

	@Override
	protected final void drawOnScreenStuffPriv(float modAmtMillis) {}

	@Override
	//animTimeMod is in seconds.
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = ri.c.getMse2DtoMse3DinWorld(ri.sceneCtrVals[ri.sceneIDX]);			//need to be here
//		curMseLoc3D = ri.c.getMseLoc(ri.sceneCtrVals[ri.sceneIDX]);
		simExec.drawMe(animTimeMod, this);
	}//drawMe	
		
	//draw custom 2d constructs below interactive component of menu
	@Override
	public void drawCustMenuObjs(float animTimeMod){
		ri.pushMatState();
		//all sub menu drawing within push mat call
		ri.translate(0,custMenuOffset+txtHeightOff);		
		//draw any custom menu stuff here
		ri.popMatState();	
	}//drawCustMenuObjs

	/////////////////////////////
	// window control
	@Override
	protected final void resizeMe(float scale) {}
	@Override
	protected final void showMe() {}
	@Override
	protected final void closeMe() {}
	@Override
	protected final void stopMe() {}

	@Override
	public void handleSideMenuMseOvrDispSel(int btn, boolean val) {}
	@Override
	protected final myPoint getMsePtAs3DPt(myPoint mseLoc) {		return new myPoint(mseLoc);	}
	@Override
	protected void setVisScreenDimsPriv() {}
	@Override
	protected final void setCustMenuBtnLabels() {	}
	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
		switch (funcRow) {
			case 0: {// row 1 of menu side bar buttons
				// {"Gen Training Data", "Save Training data","Load Training Data"}, //row 1
				resetButtonState();
				switch (btn) {
					case 0: {	
						break;
					}
					case 1: {
						break;
					}
					case 2: {
						break;
					}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 1 btn : " + btn, MsgCodes.warning2);
						break;
					}
				}
				break;
			} // row 1 of menu side bar buttons
	
			case 1: {// row 2 of menu side bar buttons
				switch (btn) {
					case 0: {
						resetButtonState();
						break;
					}
					case 1: {
						resetButtonState();
						break;
					}
					case 2: {
						resetButtonState();
						break;
					}
					case 3: {// show/hide som Map UI
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 2 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 2 of menu side bar buttons
			case 2: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0: {	
						resetButtonState();
						break;
					}
					case 1: {			
						resetButtonState();
						break;
					}
					case 2: {	
						resetButtonState();
						break;
					}
					case 3: {
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 3 btn : " + btn,
								MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 3 of menu side bar buttons
			case 3: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0: {			
						simExec.TEST_verifyPriorityQueueFunctionality();	
						resetButtonState();
						break;
					}
					case 1:{//FEL test 
						simExec.TEST_verifyFEL();		
						resetButtonState();
						break;
					}
					case 2:{//sim environment tester				
						simExec.TEST_simulator();	
						resetButtonState();
						break;
					}
					case 3: {//test tasks
						simExec.TEST_taskDists();
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 4 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 3 of menu side bar buttons
			default : {
				msgObj.dispWarningMessage(className,"launchMenuBtnHndlr","Clicked Unknown Btn row : " + funcRow +" | Btn : " + btn);
				break;
			}
		}
	}
	@Override
	protected final void handleSideMenuDebugSelEnable(int btn) {
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
			default: {
				msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				break;
			}
		}
	}
	
	@Override
	protected final void handleSideMenuDebugSelDisable(int btn) {
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
		default: {
			msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			break;
			}
		}
	}

	
	@Override
	protected boolean hndlMouseMove_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	//alt key pressed handles trajectory
	
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClick_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
			
		return false;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDrag_Indiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		return res;}	
	@Override
	protected void hndlMouseRel_Indiv() {	}
	@Override
	protected final void endShiftKeyI() {}
	@Override
	protected final void endAltKeyI() {}
	@Override
	protected final void endCntlKeyI() {}
	
	///////////////////////
	// deprecated file io stuff
	@Override
	public final void hndlFileLoad(File file, String[] vals, int[] stIdx) {}
	@Override
	public final ArrayList<String> hndlFileSave(File file) {		return null;}
	@Override
	protected final String[] getSaveFileDirNamesPriv() {return null;	}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	
	
	
	////////////////////
	// drawn trajectory stuff
	@Override
	protected final void initDrwnTraj_Indiv() {}
	@Override
	protected final void addSScrToWin_Indiv(int newWinKey) {}
	@Override
	protected final void addTrajToScr_Indiv(int subScrKey, String newTrajKey) {}
	@Override
	protected final void delSScrToWin_Indiv(int idx) {}
	@Override
	protected final void delTrajToScr_Indiv(int subScrKey, String newTrajKey) {}
	@Override
	public void processTraj_Indiv(DrawnSimpleTraj drawnTraj) {}
}//DESSimWindow

