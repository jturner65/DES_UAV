package pkgCS6730Project1.sim.layouts;

import base_Math_Objects.vectorObjs.floats.myPointf;
import pkgCS6730Project1.sim.mySimExecutive;
import pkgCS6730Project1.sim.base.mySimulator;

//DES sim specified in report
public class SimpleDesSim extends mySimulator{
	
	public SimpleDesSim(mySimExecutive _exec, int _numUAVs) {
		super(_exec, _numUAVs);		
	}
	
	//initialize DES Simulation- set up all resources - re call this every time new sim is being set up
	@Override
	protected void initSimPriv() {//
		//max dims are +/- 750 for x, y and z
		//locations of tasks - uses this to specify how large task array is
		taskLocs = new myPointf[]{
				new myPointf(400,400,-100),
				new myPointf(200,200,120),
				new myPointf(280,-200,110),
				new myPointf(-150,250, 110),
				new myPointf(-100,-500,110),
				new myPointf(-500, -100, 110),
				new myPointf(-200,-250, 100),
				new myPointf(-500,-500, 120),
				new myPointf(-600,-600, -100),			
		};	
		//optimal team size for each task
		taskOptSize = new int[] {4,2,3,1,3,5,4,4,4};
		//time required for optimal team size to complete task, in minutes.(mult by 60000 for milliseconds)
		optTeamTTCMins = new float[] {3,8,12,35,24,15,10,10,5};
		//stdev multiplier (if != 0 then uses TTC as mean of gaussian with stdev == mult * optTeamSize/teamSize)
		stdDevTTCMult = new float[] {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};			
		//transit lane : idxs of tasks connecting each lane (parent, child) - uses this to specify how large transit lane array is
		TL_taskIdxs = new int[][] {{0,1},{1,2},{1,3},{2,4},{2,6},{3,5}, {3,6},{4,7}, {5,7}, {6,7},{7,8}};

	}//initSim

	//determine which tasks are group tasks
	@Override
	protected boolean[] getIsGroupAra() {
		boolean [] isGrp = new boolean[taskLocs.length];
		//last 2 are groups
		isGrp[isGrp.length-2] = true;
		isGrp[isGrp.length-1] = true;
		return isGrp;
	}
	
	
}//SimpleDesSim