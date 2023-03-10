package discreteEventSimProject.simExec;

import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.sim.Base_Simulator;
import discreteEventSimProject.sim.layouts.ComplexDesSim;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

public class DES_ComplexSimExec extends Base_DESSimExec {

	public DES_ComplexSimExec(Base_DispWindow _win, String _name, int _numSims) {
		super(_win, _name, _numSims);
		initSimExec();
	}
	/**
	 * Build an instance of a simulator that this Sim exec will manage
	 * @param name
	 * @param type
	 * @return
	 */
	@Override
	protected final Base_Simulator buildSimOfType(String name, int type) {
		return new ComplexDesSim(this, name, 5000, type);
	}

}//class DES_ComplexSimExec
