package discreteEventSimProject.simExec;

import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.sim.Base_SimDataUpdater;
import base_Utils_Objects.sim.Base_Simulator;
import discreteEventSimProject.sim.base.DES_SimDataUpdater;
import discreteEventSimProject.sim.layouts.ComplexDesSim;
import discreteEventSimProject.simExec.base.DES_SimExec;

public class DES_ComplexSimExec extends DES_SimExec {

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
	protected Base_Simulator buildSimOfType(String name, int type) {
		return new ComplexDesSim(this, name, 5000, type);
	}
	
	@Override
	public Base_SimDataUpdater buildSimDataUpdater() {
		return new DES_SimDataUpdater(this);
	}

}//class DES_ComplexSimExec
