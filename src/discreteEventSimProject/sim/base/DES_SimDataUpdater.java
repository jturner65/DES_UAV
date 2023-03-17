package discreteEventSimProject.sim.base;

import java.util.Map;

import base_UI_Objects.windowUI.simulation.uiData.Base_UISimDataAdapter;
import discreteEventSimProject.simExec.base.Base_DESSimExec;
/**
 * DES Simulation data adapter, to communicate simulation values and parameters between sim exec and sim instances
 * @author John Turner
 *
 */
public class DES_SimDataUpdater extends Base_UISimDataAdapter {

	public DES_SimDataUpdater(Base_DESSimExec _simExec) {
		super(_simExec);
	}

	public DES_SimDataUpdater(Base_DESSimExec _simExec, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_simExec, _iVals, _fVals, _bVals);
	}

	public DES_SimDataUpdater(DES_SimDataUpdater _otr) {super(_otr);}

	@Override
	protected void updateBoolValue_Indiv(int idx, boolean value) {}

	@Override
	protected void updateIntValue_Indiv(int idx, Integer value) {}

	@Override
	protected void updateFloatValue_Indiv(int idx, Float value) {}
	
}//class DES_SimDataUpdater
