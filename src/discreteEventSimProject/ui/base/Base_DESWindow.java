package discreteEventSimProject.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Render_Interface.IRenderInterface;
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
		gIDX_UAVTeamSize			= numBaseSimGUIObjs; 
		
	/**
	 * Number of gui objects defined in base window. Subsequent IDXs in child class should start here
	 */
	protected static final int numBaseDESGUIObjs = numBaseSimGUIObjs+1;		

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
			drawUAVTeamsIDX		= numBaseSimPrivFlags +1,						//yes/no draw UAV teams
			drawTaskLocsIDX		= numBaseSimPrivFlags +2,						//yes/no draw task spheres
			drawTLanesIDX		= numBaseSimPrivFlags +3,						//yes/no draw transit lanes and queues
			dispTaskLblsIDX		= numBaseSimPrivFlags +4,						//show labels over tasks...
			dispTLnsLblsIDX		= numBaseSimPrivFlags +5,						//over transit lanes...
			dispUAVLblsIDX		= numBaseSimPrivFlags +6;						//and/or over teams			

	/**
	 * Number of boolean flags defined in base window. Subsequent IDXs of boolean flags in child class should start here
	 */
	public static final int numBaseDesPrivFlags = numBaseSimPrivFlags +7;
	
	public Base_DESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
	}//Base_DESWindow
	
	
	@Override 
	protected final String getSweepFieldName() {return "Team Size";}
	
	
	/**
	 * Instance specific reset of flag states
	 */
	protected abstract void resetDesFlags_Indiv();
	@Override
	public final void setSimToUse(int _type) {
		((Base_DESSimExec) simExec).setSimUAVTeamSize(uavTeamSize);
		simExec.setSimAndInit(_type, true);

		boolean _isSimpleSim = isSimpleSim();
		boolean showVis = (ri != null);
		uiMgr.setPrivFlag(drawVisIDX, showVis);		
		uiMgr.setPrivFlag(drawUAVTeamsIDX, showVis);	
		uiMgr.setPrivFlag(drawBoatsIDX, showVis);	
		uiMgr.setPrivFlag(drawTaskLocsIDX, showVis);		
		uiMgr.setPrivFlag(drawTLanesIDX, showVis && _isSimpleSim);			
		uiMgr.setPrivFlag(dispTaskLblsIDX, showVis && _isSimpleSim);	
		uiMgr.setPrivFlag(dispTLnsLblsIDX, showVis && _isSimpleSim);	
		uiMgr.setPrivFlag(dispUAVLblsIDX, showVis && _isSimpleSim);	
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
			default: {return handleDesPrivFlags_Indiv(idx, val, oldVal);}
		}		
	}//handleSimPrivFlags_Indiv
	
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
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *           the 6th element is a boolean array of format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 * @param tmpListObjVals : map of string arrays, keyed by UI object idx, with array values being each element in the list
	 * @param tmpBtnNamesArray : map of Object arrays to be built containing all button definitions, keyed by sequential value == objId
	 * 				the first element is true label
	 * 				the second element is false label
	 * 				the third element is integer flag idx 
	 */
	@Override
	protected final void setupGUIObjsAras_Sim(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals, TreeMap<Integer,Object[]> tmpBtnNamesArray) {
		tmpListObjVals.put(gIDX_UAVTeamSize, uavTeamSizeList);	
		
		double initTeamSizeIDX = 1.0*uavTeamSize - Integer.parseInt(uavTeamSizeList[0]);
		
		tmpUIObjArray.put(gIDX_UAVTeamSize, uiMgr.uiObjInitAra_List(new double[]{0,uavTeamSizeList.length-1, 1.0f}, initTeamSizeIDX, "UAV Team Size"));          
		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX  
		int idx=tmpBtnNamesArray.size();
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Drawing UAV Teams", "Draw UAV Teams"},  drawUAVTeamsIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Drawing Task Locs", "Draw Task Locs"},  drawTaskLocsIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Drawing Lanes", "Draw Transit Lanes"}, drawTLanesIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Showing Task Lbls", "Show Task Lbls"},  dispTaskLblsIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Showing TLane Lbls", "Show TLane Lbls"}, dispTLnsLblsIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Showing Team Lbls", "Show Team Lbls"},  dispUAVLblsIDX));  
		tmpBtnNamesArray.put(idx++, uiMgr.uiObjInitAra_Btn(new String[] {"Drawing UAV Boats", "Drawing UAV Spheres"},   drawBoatsIDX));  
			
		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals, tmpBtnNamesArray.size(), tmpBtnNamesArray);
	}//setupGUIObjsAras
	/**
	 * Return the list to use for sim layout
	 * @return
	 */
	protected final String[] getSimLayoutToUseList() {return simLayoutToUseList;}

	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *           the 6th element is a boolean array of format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 * @param tmpListObjVals : map of string arrays, keyed by UI object idx, with array values being each element in the list
	 * @param firstBtnIDX : first index to place button objects in @tmpBtnNamesArray 
	 * @param tmpBtnNamesArray : map of Object arrays to be built containing all button definitions, keyed by sequential value == objId
	 * 				the first element is true label
	 * 				the second element is false label
	 * 				the third element is integer flag idx 
	 */
	protected abstract void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals, int firstBtnIDX, TreeMap<Integer, Object[]> tmpBtnNamesArray);

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
				_dispInfoMsg("setUIWinVals", "UAV team size desired is : " + uavTeamSize);
				((Base_DESSimExec) simExec).setSimUAVTeamSize(uavTeamSize);
				//rebuild sim exec and sim environment whenever team size changes
				simExec.resetSimExec(true);				
				return true;}
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
	protected void setCamera_Indiv(float[] camVals) {
		// No custom camera handling
		setCameraBase(camVals);
	}//setCameraIndiv
	
	/**
	 * modAmtMillis is time passed per frame in milliseconds
	 */
	@Override
	protected boolean simMePostExec_Indiv(float modAmtMillis, boolean done) {//run simulation
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
	protected final void endShiftKey_Indiv() {}
	@Override
	protected final void endAltKey_Indiv() {}
	@Override
	protected final void endCntlKey_Indiv() {}
	
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

