package discreteEventSimProject.sim.base;

import java.util.Map;

import base_Utils_Objects.sim.Base_SimDataAdapter;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

public class DES_SimDataUpdater extends Base_SimDataAdapter {

	public DES_SimDataUpdater(Base_DESSimExec _simExec) {
		super(_simExec);
	}

	public DES_SimDataUpdater(Base_DESSimExec _simExec, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_simExec, _iVals, _fVals, _bVals);
	}

	public DES_SimDataUpdater(DES_SimDataUpdater _otr) {super(_otr);}

	@Override
	protected void updateBoolValue_Indiv(int idx, boolean value) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void updateIntValue_Indiv(int idx, Integer value) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void updateFloatValue_Indiv(int idx, Float value) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Implementation-specific set draw vis idx to be val
	 * @param val
	 */
	public final boolean checkAndSetSimDrawVis(boolean val) {
		return checkAndSetBoolValue(Base_DESSimExec.drawVisIDX, val);		
	}
	
	/**
	 * Implementation-specific get is should draw vis
	 * @param val
	 */
	public final boolean getSimDrawVis() {
		return getBoolValue(Base_DESSimExec.drawVisIDX);
	}
	
}//class DES_SimDataUpdater
