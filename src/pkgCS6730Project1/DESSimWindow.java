package pkgCS6730Project1;

import java.io.File;
import java.util.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.base_UpdateFromUIData;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.uiObjs.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class DESSimWindow extends myDispWindow {
	//simulation executive
	private mySimExecutive simExec;
	private mySimulator smplSim, cmplxSim;
	//motion
	///////////
	//ui vals
	//list of values for dropdown list of team size
	//private String[] uavTeamSizeVals = new String[] {"2","3","4","5","6","7","8","9"};
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

	public final int numGUIObjs = 4;											//# of gui objects for ui	
	
	//display variables
	//private float[] UIrectBox;	//box holding x,y,w,h values of black rectangle to hold UI sim display values
	
	/////////
	//custom debug/function ui button names -empty will do nothing
//	public String[] menuDbgBtnNames = new String[] {"Verify PQ", "Verify FEL", "Show Sim", "Test Tasks"};//must have literals for every button or this is ignored
//	public String[] menuFuncBtnNames = new String[] {"Use Main Sim", "Use Huge Sim"};//must have literals for every button or ignored
	
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
			conductExpIDX		= 10,						//conduct experiment with current settings
			condUAVSweepExpIDX  = 11;						//sweep through UAV Team Sizes

	public static final int numPrivFlags = 12;
	
//	public String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar
//		// menu, or ignored
//		{ "Simple SIM", "Complex SIM", "---"}, // row 1
//		{ "---", "---", "---", "---" }, // row 3
//		{ "---", "---", "---", "---" }, // row 2
//		{ "---", "---", "---", "---" }, 
//		{ "---", "---", "---", "---", "---" } 
//	};
		
	public DESSimWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, int _flagIdx) {
		super(_p, _AppMgr, _winIdx, _flagIdx);
		super.initThisWin(false);
	}//DancingBallWin
	

	@Override
	public final int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {

		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX
		tmpBtnNamesArray.add(new Object[] {"Visualization Debug", "Enable Debug", debugAnimIDX});  
		tmpBtnNamesArray.add(new Object[] {"Resetting Simulation", "Reset Simulation",   resetSimIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Vis", "Render Visualization",  drawVisIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Teams", "Draw UAV Teams",  drawUAVTeamsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Task Locs", "Draw Task Locs",  drawTaskLocsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing Lanes", "Draw Transit Lanes", drawTLanesIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Task Lbls", "Show Task Lbls",  dispTaskLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing TLane Lbls", "Show TLane Lbls", dispTLnsLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Showing Team Lbls", "Show Team Lbls",  dispUAVLblsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Drawing UAV Boats", "Drawing UAV Spheres",   drawBoatsIDX});  
		tmpBtnNamesArray.add(new Object[] {"Experimenting", "Conduct Experiment", conductExpIDX});  
		tmpBtnNamesArray.add(new Object[] {"Team SweepSize Experiment", "Conduct Team Sweep Experiment", condUAVSweepExpIDX});  
		return numPrivFlags;
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
		setPrivFlags(dispTaskLblsIDX, false);	
		setPrivFlags(dispTLnsLblsIDX, false);	
		setPrivFlags(dispUAVLblsIDX, false);	
		resetDesFlags();
		//pa.setSimIsRunning(false);
		
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
		setPrivFlags(dispTaskLblsIDX, showVis);	
		setPrivFlags(dispTLnsLblsIDX, showVis);	
		setPrivFlags(dispUAVLblsIDX, showVis);	
		resetDesFlags();
		//turn off simulation if running
		//pa.setSimIsRunning(false);	
		
	}//initSimpleSim	
	
	@Override
	protected void initMe() {//all ui objects set by here
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);
		//called once
		//initPrivFlags(numPrivFlags);
		//initialize sim exec to simple world sim
		simExec = new mySimExecutive(pa, msgObj);
		
		setSimpleSim();
		
		custMenuOffset = uiClkCoords[3];	//495	
	}//initMe	
		
	@Override
	protected base_UpdateFromUIData buildUIDataUpdateObject() {return null;	}

	@Override
	protected void buildUIUpdateStruct_Indiv(TreeMap<Integer, Integer> intValues, TreeMap<Integer, Float> floatValues,TreeMap<Integer, Boolean> boolValues) {	}

	@Override
	protected int[] getFlagIDXsToInitToTrue() {return null;}
	
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
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], true);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(conductExpIDX);
				} 
				break;}
			case condUAVSweepExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
				if(val) {
					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], false);
					AppMgr.setSimIsRunning(true);
					addPrivBtnToClear(condUAVSweepExpIDX);
				} 
				break;}
			
			default:					{}
		}		
	}//setPrivFlags	
		

	@Override
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {
		tmpListObjVals.put(gIDX_UAVTeamSize, new String[] {"2","3","4","5","6","7","8","9"});				                                                                                                                              			
		tmpUIObjArray.put(gIDX_FrameTimeScale, new Object[] { new double[]{1.0f,10000.0f,1.0f}, 1.0*mySimExecutive.frameTimeScale, "Sim Speed Multiplier", GUIObj_Type.FloatVal, new boolean[]{true}});  
		tmpUIObjArray.put(gIDX_UAVTeamSize, new Object[] { new double[]{0,tmpListObjVals.get(gIDX_UAVTeamSize).length-1, 1.0f}, 1.0*mySimulator.uavTeamSize - Integer.parseInt(tmpListObjVals.get(gIDX_UAVTeamSize)[0]), "UAV Team Size", GUIObj_Type.ListVal, new boolean[]{true}});          
		tmpUIObjArray.put(gIDX_ExpLength, new Object[] { new double[]{1.0f, 1440, 1.0f}, 720.0, "Experiment Duration", GUIObj_Type.IntVal, new boolean[]{true}});    
		tmpUIObjArray.put(gIDX_NumExpTrials, new Object[] { new double[]{1.0f, 100, 1.0f}, 1.0, "# Experimental Trials", GUIObj_Type.IntVal, new boolean[]{true}});  
					
		//tmpUIObjArray.put(gIDX_MapType,new Object[] { new double[]{0.0, tmpListObjVals.get(gIDX_MapType).length-1, 1},0.0, "Map Type to Show", GUIObj_Type.ListVal, new boolean[]{true}}); 
	
	}//setupGUIObjsAras
	
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
					mySimulator.uavTeamSize = (int)val + 2;//add idx 0 as min size
					msgObj.dispInfoMessage("DESSimWindow", "setUIWinVals", "UAV team size desired is : " + mySimulator.uavTeamSize);
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

	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.setCameraWinVals(camVals);//camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv
	
	@Override
	//modAmtMillis is time passed per frame in milliseconds
	protected boolean simMe(float modAmtMillis) {//run simulation
		boolean done = simExec.simMe(modAmtMillis);
		if(done) {setPrivFlags(conductExpIDX, false);}
		return done;	
	}//simMe
	
	@Override
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatState();
		//display current simulation variables
		simExec.des.drawResultBar(pa,  yOff);
		pa.popMatState();		
	}

	@Override
	protected final void drawOnScreenStuffPriv(float modAmtMillis) {}

	@Override
	//animTimeMod is in seconds.
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//need to be here
//		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
		simExec.drawMe(animTimeMod, this);
	}//drawMe	
		
	//draw custom 2d constructs below interactive component of menu
	@Override
	public void drawCustMenuObjs(){
		pa.pushMatState();
		//all sub menu drawing within push mat call
		pa.translate(0,custMenuOffset+yOff);		
		//draw any custom menu stuff here
		pa.popMatState();	
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
	protected final void setCustMenuBtnNames() {	}
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn) {
		msgObj.dispMessage(className, "launchMenuBtnHndlr", "Begin requested action : Click Functions "+(funcRow+1)+" in " + name + " : btn : " + btn, MsgCodes.info4);
		switch (funcRow) {
			case 0: {// row 1 of menu side bar buttons
				// {"Gen Training Data", "Save Training data","Load Training Data"}, //row 1
				resetButtonState();
				switch (btn) {
					case 0: {						
						setSimpleSim();
						AppMgr.setSimIsRunning(false);
						break;
					}
					case 1: {
						setComplexSim();
						AppMgr.setSimIsRunning(false);
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
		}
		msgObj.dispMessage(className, "launchMenuBtnHndlr", "End requested action (multithreaded actions may still be working) : Click Functions "+(funcRow+1)+" in " + name + " : btn : " + btn, MsgCodes.info4);
	}
	@Override
	public final void handleSideMenuDebugSelEnable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable","Click Debug functionality on in " + name + " : btn : " + btn, MsgCodes.info4);
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
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "End Debug functionality on selection.",MsgCodes.info4);
	}
	
	@Override
	public final void handleSideMenuDebugSelDisable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable","Click Debug functionality off in " + name + " : btn : " + btn, MsgCodes.info4);
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
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "End Debug functionality off selection.",MsgCodes.info4);
	}

	
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	//alt key pressed handles trajectory
	
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
			
		return false;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		return res;}	
	@Override
	protected void hndlMouseRelIndiv() {	}
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
	protected final void initDrwnTrajIndiv() {}
	@Override
	protected final void addSScrToWinIndiv(int newWinKey) {}
	@Override
	protected final void addTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	@Override
	protected final void delSScrToWinIndiv(int idx) {}
	@Override
	protected final void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	@Override
	public final void processTrajIndiv(myDrawnSmplTraj drawnTraj) {}


}//DESSimWindow

