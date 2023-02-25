package discreteEventSimProject.ui;

import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.sim.layouts.ComplexDesSim;
import discreteEventSimProject.ui.base.Base_DESWindow;

public class DynamicDESWindow extends Base_DESWindow {
	public final static int 
		gIDX_tmpIDX = numBaseGUIObjs;
	
	public final int numDynamicPrivFlags = numPrivFlags;
	
	public DynamicDESWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
		super.initThisWin(false);
	}

	@Override
	protected int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
		return numDynamicPrivFlags;
	}

	@Override
	protected void resetDesFlags_Indiv() {
		// TODO Auto-generated method stub

	}

	@Override
	protected DES_Simulator buildSimOfType(int _type) {
		// TODO Auto-generated method stub
		return new ComplexDesSim(simExec, 5000, _type);
	}


	/**
	 * Dynamic sims are not simple, since their layouts are generated and can be very large
	 */
	@Override
	protected boolean isSimpleSim() {		return false;	}
	@Override
	protected void initMe_Indiv() {
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

}
