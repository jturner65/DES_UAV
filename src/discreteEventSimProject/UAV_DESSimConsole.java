package discreteEventSimProject;

import java.util.ArrayList;
import java.util.HashMap;

import base_Utils_Objects.appManager.Console_AppManager;
import base_Utils_Objects.appManager.argParse.cmdLineArgs.base.Base_CmdLineArg;
import base_Utils_Objects.io.messaging.MsgCodes;
import discreteEventSimProject.simExec.DES_SimpleSimExec;

/**
 * console version of Discrete Event Simulator with UAV teams performing tasks
 * @author john
 *
 */
public class UAV_DESSimConsole extends Console_AppManager {
	//project-specific variables
	private final String prjNmShrt = "UAV_DESSim";
	private final String prjNmLong = "UAV Discrete Event Simulation";
	private final String projDesc = "Console-driven demonstration of Future Event List-driven sequential discrete event simulation.";

	public UAV_DESSimConsole() {
		super();
	}
	
	
	//testType == 0 is regular fixed length experiment
	//testType == 1 is FEL test
	//testType == 2 is test of priority queue functionality
	public static void simLoop(DES_SimpleSimExec simExec, int testType) {
		//modAmtMillis is delta T between each "frame" in graphical simulation
		float modAmtMillis = 33.0f;//33 milliseconds
		boolean doneExp = false;//done with experiment?
		while  (!doneExp) {
			doneExp = simExec.stepSimulation(modAmtMillis);
		}				
		switch(testType) {
			case 0 :{				
				return;}
			case 1 :{//FEL test 
				simExec.TEST_verifyFEL();				
				return;}
			case 2 :{							
				simExec.TEST_verifyPriorityQueueFunctionality();return;}
			case 3 :{//sim environment tester				
				simExec.TEST_simulator();				
			}
		}		
	}//	
	
	protected void initExec() {
		HashMap<String, Object> argsMap = getArgsMap();
		
		//instance sim exec and run loop
		DES_SimpleSimExec simExec = new DES_SimpleSimExec(null, "UAV_DESSimConsole", 1);
		simExec.setTimeScale((Float)argsMap.get("frameTimeScale"));
		simExec.createAllSims();
		simExec.setSimUAVTeamSize((int)argsMap.get("numUAVPerTeam"));
//		DES_Simulator des = new SimpleDesSim(simExec, 100, 0);
//		des.setUavTeamSize(uavTeamSize);
		simExec.setSimAndInit(0, true);
		
		int runType = 0;
		if((boolean) argsMap.get("doFELTest")){//test future event list after running loop for a few iterations			
			//initialize experimental trials
			simExec.initializeTrials(60,1);
			runType = 1;
		} else if((boolean) argsMap.get("doPQTest")){//test priority queue, don't need to run the sim
			//initialize experimental trials
			simExec.initializeTrials(1,1);
			runType = 2;
		} else if((boolean) argsMap.get("doSimEnvTest")){	//test environment, run sim for a few iterations
			//initialize experimental trials
			simExec.initializeTrials(60,1);
			runType = 3;
		} else {
			//initialize experimental trials
			simExec.initializeTrials((Integer)argsMap.get("minsToRun"),(Integer)argsMap.get("numTrials"));
		}
		simLoop(simExec, runType);
	}//initExec()
	
	/**
	 * Set various relevant runtime arguments in argsMap
	 * @param _passedArgs command-line arguments
	 */
	@Override
	protected HashMap<String,Object> setRuntimeArgsVals(HashMap<String, Object> _passedArgsMap) {
		return  _passedArgsMap;
	}
	
	/**
	 * Configure desired arguments for non-graphical execution of this program
	 * -t : timeStep multiplier, valid values are 1-10000.  value 1 is real time, value 10000 is 10000x speed up");
	 * -u : uav team size, valid values are 2-9");
	 * -m : number of minutes simulation should last. (min is 1, max is 525600 (year in sim time))"); 
	 * -n : number of experimental trials that should be conducted (min is 1, max is 100)"); 
	 * ---The following are tests, and a regular simulation will not be run");
	 * -p : test integrity of priority queue code - all other args are bypassed if this is set"); 
	 * -f : run test of integrity of FEL before and after multiple events have been processsed");
	 * -s : display state of simulator after being built");					
	 */
	@Override
	protected ArrayList<Base_CmdLineArg> getCommandLineParserAttributes() {
		ArrayList<Base_CmdLineArg> cmdArgs = new ArrayList<Base_CmdLineArg>();
		//timestep multiplier
		cmdArgs.add(buildFloatCommandLineArgDesc('t',"timestepMult", "frameTimeScale",
				"timeStep multiplier, valid values are 1-10000.  value 1 is real time, value 10000 is 10000x speed up",
				1000.0f, null, new Float[]{1.0f, 10000.0f}));
		
		//uav team size
		cmdArgs.add(buildIntCommandLineArgDesc('u', "uavTeamSize", "numUAVPerTeam",
				"uav team size, valid values are 2-9", 
				4, null, new Integer[] {2, 9}));
		
		//sim duration
		cmdArgs.add(buildIntCommandLineArgDesc('m', "duration", "minsToRun",
				"number of minutes simulation should last. (min is 1, max is 525600 (year in sim time))", 
				720, null, new Integer[] {1, 525600}));
		
		//Number of experimental trials
		cmdArgs.add(buildIntCommandLineArgDesc('n', "numTrials", "numTrials",
				"number of experimental trials that should be conducted (min is 1, max is 100)", 
				10, null, new Integer[] {1, 100}));
		
		cmdArgs.add(buildBoolCommandLineArgDesc('p', "testPQ", "doPQTest", 
				"test integrity of priority queue code - all other args are bypassed if this is set", false, null));
		cmdArgs.add(buildBoolCommandLineArgDesc('f', "testFel", "doFELTest", 
				"test integrity of FEL before and after multiple events have been processsed", false, null));
		cmdArgs.add(buildBoolCommandLineArgDesc('s', "testSimEnv", "doSimEnvTest", 
				"display state of simulator after being built", false, null));
		
		return cmdArgs;
	}

	@Override
	public String getPrjNmLong() {return prjNmLong;}
	@Override
	public String getPrjNmShrt() {return prjNmShrt;}
	@Override
	public String getPrjDescr() {return projDesc;}

	/**
	 * Set minimum level of message object console messages to display for this application. If null then all messages displayed
	 * @return
	 */
	@Override
	protected final MsgCodes getMinConsoleMsgCodes() {return null;}
	/**
	 * Set minimum level of message object log messages to save to log for this application. If null then all messages saved to log.
	 * @return
	 */
	@Override
	protected final MsgCodes getMinLogMsgCodes() {return null;}
	
	/**
	 * main for non-graphical execution of this program
	 * @param args
	 * args will be two sets of key-value pairs.  the key is prefixed with a dash
	 * -t : timeStep multiplier, valid values are 1-10000.  value 1 is real time, value 10000 is 10000x speed up");
	 * -u : uav team size, valid values are 2-9");
	 * -m : number of minutes simulation should last. (min is 1, max is 525600 (year in sim time))"); 
	 * -n : number of experimental trials that should be conducted (min is 1, max is 100)"); 
	 * ---The following are tests, and a regular simulation will not be run");
	 * -p : test integrity of priority queue code - all other args are bypassed if this is set"); 
	 * -f : run test of integrity of FEL before and after multiple events have been processsed");
	 * -s : display state of simulator after being built");					
	 */
	public static void main(String[] args) {
		UAV_DESSimConsole mainObj = new UAV_DESSimConsole();
		UAV_DESSimConsole.invokeMain(mainObj, args);
		mainObj.initExec();
	}//main


	@Override
	protected boolean showMachineData() {	return true;}

}//class
