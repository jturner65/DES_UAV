package discreteEventSimProject.sim.base;

import java.util.Map;

import base_Utils_Objects.sim.Base_SimDataUpdater;
import base_Utils_Objects.simExec.Base_SimExec;

public class DES_SimDataUpdater extends Base_SimDataUpdater {

	public DES_SimDataUpdater(Base_SimExec _simExec) {
		super(_simExec);
	}

	public DES_SimDataUpdater(Base_SimExec _simExec, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_simExec, _iVals, _fVals, _bVals);
	}

	public DES_SimDataUpdater(Base_SimDataUpdater _otr) {super(_otr);}

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

}//class DES_SimDataUpdater
