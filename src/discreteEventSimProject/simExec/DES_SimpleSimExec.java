package discreteEventSimProject.simExec;

import base_UI_Objects.windowUI.simulation.ui.Base_UISimWindow;
import base_Utils_Objects.sim.Base_Simulator;
import discreteEventSimProject.sim.layouts.SimpleDesSim;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

public class DES_SimpleSimExec extends Base_DESSimExec {	
	
	public DES_SimpleSimExec(Base_UISimWindow _win, String _name, int _numSims) {
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
	protected final Base_Simulator buildSimOfType(String _simName, int _type) {
		return new SimpleDesSim(this, _simName, 100, _type);
	}

}//class DES_SimpleSimExec
