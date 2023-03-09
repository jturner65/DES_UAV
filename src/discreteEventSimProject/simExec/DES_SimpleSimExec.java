package discreteEventSimProject.simExec;

import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.sim.Base_SimDataUpdater;
import base_Utils_Objects.sim.Base_Simulator;
import discreteEventSimProject.sim.base.DES_SimDataUpdater;
import discreteEventSimProject.sim.layouts.SimpleDesSim;
import discreteEventSimProject.simExec.base.DES_SimExec;

public class DES_SimpleSimExec extends DES_SimExec {	
	
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
	protected Base_Simulator buildSimOfType(String name, int _type) {
		return new SimpleDesSim(this, name, 100, _type);
	}
	
	@Override
	public Base_SimDataUpdater buildSimDataUpdater() {
		return new DES_SimDataUpdater(this);
	}
}//class DES_SimpleSimExec
