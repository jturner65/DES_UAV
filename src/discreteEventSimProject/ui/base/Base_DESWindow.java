package discreteEventSimProject.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.simulation.simExec.Base_UISimExec;
import base_UI_Objects.windowUI.simulation.ui.Base_UISimWindow;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_Utils_Objects.io.messaging.MsgCodes;
import discreteEventSimProject.simExec.base.Base_DESSimExec;
import discreteEventSimProject.ui.DES_UIDataUpdater;

public abstract class Base_DESWindow extends Base_UISimWindow {
	///////////
	//ui vals

	public final static int
		gIDX_UAVTeamSize			= numBaseSimGUIObjs, 
		gIDX_ExpLength				= numBaseSimGUIObjs+1,			//length of time for experiment, in minutes
		gIDX_NumExpTrials			= numBaseSimGUIObjs+2;
		
	/**
	 * Number of gui objects defined in base window. Subsequent IDXs in child class should start here
	 */
	protected static final int numBaseDESGUIObjs = numBaseSimGUIObjs+3;		

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
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	
	/**
	 * private child-class flags - window specific
	 */
	public static final int 
			drawBoatsIDX		= numBaseSimPrivFlags,						//whether to draw animated boats or simple spheres for consumer UAVs
			drawUAVTeamsIDX		= numBaseSimPrivFlags +2,						//yes/no draw UAV teams
			drawTaskLocsIDX		= numBaseSimPrivFlags +3,						//yes/no draw task spheres
			drawTLanesIDX		= numBaseSimPrivFlags +4,						//yes/no draw transit lanes and queues
			dispTaskLblsIDX		= numBaseSimPrivFlags +5,						//show labels over tasks...
			dispTLnsLblsIDX		= numBaseSimPrivFlags +6,						//over transit lanes...
			dispUAVLblsIDX		= numBaseSimPrivFlags +7,						//and/or over teams			
			conductExpIDX		= numBaseSimPrivFlags +8,						//conduct experiment with current settings
			condUAVSweepExpIDX  = numBaseSimPrivFlags +9;						//sweep through UAV Team Sizes

	/**
	 * Number of boolean flags defined in base window. Subsequent IDXs of boolean flags in child class should start here
	 */
	public static final int numBaseDesPrivFlags = numBaseSimPrivFlags +10;
	
	public Base_DESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
	}//DancingBallWin
	

	@Override
	public final int initSimPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Teams", "Draw UAV Teams",  drawUAVTeamsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Task Locs", "Draw Task Locs",  drawTaskLocsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Lanes", "Draw Transit Lanes", drawTLanesIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Task Lbls", "Show Task Lbls",  dispTaskLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing TLane Lbls", "Show TLane Lbls", dispTLnsLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Team Lbls", "Show Team Lbls",  dispUAVLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Boats", "Drawing UAV Spheres",   drawBoatsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Experimenting", "Run Experiment", conductExpIDX});  
		tmpBtnNamesArray.add(new Object[] {"Team SweepSize Experiment", "Run Team Sweep Experiment", condUAVSweepExpIDX});  
		return initSimPrivBtns_Indiv(tmpBtnNamesArray);
	}//initAllPrivBtns	
	
	protected abstract int initSimPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	
	/**
	 * Instance specific reset of flag states
	 */
	protected abstract void resetDesFlags_Indiv();
	@Override
	public final void setSimToUse(int _type) {
		boolean _isSimpleSim = isSimpleSim();
		((Base_DESSimExec) simExec).setSimUAVTeamSize(uavTeamSize);
		simExec.setSimAndInit(_type, true);
		boolean showVis = (ri != null);
		privFlags.setFlag(drawVisIDX, showVis);		
		privFlags.setFlag(drawUAVTeamsIDX, showVis);	
		privFlags.setFlag(drawBoatsIDX, showVis);	
		privFlags.setFlag(drawTaskLocsIDX, showVis);		
		privFlags.setFlag(drawTLanesIDX, showVis && _isSimpleSim);			
		privFlags.setFlag(dispTaskLblsIDX, showVis && _isSimpleSim);	
		privFlags.setFlag(dispTLnsLblsIDX, showVis && _isSimpleSim);	
		privFlags.setFlag(dispUAVLblsIDX, showVis && _isSimpleSim);	
		resetDesFlags_Indiv();
		AppMgr.setSimIsRunning(false);
	}
	
	protected abstract boolean isSimpleSim();
	
	/**
	 * Initialize any UI control flags appropriate for all boids window application
	 */
	@Override
	protected final void initDispFlags() {
		//this window uses a customizable camera
		dispFlags.setUseCustCam(true);
		// capable of using right side menu
		dispFlags.setHasRtSideMenu(true);		
	}
	
	/**
	 * Initialize the simulation executive during initMe() after simExec was created
	 * @param showVis whether or not we should render the visualizations for this simulation
	 */
	@Override
	protected final void initSimExec(boolean showVis) {
		boolean _isSimpleSim = isSimpleSim();
		simExec.initMasterDataAdapter(Base_DESSimExec.drawUAVTeamsIDX, showVis);	
		simExec.initMasterDataAdapter(Base_DESSimExec.drawBoatsIDX, showVis);	
		simExec.initMasterDataAdapter(Base_DESSimExec.drawTaskLocsIDX, showVis);		
		simExec.initMasterDataAdapter(Base_DESSimExec.drawTLanesIDX, showVis && _isSimpleSim);			
		simExec.initMasterDataAdapter(Base_DESSimExec.dispTaskLblsIDX, showVis && _isSimpleSim);	
		simExec.initMasterDataAdapter(Base_DESSimExec.dispTLnsLblsIDX, showVis && _isSimpleSim);	
		simExec.initMasterDataAdapter(Base_DESSimExec.dispUAVLblsIDX, showVis && _isSimpleSim);					
	}//initSimExec
	
	@Override
	protected final void initMeSim() {//all ui objects set by here
		//Instance class specifics
		initMeSim_Indiv();
	}//initMe	
	
	protected abstract Base_UISimExec buildSimulationExecutive(String _name, int _numSimulations);
	
	protected abstract void initMeSim_Indiv();
	
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
	protected final void updateCalcObjUIVals() {
		//pass updates to simulator executive
		
		
	}
	
	@Override
	protected int[] getFlagIDXsToInitToTrue() {return null;}
	
	/**
	 * Handle application-specific flag setting  TODO use sim uiDataUpdater
	 */
	@Override
	protected final boolean handleSimPrivFlags_Indiv(int idx, boolean val, boolean oldVal){
		switch(idx){
			case drawBoatsIDX			:{//set value directly in DES (bypass exec)
				simExec.setSimFlag(Base_DESSimExec.drawBoatsIDX, val);			return true;}
			case drawUAVTeamsIDX			:{//set value directly in DES (bypass exec)
				simExec.setSimFlag(Base_DESSimExec.drawUAVTeamsIDX, val);		return true;}
			case drawTaskLocsIDX			:{//set value directly in DES (bypass exec)
				simExec.setSimFlag(Base_DESSimExec.drawTaskLocsIDX, val);		return true;}
			case drawTLanesIDX			:{//set value directly in DES (bypass exec)
				simExec.setSimFlag(Base_DESSimExec.drawTLanesIDX, val);		return true;}
			case dispTaskLblsIDX		: {				
				simExec.setSimFlag(Base_DESSimExec.dispTaskLblsIDX, val);		return true;}
			case dispTLnsLblsIDX		: {				
				simExec.setSimFlag(Base_DESSimExec.dispTLnsLblsIDX, val);		return true;}
			case dispUAVLblsIDX			: {				
				simExec.setSimFlag(Base_DESSimExec.dispUAVLblsIDX, val);		return true;}				
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					((Base_DESSimExec) simExec).initializeTrials(uiUpdateData.getIntValue(gIDX_ExpLength), uiUpdateData.getIntValue(gIDX_NumExpTrials), true);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(conductExpIDX);
				} 
				return true;}
			case condUAVSweepExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					((Base_DESSimExec) simExec).initializeTrials(uiUpdateData.getIntValue(gIDX_ExpLength), uiUpdateData.getIntValue(gIDX_NumExpTrials), false);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(condUAVSweepExpIDX);
				} 
				return true;}			
			default: {return handleDesPrivFlags_Indiv(idx, val, oldVal);}
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
	protected final void setupGUIObjsAras_Sim(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {
		tmpListObjVals.put(gIDX_UAVTeamSize, uavTeamSizeList);	
		
		double initTeamSizeIDX = 1.0*uavTeamSize - Integer.parseInt(uavTeamSizeList[0]);
		
		tmpUIObjArray.put(gIDX_UAVTeamSize, uiObjInitAra_List(new double[]{0,uavTeamSizeList.length-1, 1.0f}, initTeamSizeIDX, "UAV Team Size", new boolean[]{true}));          
		tmpUIObjArray.put(gIDX_ExpLength, uiObjInitAra_Int(new double[]{1.0f, 1440, 1.0f}, 720.0, "Experiment Duration", new boolean[]{true}));    
		tmpUIObjArray.put(gIDX_NumExpTrials, uiObjInitAra_Int(new double[]{1.0f, 100, 1.0f}, 1.0, "# Experimental Trials", new boolean[]{true}));  
		
		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals);
	}//setupGUIObjsAras
	/**
	 * Return the list to use for sim layout
	 * @return
	 */
	protected final String[] getSimLayoutToUseList() {return simLayoutToUseList;}
	
	
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
	protected final boolean setUI_SimIntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx){		
			case gIDX_UAVTeamSize : {
				uavTeamSize = ival + Integer.parseInt(uavTeamSizeList[0]);//add idx 0 as min size
				msgObj.dispDebugMessage("DESSimWindow", "setUIWinVals", "UAV team size desired is : " + uavTeamSize);
				((Base_DESSimExec) simExec).setSimUAVTeamSize(uavTeamSize);
				//rebuild sim exec and sim environment whenever team size changes
				simExec.resetSimExec(true);				
				return true;}
			case gIDX_ExpLength 		: {return true;}//determines experiment length				
			case gIDX_NumExpTrials 		: {return true;}//# of trials for experiments
			default : {	return setUI_IntDESValsCustom(UIidx, ival, oldVal);	}
		}		
	}//setUI_SimIntValsCustom
	
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
	protected final boolean setUI_SimUIFloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){		
			default : {	return setUI_FloatDESValsCustom(UIidx, val, oldVal);
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
	
	/**
	 * modAmtMillis is time passed per frame in milliseconds
	 */
	@Override
	protected boolean simMePostExec_Indiv(float modAmtMillis, boolean done) {//run simulation
		if(done) {privFlags.setFlag(conductExpIDX, false);}
		return done;	
	}//simMe	

	@Override
	protected final void drawOnScreenStuffPriv(float modAmtMillis) {}
		
	//draw custom 2d constructs below interactive component of menu
	@Override
	public void drawSimCustMenuObjs(float animTimeMod){
		ri.pushMatState();	
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
			case 0: {// row 0 of menu side bar buttons
				// {"Gen Training Data", "Save Training data","Load Training Data"}, //row 1				
				switch (btn) {
					case 0: {resetButtonState();break;}
					case 1: {resetButtonState();break;}
					case 2: {resetButtonState();break;}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 1 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // end row 0 of menu side bar buttons	
			case 1: {// row 1 of menu side bar buttons
				switch (btn) {
					case 0: {resetButtonState();break;}
					case 1: {resetButtonState();break;}
					case 2: {resetButtonState();break;}
					case 3: {resetButtonState();break;}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 2 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // end row 1 of menu side bar buttons
			case 2: {// row 2 of menu side bar buttons
				switch (btn) {
					case 0: {resetButtonState();break;}
					case 1: {resetButtonState();break;}
					case 2: {resetButtonState();break;}
					case 3: {resetButtonState();break;}
					default: {
						msgObj.dispMessage(className, "launchMenuBtnHndlr", "Unknown Functions 3 btn : " + btn,	MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // end row 2 of menu side bar buttons
			case 3: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0: {			
						((Base_DESSimExec) simExec).TEST_verifyPriorityQueueFunctionality();	
						resetButtonState();
						break;
					}
					case 1:{//FEL test 
						((Base_DESSimExec) simExec).TEST_verifyFEL();		
						resetButtonState();
						break;
					}
					case 2:{//sim environment tester				
						((Base_DESSimExec) simExec).TEST_simulator();	
						resetButtonState();
						break;
					}
					case 3: {//test tasks
						((Base_DESSimExec) simExec).TEST_taskDists();
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
			} // end row 3 of menu side bar buttons
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

