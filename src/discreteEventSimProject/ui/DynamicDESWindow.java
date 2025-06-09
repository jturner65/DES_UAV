package discreteEventSimProject.ui;

import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.simulationUI.simExec.Base_UISimExec;
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
	protected boolean isSimpleSim() {		return false;	}
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
	public int getTotalNumOfPrivBools(){		return numDynamicPrivFlags;	}
	
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
	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals, int firstBtnIDX, TreeMap<Integer, Object[]> tmpBtnNamesArray) {}

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
