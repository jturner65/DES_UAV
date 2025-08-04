package discreteEventSimProject.ui;

import java.util.LinkedHashMap;

import base_Render_Interface.IGraphicsAppInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.simulationUI.simExec.Base_UISimExec;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
import discreteEventSimProject.simExec.DES_SimpleSimExec;
import discreteEventSimProject.ui.base.Base_DESWindow;

/**
 * This window describes certain statically defined resource configurations
 * @author John Turner
 *
 */
public class StaticDESWindow extends Base_DESWindow {
    public final static int 
        gIDX_tmpIDX = numBaseDESGUIObjs;
    
    public final int numStaticPrivFlags = numBaseDesPrivFlags;
    /**
     * @param _p
     * @param _AppMgr
     * @param _winIdx
     */
    public StaticDESWindow(IGraphicsAppInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
        super(_p, _AppMgr, _winIdx);
        super.initThisWin(false);
    }

    /**
     * Static sims will be simple due to the limited number of tasks and lanes
     */
    @Override
    protected boolean isSimpleSim() {        return true;    }
    
    /**
     * Instance-specific initialization
     */
    @Override
    protected final void initMeSim_Indiv() {        
    }
        
    /**
     * Instance-specific boolean flags to handle.  Returns false if it does not handle passed index 
     * @param idx
     * @param val
     * @param oldVal
     * @return
     */
    @Override
    protected final boolean handleDesPrivFlags_Indiv(int idx, boolean val, boolean oldVal) {
        return false;
    }

    @Override
    protected final void resetDesFlags_Indiv() {
        
    }
    /**
     * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
     */
    @Override
    public int getTotalNumOfPrivBools(){        return numStaticPrivFlags;    }
    
    /**
     * Build all UI objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
     * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
     *             - The object IDX                   
     *          - A double array of min/max/mod values                                                   
     *          - The starting value                                                                      
     *          - The label for object                                                                       
     *          - The object type (GUIObj_Type enum)
     *          - A boolean array of behavior configuration values : (unspecified values default to false)
     *               idx 0: value is sent to owning window,  
     *               idx 1: value is sent on any modifications (while being modified, not just on release), 
     *               idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
     *          - A boolean array of renderer format values :(unspecified values default to false) - Behavior Boolean array must also be provided!
     *                 - Should be multiline
     *                 - One object per row in UI space (i.e. default for multi-line and btn objects is false, single line non-buttons is true)
     *                 - Force this object to be on a new row/line (For side-by-side layouts)
     *                 - Text should be centered (default is false)
     *                 - Object should be rendered with outline (default for btns is true, for non-buttons is false)
     *                 - Should have ornament
     *                 - Ornament color should match label color 
     */
    @Override
    protected final void setupGUIObjsAras_SimIndiv(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap) {}
    
    /**
     * Build all UI buttons to be shown in left side bar menu for this window. This is for instancing windows to add to button region
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    @Override
    protected final void setupGUIBoolSwitchAras_SimIndiv(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {}
    
    @Override
    protected final boolean setUI_IntDESValsCustom(int UIidx, int ival, int oldVal) {return false;}

    @Override
    protected boolean setUI_FloatDESValsCustom(int UIidx, float val, float oldVal) {return false;}

    /**
     * Build the executive managing the simulations owned by this window
     * @param _simName base name of simulation the sim exec manages
     * @param _numSimulations
     * @return
     */
    @Override
    protected Base_UISimExec buildSimulationExecutive(String _simName, int _numSimulations) {
        return new DES_SimpleSimExec(this, _simName, _numSimulations);
    }
    
}//class StaticDESWindow
