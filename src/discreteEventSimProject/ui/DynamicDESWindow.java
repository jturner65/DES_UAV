package discreteEventSimProject.ui;

import java.util.LinkedHashMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.simulationUI.simExec.Base_UISimExec;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
import discreteEventSimProject.simExec.DES_ComplexSimExec;
import discreteEventSimProject.ui.base.Base_DESWindow;

public class DynamicDESWindow extends Base_DESWindow {
    public final static int 
        gIDX_tmpIDX = numBaseDESGUIObjs;
    
    public final int numDynamicPrivFlags = numBaseDesPrivFlags;
    
    public DynamicDESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
        super(_p, _AppMgr, _winIdx);
        super.initThisWin(false);
    }

    @Override
    protected void resetDesFlags_Indiv() {}

    /**
     * Dynamic sims are not simple, since their layouts are generated and can be very large
     */
    @Override
    protected boolean isSimpleSim() {        return false;    }
    @Override
    protected void initMeSim_Indiv() {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean handleDesPrivFlags_Indiv(int idx, boolean val, boolean oldVal) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
     */
    @Override
    public int getTotalNumOfPrivBools(){        return numDynamicPrivFlags;    }
    
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
    protected boolean setUI_IntDESValsCustom(int UIidx, int ival, int oldVal) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean setUI_FloatDESValsCustom(int UIidx, float val, float oldVal) {
        // TODO Auto-generated method stub
        return false;
    }
    /**
     * Build the executive managing the simulations owned by this window
     * @param _simName base name of simulation the sim exec manages
     * @param _numSimulations
     * @return
     */
    @Override
    protected Base_UISimExec buildSimulationExecutive(String _simName, int _numSimulations) {
        return new DES_ComplexSimExec(this, _simName, _numSimulations);
    }

}//class DynamicDESWindow
