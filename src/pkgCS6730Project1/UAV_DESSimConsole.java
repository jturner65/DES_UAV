package pkgCS6730Project1;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;

/**
 * console version of Discrete Event Simulator with UAV teams performing tasks
 * @author john
 *
 */
public class UAV_DESSimConsole {
	
	//testType == 0 is regular fixed length experiment
	//testType == 1 is FEL test
	//testType == 2 is test of priority queue functionality
	public static void simLoop(mySimExecutive simExec, int testType) {
		//modAmtMillis is delta T between each "frame" in graphical simulation
		float modAmtMillis = 33.0f;//33 milliseconds
		boolean doneExp = false;//done with experiment?
		while  (!doneExp) {
			doneExp = simExec.simMe(modAmtMillis);
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
		float frtMult = 1000.0f;		//framerate multipler 1-> 10,000; 1 is real time - how much time passes for each frame
		int numUAVPerTeam = 4;			//default # of uavs per team
		int minsToRun = 720;			//number of minutes simulation should last. (min is 1, max is 525600 (year in sim time))
		int numTrials = 10;
		//this value is equivalent of 12 hours
		boolean doPQTest = false, doFELTest = false, doSimEnvTest = false;
		
		for (int idx = 0;idx<args.length; ++idx) {
			String arg = args[idx];
			switch (arg) {
			case "-h" : {
				System.out.println("Command line args should be in the following format : ");
				System.out.println("-h : this help message");
				System.out.println("-t : timeStep multiplier, valid values are 1-10000.  value 1 is real time, value 10000 is 10000x speed up");
				System.out.println("-u : uav team size, valid values are 2-9");
				System.out.println("-m : number of minutes simulation should last. (min is 1, max is 525600 (year in sim time))"); 
				System.out.println("-n : number of experimental trials that should be conducted (min is 1, max is 100)"); 
				System.out.println("---The following are tests, and a regular simulation will not be run");
				System.out.println("-p : test integrity of priority queue code - all other args are bypassed if this is set"); 
				System.out.println("-f : run test of integrity of FEL before and after multiple events have been processsed");
				System.out.println("-s : display state of simulator after being built");					
				System.exit(0);}	
			case "-t" : {
				if(idx+1 >= args.length) {break;}
				frtMult = Float.parseFloat(args[idx+1]);
				if ((frtMult < 1) || (frtMult > 10000.0f)){frtMult = 1000.0f;}
				break;}
			case "-u" : {
				if(idx+1 >= args.length) {break;}
				numUAVPerTeam = Integer.parseInt(args[idx+1]);
				if ((numUAVPerTeam < 2) || (numUAVPerTeam > 9)){numUAVPerTeam = 4;}						
				break;}					
			case "-m" : {
				if(idx+1 >= args.length) {break;}
				minsToRun = Integer.parseInt(args[idx+1]);
				if ((minsToRun < 1) || (minsToRun > 525600)){minsToRun = 1;}						
				break;}
			case "-n" : {
				if(idx+1 >= args.length) {break;}
				numTrials = Integer.parseInt(args[idx+1]);
				if ((numTrials < 1) || (numTrials > 100)){numTrials = 1;}						
				break;}
			case "-p" : {				doPQTest = true;		break;}
			case "-f" : {				doFELTest = true;		break;}
			case "-s" : {				doSimEnvTest = true;	break;}
			}
		}//for all args			
		
		//make actual # of loop iterations scaled by frame rate multiplier
		//set default values
		
		IRenderInterface dummy = null;
		mySimulator.uavTeamSize = numUAVPerTeam;
		mySimExecutive.frameTimeScale = frtMult;
	
		//instance sim exec and run loop
		mySimExecutive simExec = new mySimExecutive(dummy);
		mySimulator des = new simpleDesSim(simExec, 100);
		simExec.initSimWorld(des, true);
		
		int runType = 0;
		if(doFELTest){//test future event list after running loop for a few iterations			
			//initialize experimental trials
			simExec.initializeTrials(60,1);
			runType = 1;
		} else if(doPQTest){//test priority queue, don't need to run the sim
			//initialize experimental trials
			simExec.initializeTrials(1,1);
			runType = 2;
		} else if(doSimEnvTest){	//test environment, run sim for a few iterations
			//initialize experimental trials
			simExec.initializeTrials(60,1);
			runType = 3;
		} else {
			//initialize experimental trials
			simExec.initializeTrials(minsToRun,numTrials);
		}
		simLoop(simExec, runType);
	}//main

}//class
