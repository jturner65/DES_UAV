package discreteEventSimProject.ui;

import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.simulation.simExec.Base_UISimExec;
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
	public StaticDESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
		super.initThisWin(false);
	}

	/**
	 * Instance-specific button instantiation. Should return total number of buttons/booleans
	 */
	@Override
	protected final int initSimPrivBtns_Indiv(TreeMap<Integer, Object[]> tmpBtnNamesArray) {
		return numStaticPrivFlags;
	}
	/**
	 * Static sims will be simple due to the limited number of tasks and lanes
	 */
	@Override
	protected boolean isSimpleSim() {		return true;	}
	
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

	@Override
	protected final void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray,
			TreeMap<Integer, String[]> tmpListObjVals) {}

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
