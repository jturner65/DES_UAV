package pkgCS6730Project1;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.myDispWindow;
import processing.core.*;

/**
 * CS6730 Project 1 : UAV Discrete Event Simulator
 * @author john turner
 */

public class UAV_DESSim extends my_procApplet {

	public String prjNmLong = "CS6730 Project 1 : UAV Des Simulation", prjNmShrt = "Prj1_UAV_DESSim";
	public String authorString = "John Turner";
	public final int[] bground = new int[]{244,244,255,255};		//bground color
	
	private int[] visFlags;
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
	    String[] appletArgs = new String[] { "pkgCS6730Project1.UAV_DESSim" };
	    if (passedArgs != null) {	    	PApplet.main(PApplet.concat(appletArgs, passedArgs));  } else {	    	PApplet.main(appletArgs);	    }
	}

	@Override
	protected int[] getDesiredAppDims() {return new int[] {(int)(getDisplayWidth()*.95f), (int)(getDisplayHeight()*.92f)};}

	@Override
	protected void setup_indiv() {		setBkgrnd(); setDesired3DGridDims(1500);}
	@Override
	public void setBkgrnd(){background(bground[0],bground[1],bground[2],bground[3]);}//setBkgrnd	

	@Override
	protected void initMainFlags_Priv() {
		setMainFlagToShow_debugMode(true);
		setMainFlagToShow_runSim(true);
		setMainFlagToShow_singleStep(true);
		setMainFlagToShow_showRtSideMenu(true);
	}

	@Override
	protected void initVisOnce_Priv() {
		showInfo = true;
		drawnTrajEditWidth = 10;
		//includes 1 for menu window (never < 1) - always have same # of visFlags as myDispWindows
		int numWins = numVisFlags;		
		//titles and descs, need to be set before sidebar menu is defined
		String[] _winTitles = new String[]{"","2D COTS Morph","3D COTS Morph"},//,"SOM Map UI"},
				_winDescr = new String[] {"","Display 2 COTS patches and the morph between them","Display 2 COTS patches in 3D and the morph between them"};
		initWins(numWins,_winTitles, _winDescr);
		//call for menu window
		buildInitMenuWin(showUIMenu);
		//menu bar init
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = new DES_SimSideBarMenu(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);	
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, width-menuWidth, height}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth, height};	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//String _ttl, String _desc 			: window title and description
		//boolean[] _dispFlags 					: 
		//   flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)
		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{showDESwin},new int[]{dispDES_SimWin});

		wIdx = dispDES_SimWin; fIdx= showDESwin;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{210,220,250,255},new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new DESSimWindow(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);		

		
	}

	@Override
	protected void initOnce_Priv() {
		//which objects to initially show
		setVisFlag(showUIMenu, true);					//show input UI menu	
		//setVisFlag(showSpereAnimRes, true);
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

	@Override
	public void initVisFlags() {
		visFlags = new int[1 + numVisFlags/32];for(int i =0; i<numVisFlags;++i){forceVisFlag(i,false);}	
		((DES_SimSideBarMenu)dispWinFrames[dispMenuIDX]).initPFlagColors();			//init sidebar window flags
	}

	@Override
	public void setVisFlag(int idx, boolean val) {
		int flIDX = idx/32, mask = 1<<(idx%32);
		visFlags[flIDX] = (val ?  visFlags[flIDX] | mask : visFlags[flIDX] & ~mask);
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
		case 'S' : {save(getScreenShotSaveName(prjNmShrt));break;}//save picture of current image			
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
	public String[] getMouseOverSelBtnNames() {
		return new String[0]; 
	}

	@Override
	//these tie using the UI buttons to modify the window in with using the boolean tags - PITA but currently necessary
	public void handleShowWin(int btn, int val, boolean callFlags){//display specific windows - multi-select/ always on if sel
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			setMenuBtnState(DES_SimSideBarMenu.btnShowWinIdx,btn, val);
		} else {//called from clicking on buttons in UI
		
			//val is btn state before transition 
			boolean bVal = (val == 1?  false : true);
			//each entry in this array should correspond to a clickable window, not counting menu
			setVisFlag(btn+1, bVal);
		}
	}//handleShowWin
	
	
	@Override
	//get vis flag
	public boolean getVisFlag(int idx){int bitLoc = 1<<(idx%32);return (visFlags[idx/32] & bitLoc) == bitLoc;}	
	@Override
	public void forceVisFlag(int idx, boolean val) {
		int flIDX = idx/32, mask = 1<<(idx%32);
		visFlags[flIDX] = (val ?  visFlags[flIDX] | mask : visFlags[flIDX] & ~mask);
		//doesn't perform any other ops - to prevent looping
	}
	@Override
	protected int[] getClr_Custom(int colorVal, int alpha) {	return new int[] {255,255,255,alpha};}

	@Override
	protected void setSmoothing() {
		noSmooth();
		
	}

	
}//papplet class





