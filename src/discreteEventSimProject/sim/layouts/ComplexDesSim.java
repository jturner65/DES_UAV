package discreteEventSimProject.sim.layouts;

import java.util.concurrent.ThreadLocalRandom;

import discreteEventSimProject.sim.base.DES_Simulator;
import discreteEventSimProject.simExec.base.Base_DESSimExec;

/**
 * randomly generate a simulation that is very complex
 * @author John Turner
 *
 */
public class ComplexDesSim extends DES_Simulator{
	
	protected float simGridDims = 600.0f;
	/**
	 * # of tasks per side of cube
	 */
	protected int numPerSide = 4;
	
	public ComplexDesSim(Base_DESSimExec _exec, String _name, int _maxNumUAVs, int _simLayoutToUse) {
		super(_exec, _name, _maxNumUAVs, _simLayoutToUse);
		initSim();
	}
	
	/**
	 * setup task and transit lane connections and locations, along with opt team size and opt team 
	 * TTC in mins initialize sizes and locations of tasks and transit lanes
	 */
	@Override
	protected void initSim_Concrete() {
		
		// use simLayoutToUse value to determine which sim to build
		numPerSide += simLayoutToUse;
		simGridDims += (simLayoutToUse * 100.0f);
		//set tasks in a dense 3d grid DAG
		
		//predefine array of location point idxs that will be used to build the dense grid DAG
		int[][][] locIdxs = new int[numPerSide][][];
		//find task location points and 3d array of point idxs
		//build array of all task locations, as well as populating 3d array of idxs for each point in dense grid
		taskLocs = buildDenseGridTaskLocs(locIdxs, simGridDims);
		//build 2d array of task lanes, with 2nd dim being [starting idx, ending idx] of task location lanes connect to 
		TL_taskIdxs = buildDenseGridTaskLanePts(locIdxs, taskLocs.length-1);
		
		taskOptSize = new int[taskLocs.length];
		optTeamTTCMins = new float[taskLocs.length];
		stdDevTTCMult = new float[taskLocs.length];
		taskOptSize[0] = 3;
		optTeamTTCMins[0] = 3;
		stdDevTTCMult[0] = 0.0f;		
		for(int i=1;i<taskLocs.length;++i) {
			taskOptSize[i] = (int)Math.round((ThreadLocalRandom.current().nextDouble(0,1) * 5)+1);
			optTeamTTCMins[i] = (float)(ThreadLocalRandom.current().nextDouble(0,1) * 30) + 5.0f;
			stdDevTTCMult[i] = 0.0f;		
		}
	}//initSimPriv
	
	/**
	 * Get sim grid dimensions
	 * @return
	 */
	public float getSimGridDims() {return simGridDims;}
	
	public void setSimGridDims(float _simGridDims) {simGridDims = _simGridDims;}
	
	
	public int getNumTasksPerSide() {return numPerSide;}
	
	public void setNumTasksPerSide(int _numPerSide) {numPerSide = _numPerSide;}
	
	
	/**
	 * determine which tasks are group tasks TODO change this to be some % of groups in map
	 */
	@Override
	protected boolean[] getIsGroupAra() {
		boolean [] isGrp = new boolean[taskLocs.length];
		for(int i=1;i<taskLocs.length-1;++i) {
			isGrp[i]= (ThreadLocalRandom.current().nextDouble(0,1) > .8);
			
		}
		//entry is not group
		isGrp[0] = false;
		//last is group
		isGrp[isGrp.length-2] = true;
		//eixt is not group
		isGrp[isGrp.length-1] = true;
		return isGrp;
	}

	@Override
	protected boolean handlePrivSimFlags_Indiv(int idx, boolean val, boolean oldVal) {	return false;}
	
}//complexDesSim
