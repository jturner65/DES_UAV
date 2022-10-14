package pkgCS6730Project1;

import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.myDispWindow;
import pkgCS6730Project1.ui.DESSimWindow;

/**
 * CS6730 Project 1 : UAV Discrete Event Simulator
 * @author john turner
 */

public class UAV_DESSim extends GUI_AppManager {

	public String prjNmLong = "CS6730 Project 1 : UAV Des Simulation", prjNmShrt = "Prj1_UAV_DESSim";
	public String authorString = "John Turner";
	public final int[] bground = new int[]{244,244,255,255};		//bground color
	
	private final int
		showUIMenu 		= 0,			//whether or not to show sidebar menu
		showDESwin		= 1;			//whether to show 1st window

	public final int numVisFlags = 2;
	
	//idx's in dispWinFrames for each window - 0 is always left side menu window
	private static final int
		dispDES_SimWin = 1
		;
	
///////////////
//CODE STARTS
///////////////	
	//////////////////////////////////////////////// code
	public static void main(String[] passedArgs) {
	    UAV_DESSim me = new UAV_DESSim();
	    UAV_DESSim.invokeProcessingMain(me, passedArgs);	
	}
	@Override
	protected void setRuntimeArgsVals(String[] _passedArgs) {
	}


	/**
	 * whether or not we want to restrict window size on widescreen monitors
	 * 
	 * @return 0 - use monitor size regardless
	 * 			1 - use smaller dim to be determine window 
	 * 			2+ - TBD
	 */
	@Override
	protected int setAppWindowDimRestrictions() {	return 1;}	
	
	
	@Override
	protected void setup_Indiv() {		setBkgrnd(); setDesired3DGridDims(1500);}
	@Override
	public void setBkgrnd(){pa.setRenderBackground(bground[0],bground[1],bground[2],bground[3]);}//setBkgrnd	

	@Override
	protected void initMainFlags_Indiv() {
		setMainFlagToShow_debugMode(true);
		setMainFlagToShow_runSim(true);
		setMainFlagToShow_singleStep(true);
		setMainFlagToShow_showRtSideMenu(true);
	}

	@Override
	protected void initAllDispWindows() {
		showInfo = true;
		//includes 1 for menu window (never < 1) - always have same # of visFlags as myDispWindows
		int numWins = numVisFlags;		
		//titles and descs, need to be set before sidebar menu is defined
		String[] _winTitles = new String[]{"","UAV DES Sim"},//,"SOM Map UI"},
				_winDescr = new String[] {"","Display UAV Discrete Event Simulator"};
		initWins(numWins,_winTitles, _winDescr);
		//call for menu window
		buildInitMenuWin(showUIMenu);
		//menu bar init
		String[] menuBtnTitles = new String[]{"Sim Map","Functions 2","Functions 3","Functions 4"};
		String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar
			// menu, or ignored
			{ "Simple SIM", "Complex SIM", "---"}, // row 1
			{ "---", "---", "---", "---" }, // row 2
			{ "---", "---", "---", "---" }, // row 3
			{"Verify PQ", "Verify FEL", "Show Sim", "Test Tasks"}
		};				
		
		String[] menuDbgBtnNames = new String[] {};//must have literals for every button or this is ignored
		
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = buildSideBarMenu(wIdx, fIdx, menuBtnTitles, menuBtnNames, menuDbgBtnNames, false, false);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, pa.getWidth()-menuWidth, pa.getHeight()}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth, pa.getHeight()};	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//String _ttl, String _desc 			: window title and description
		//boolean[] _dispFlags 					: 
		//   flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)

		wIdx = dispDES_SimWin; fIdx= showDESwin;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{210,220,250,255},new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new DESSimWindow(pa, this, wIdx, fIdx);		
		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{showDESwin},new int[]{dispDES_SimWin});
		
	}

	@Override
	protected void initOnce_Indiv() {
		//which objects to initially show
		setVisFlag(showUIMenu, true);					//show input UI menu
		setVisFlag(showDESwin, true);
	}
	@Override
	//called multiple times, whenever re-initing
	protected void initProgram_Indiv(){	}//initProgram	
	@Override
	protected void initVisProg_Indiv() {}	
	@Override
	protected String getPrjNmLong() {return prjNmLong;}
	@Override
	protected String getPrjNmShrt() {		return prjNmShrt;	}

	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////
	
	/**
	 * Individual extending Application Manager post-drawMe functions
	 * @param modAmtMillis
	 * @param is3DDraw
	 */
	@Override
	protected void drawMePost_Indiv(float modAmtMillis, boolean is3DDraw) {}
	
	/**
	 * return the number of visible window flags for this application
	 * @return
	 */
	@Override
	public int getNumVisFlags() {return numVisFlags;}
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	protected void setVisFlag_Indiv(int idx, boolean val ){
		switch (idx){
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setFlags(myDispWindow.showIDX,val);    break;}											//whether or not to show the main ui window (sidebar)			
			case showDESwin			: { setWinFlagsXOR(dispDES_SimWin, val); break;}//setDispAndModMapMgr(showCOTS_2DMorph, dispCOTS_2DMorph, val);break;}
			default : {break;}
		}
	}

	@Override
	protected void handleKeyPress(char key, int keyCode) {
		switch (key){
		case ' ' : {toggleSimIsRunning(); break;}							//run sim
		case 'f' : {dispWinFrames[curFocusWin].setInitCamView();break;}					//reset camera
		case 'a' :
		case 'A' : {toggleSaveAnim();break;}						//start/stop saving every frame for making into animation
		case 's' :
		case 'S' : {break;}//save(getScreenShotSaveName(prjNmShrt));break;}//save picture of current image			
		default : {	}
	}//switch	
	}
	
	
	@Override
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){return ((altIsPressed() ? .1 : 1.0) * (shiftIsPressed() ? 10.0 : 1.0));}	
	//keys/criteria are present that means UI objects are modified by set values based on clicks (as opposed to dragging for variable values)
	//to facilitate UI interaction non-mouse computers, set these to be single keys
	@Override
	public boolean isClickModUIVal() {
		//TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or conventient
		return altIsPressed() || shiftIsPressed();		
	}

	@Override
	public float[] getUIRectVals(int idx) {
		//this.pr("In getUIRectVals for idx : " + idx);
		switch(idx){
		case dispMenuIDX 				: { return new float[0];}			//idx 0 is parent menu sidebar
		case dispDES_SimWin				: { return dispWinFrames[dispMenuIDX].uiClkCoords;}
		default :  return dispWinFrames[dispMenuIDX].uiClkCoords;
		}
	}
	
	/**
	 * present an application-specific array of mouse over btn names 
	 * for the selection of the desired mouse over text display - if is length 0 or null, will not be displayed
	 */
	@Override
	public String[] getMouseOverSelBtnLabels() {
		return new String[0]; 
	}

	@Override
	//these tie using the UI buttons to modify the window in with using the boolean tags - PITA but currently necessary
	public void handleShowWin(int btn, int val, boolean callFlags){//display specific windows - multi-select/ always on if sel
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			//setMenuBtnState(mySideBarMenu.btnShowWinIdx,btn, val);
		} else {//called from clicking on buttons in UI
		
			//val is btn state before transition 
			boolean bVal = (val == 1?  false : true);
			//each entry in this array should correspond to a clickable window, not counting menu
			setVisFlag(winFlagsXOR[btn], bVal);
			//setVisFlag(btn+1, bVal);
		}
	}//handleShowWin
	
	
	@Override
	public int[] getClr_Custom(int colorVal, int alpha) {	return new int[] {255,255,255,alpha};}

	@Override
	protected void setSmoothing() {pa.setSmoothing(0);		}

	
}//papplet class





