package discreteEventSimProject;

import java.util.HashMap;

import base_UI_Objects.GUI_AppManager;
import base_Utils_Objects.io.messaging.MsgCodes;
import discreteEventSimProject.ui.DynamicDESWindow;
import discreteEventSimProject.ui.StaticDESWindow;

/**
 * CS6730 Project 1 : UAV Discrete Event Simulator
 * @author john turner
 */

public class UAV_DESSim extends GUI_AppManager {

    public final String prjNmShrt = "UAV_DESSim_UI";
    public final String prjNmLong = "UAV Discrete Event Simulation";
    public final String projDesc = "Demonstrate Future Event List-driven sequential discrete event simulation via UI.";
    
    public String authorString = "John Turner";
    public final int[] bground = new int[]{244,244,255,255};        //bground color

    private boolean useSphereBKGnd = false;
    
    private String bkSkyBox = "bkgrndTex.jpg";
    
    /**
     * size of 3d grid cube side
     */
    private final int GridDim_3D = 1500;
    
    /**
     * idx's in dispWinFrames for each window - 0 is always left side menu window
     * Side menu is dispMenuIDX == 0
     */
    private static final int
        dispDES_SimWin_1 = 1,
        dispDES_SimWin_2 = 2;
    
    /**
     * # of visible windows including side menu (always at least 1 for side menu)
     */
    private static final int numVisWins = 3;    
///////////////
//CODE STARTS
///////////////    
    //////////////////////////////////////////////// code
    public static void main(String[] passedArgs) {
        UAV_DESSim me = new UAV_DESSim();
        UAV_DESSim.invokeProcessingMain(me, passedArgs);    
    }
    
    protected UAV_DESSim() {super();}
    
    /**
     * Set various relevant runtime arguments in argsMap
     * @param _passedArgs command-line arguments
     */
    @Override
    protected HashMap<String,Object> setRuntimeArgsVals(HashMap<String, Object> _passedArgsMap) {
        return  _passedArgsMap;
    }
    
    /**
     * Called in pre-draw initial setup, before first init
     * potentially override setup variables on per-project basis.
     * Do not use for setting background color or Skybox anymore.
     *      (Current settings in ProcessingRenderer)     
     *      strokeCap(PROJECT);
     *      textSize(txtSz);
     *      textureMode(NORMAL);            
     *      rectMode(CORNER);    
     *      sphereDetail(4);     * 
     */
    @Override
    protected void setupAppDims_Indiv() {setDesired3DGridDims(GridDim_3D);}
    @Override
    protected boolean getUseSkyboxBKGnd(int winIdx) {    return useSphereBKGnd;}
    @Override
    protected String getSkyboxFilename(int winIdx) {    return bkSkyBox;}
    @Override
    protected int[] getBackgroundColor(int winIdx) {return bground;}
    @Override
    protected int getNumDispWindows() {    return numVisWins;    }
    
    /**
     * whether or not we want to restrict window size on widescreen monitors
     * 
     * @return 0 - use monitor size regardless
     *             1 - use smaller dim to be determine window 
     *             2+ - TBD
     */
    @Override
    protected int setAppWindowDimRestrictions() {    return 1;}    
    @Override
    public String getPrjNmShrt() {return prjNmShrt;}
    @Override
    public String getPrjNmLong() {return prjNmLong;}
    @Override
    public String getPrjDescr() {return projDesc;}    
    

    /**
     * Set minimum level of message object console messages to display for this application. If null then all messages displayed
     * @return
     */
    @Override
    protected final MsgCodes getMinConsoleMsgCodes() {return null;}
    /**
     * Set minimum level of message object log messages to save to log for this application. If null then all messages saved to log.
     * @return
     */
    @Override
    protected final MsgCodes getMinLogMsgCodes() {return null;}

    @Override
    protected void initBaseFlags_Indiv() {
        setBaseFlagToShow_debugMode(true);
        setBaseFlagToShow_runSim(true);
        setBaseFlagToShow_singleStep(true);
        setBaseFlagToShow_showRtSideMenu(true);        
        setBaseFlagToShow_showStatusBar(true);
        setBaseFlagToShow_showDrawableCanvas(false);
    }

    @Override
    protected void initAllDispWindows() {
        showInfo = true;
        //titles and descs, need to be set before sidebar menu is defined
        String[] _winTitles = new String[]{"","UAV DES Sim 1","UAV DES Sim 2"},
                _winDescr = new String[]{"","Display UAV Discrete Event Simulator 1","Display UAV Discrete Event Simulator 2"};

        //instanced window dims when open and closed - only showing 1 open at a time - and init cam vals
        float[][] _floatDims  = getDefaultWinAndCameraDims();    

        //menu bar init
        String[] menuBtnTitles = new String[]{"Sim Layouts","Functions 1","Functions 2","Verifications"};
        String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar
            // menu, or ignored
            { "Layout 0","Layout 1","Layout 2"}, // row 1
            { "---", "---", "---", "---" }, // row 2
            { "---", "---", "---", "---" }, // row 3
            {"Verify PQ", "Verify FEL", "Show Sim", "Test Tasks"}
        };                
        
        String[] menuDbgBtnNames = new String[]{};//must have literals for every button or this is ignored
        //build menu
        buildSideBarMenu(_winTitles, menuBtnTitles, menuBtnNames, menuDbgBtnNames, true, false);
        //define windows
        /**
         *  _winIdx The index in the various window-descriptor arrays for the dispWindow being set
         *  _title string title of this window
         *  _descr string description of this window
         *  _dispFlags Essential flags describing the nature of the dispWindow for idxs : 
         *         0 : dispWinIs3d, 
         *         1 : canDrawInWin; 
         *         2 : canShow3dbox (only supported for 3D); 
         *         3 : canMoveView
         *  _floatVals an array holding float arrays for 
         *                 rectDimOpen(idx 0),
         *                 rectDimClosed(idx 1),
         *                 initCameraVals(idx 2)
         *  _intClrVals and array holding int arrays for
         *                 winFillClr (idx 0),
         *                 winStrkClr (idx 1),
         *                 winTrajFillClr(idx 2),
         *                 winTrajStrkClr(idx 3),
         *                 rtSideFillClr(idx 4),
         *                 rtSideStrkClr(idx 5)
         *  _sceneCenterVal center of scene, for drawing objects (optional)
         *  _initSceneFocusVal initial focus target for camera (optional)
         */
        int wIdx = dispDES_SimWin_1;
        setInitDispWinVals(wIdx, _winTitles[wIdx], _winDescr[wIdx], getDfltBoolAra(true), _floatDims,        
                new int[][] {new int[]{210,240,250,255},new int[]{255,255,255,255},
                    new int[]{180,180,180,255},new int[]{100,100,100,255},
                    new int[]{0,0,0,200},new int[]{255,255,255,255}});

        setDispWindow(wIdx, new StaticDESWindow(ri, this, wIdx));        
        wIdx = dispDES_SimWin_2;
        setInitDispWinVals(wIdx, _winTitles[wIdx], _winDescr[wIdx], getDfltBoolAra(true), _floatDims,        
                new int[][] {new int[]{240,210,250,255},new int[]{255,255,255,255},
                    new int[]{180,180,180,255},new int[]{100,100,100,255},
                    new int[]{0,0,0,200},new int[]{255,255,255,255}});
        setDispWindow(wIdx, new DynamicDESWindow(ri, this, wIdx));        
        //specify windows that cannot be shown simultaneously here
        initXORWins(new int[]{dispDES_SimWin_1, dispDES_SimWin_2},new int[]{dispDES_SimWin_1, dispDES_SimWin_2});
        
    }//initAllDispWindows
    
    /**
     * Map indexed by window ID, holding an array of the titles (idx 0) and descriptions (idx 1) for every sub window
     * return null if none exist, and only put an entry in the map if one exists for that window
     * @return
     */
    @Override
    protected final HashMap<Integer, String[]> getSubWindowTitles(){ return null;}

    @Override
    protected void initOnce_Indiv() {
        //which objects to initially show
        setWinVisFlag(dispDES_SimWin_1, true);
        setShowStatusBar(true);
    }
    @Override
    //called multiple times, whenever re-initing
    protected void initProgram_Indiv(){    }//initProgram    

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
    public int getNumVisFlags() {return numVisWins;}
    @Override
    //address all flag-setting here, so that if any special cases need to be addressed they can be
    protected void setVisFlag_Indiv(int idx, boolean val ){
        switch (idx){
            case dispDES_SimWin_1            : { setWinFlagsXOR(dispDES_SimWin_1, val); break;}
            case dispDES_SimWin_2             : { setWinFlagsXOR(dispDES_SimWin_2, val); break;}
            default : {break;}
        }
    }

    @Override
    protected void handleKeyPress(char key, int keyCode) {
        switch (key){
        case ' ' : {toggleSimIsRunning(); break;}                            //run sim
        case 'f' : {getCurFocusDispWindow().setInitCamView();break;}                    //reset camera
        case 'a' :
        case 'A' : {toggleSaveAnim();break;}                        //start/stop saving every frame for making into animation
        case 's' :
        case 'S' : {break;}//save(getScreenShotSaveName(prjNmShrt));break;}//save picture of current image            
        default : {    }
    }//switch    
    }
    
    
    //keys/criteria are present that means UI objects are modified by set values based on clicks (as opposed to dragging for variable values)
    //to facilitate UI interaction non-mouse computers, set these to be single keys
    @Override
    public boolean isClickModUIVal() {
        //TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or conventient
        return altIsPressed() || shiftIsPressed();        
    }

    @Override
    public float[] getUIRectVals_Indiv(int idx, float[] menuClickDim) {
        //this.pr("In getUIRectVals for idx : " + idx);
        switch(idx){
        case dispDES_SimWin_1                : { return menuClickDim;}
        case dispDES_SimWin_2                : { return menuClickDim;}
        default :  return menuClickDim;
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
    public void setSmoothing() {ri.setSmoothing(0);        }

    @Override
    protected boolean showMachineData() {    return true;}

}//papplet class





