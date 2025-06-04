package discreteEventSimProject.ui;

import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.simulation.simExec.Base_UISimExec;
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
	protected int initSimPrivBtns_Indiv(TreeMap<Integer, Object[]> tmpBtnNamesArray) {
		return numDynamicPrivFlags;
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

	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray,
			TreeMap<Integer, String[]> tmpListObjVals) {
		// TODO Auto-generated method stub

	}

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
