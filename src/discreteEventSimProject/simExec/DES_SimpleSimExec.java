package discreteEventSimProject.simExec;

import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.sim.Base_Simulator;
import discreteEventSimProject.sim.layouts.SimpleDesSim;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

public class DES_SimpleSimExec extends Base_DESSimExec {	
	
	public DES_SimpleSimExec(Base_DispWindow _win, String _name, int _numSims) {
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
	protected final Base_Simulator buildSimOfType(String name, int _type) {
		return new SimpleDesSim(this, name, 100, _type);
	}

}//class DES_SimpleSimExec