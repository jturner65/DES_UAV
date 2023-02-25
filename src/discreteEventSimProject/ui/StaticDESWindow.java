package discreteEventSimProject.ui;

import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.sim.layouts.SimpleDesSim;
import discreteEventSimProject.ui.base.Base_DESWindow;

/**
 * This window describes certain statically defined resource configurations
 * @author John Turner
 *
 */
public class StaticDESWindow extends Base_DESWindow {
	public final static int 
		gIDX_tmpIDX = numBaseGUIObjs;
	
	public final int numStaticPrivFlags = numPrivFlags;
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
	protected final int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
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
	protected final void initMe_Indiv() {
		
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
			TreeMap<Integer, String[]> tmpListObjVals) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected final boolean setUI_IntDESValsCustom(int UIidx, int ival, int oldVal) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean setUI_FloatDESValsCustom(int UIidx, float val, float oldVal) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected DES_Simulator buildSimOfType(int _type) {
		return new SimpleDesSim(simExec, 100, _type);
	}

	
}//class StaticDESWindow
