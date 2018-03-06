package pkgCS6730Project1;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * class to describe a task - use as struct to interact with ui
 * @author john
 */
public class taskDesc {
	//these vals will not change until map rebuilt/reset
	//idx of the task described by this taskDesc
	public final int idx;	
	//location in world of this task
	public final myPointf taskLoc;
	//whether or not this task allows for multiple teams to consume simultaneously
	public final boolean isGroupTask;
	//radius of sphere representing this task in rendering and in map calculations
	public final float rad;
	//size of team required to complete this task at optimal per-uav speed (more will speed up completion on diminishing returns, less will slow down completion)
	public final int optUAVTeamSize;
	//time to complete resource task for team of size optUAVTeamSize - time is in milliseconds; minTimeToComplete is min time to complete task (hard threshold)
	public final float timeForOptToCmp, minTimeToComplete;
	//scaling factor for std for completion time - might need to tweak this depending on random completion time results.
	//this is so STD is dependent on team size, with smaller teams having larger stds than larger ones
	protected final float stdTCScaleFact;
	//this is exponent to be used for diminishing returns formula, based on optUAVTeamSize and timeToComplete
	protected final double scaleForOptTeam;
	//work per unit
	//# of uavs per team in this run - must be > 1
	public final int numUAVs;
	//avg completion time for this task with given # of uavs
	public final double meanCompTime;
	//standard deviation of this task's completion time
	public final double stdDev;
	//name
	public final String name;
		
	public taskDesc(int _idx, myPointf _tl, boolean _isGrp, 
					int _toSz, float _optTTC, float _stdMlt, 
					float _rad, int _numUAVs, boolean _show) {
		idx = _idx; 
		taskLoc = new myPointf(_tl); 
		isGroupTask=_isGrp; 
		name = (isGroupTask ? "GrpTask" : "Task") + "_"+(idx+1);
		optUAVTeamSize=_toSz; 
		timeForOptToCmp=_optTTC;
		stdTCScaleFact=_stdMlt; 
		rad=_rad;
		numUAVs=_numUAVs;
		minTimeToComplete = .1f * timeForOptToCmp;
		
		scaleForOptTeam  = Math.E/(optUAVTeamSize + 1.0);
		//using power of 1 for now TODO allow entry?  
		meanCompTime = calcMeanCompTime(1.0f, numUAVs);
		//smaller teams  have greater std, larger have less
		stdDev = (optUAVTeamSize * stdTCScaleFact)/(1.0f*numUAVs);
		if(_show) {
			System.out.println("TaskDesc made : " + this.toString());
		}
	}//ctor
		
	//using given exponent, calculate work done with specified optimal team size
	private double calcMeanCompTime(double calcExp, int _numUAVs) {
		//normalizer so optUAVTeamSize always completes task in timeForOptToCmp
		double ttcNormalizer = calcFunc((optUAVTeamSize + 1.0)*scaleForOptTeam, calcExp);		
		//scaling value of work per unit time for actual size of teams
		double workPerTime = calcFunc(scaleForOptTeam * (_numUAVs + 1.0),calcExp)/ ttcNormalizer;
		double muCmpTime = Math.abs(timeForOptToCmp / workPerTime);
		return muCmpTime;
	}//calcMeanCompTime
	
	//calculate diminishing returns function, based on passed power (n) and value (x)
	//allow sweep through powers for reporting
	private double calcFunc(double x, double n) {
		double res = 0.0;
		if (Math.abs(n-1.0) < .00001) {//==1->use natural log
			res = Math.log(x);
		} else {//function built as integral of diminishing returns always-positive derivative function 1/x^n
			res = (Math.pow(x, (1-n)) - 1.0)/(1-n);
		}
		return res;
	}//

	public long getCompletionTime() {
		double res = meanCompTime;
		if(stdTCScaleFact > 0) {//if we are using std then use calced value as mean of gaussian
			//smaller teams  have greater std, larger have less
			res = (ThreadLocalRandom.current().nextGaussian() * stdDev) + meanCompTime;
		}
		if (res < minTimeToComplete) {res = minTimeToComplete;}
		//round up to nearest long
		return (long) Math.ceil(res);
	}//getCompletionTime	
	
	//return an array of doubles that shows the effects of sweeping p (diminishing returns formula) and n(# of uavs in team) for this task
	//minP must be greater than 0, max P can be anything
	public double[][] getTaskPerfForNData(int minSize, int maxSize, double minP, double maxP, double pwrIncr) {
		int numPwrs = 1 + (int) ((maxP - minP)/pwrIncr);
		double[][] res = new double[numPwrs][];
		int numTmSz = maxSize-minSize + 1;
		for(int i=0;i<res.length;++i) {res[i]=new double[numTmSz];}		
		//conduct test
		int pIdx = 0;
		for(double pwr=minP; pwr<=maxP; pwr+= pwrIncr) {
			for(int n=minSize;n<=maxSize;++n) {res[pIdx][n] = calcMeanCompTime(pwr, n);}
			++pIdx;
		}
		return res;
	}//getTaskPerfForNData
	
	public static String getTaskPerfForNCSVHdr(int minSize, int maxSize, double minP, double maxP, double pwrIncr) {
		String res = "Pwr, Opt UAV Team Size, Comp Time For Opt Team, Min Time To Comp";
		for(int i=minSize;i<=maxSize;++i) {
			if(i<1) {continue;}//bad size
			res += ",Size="+i+", Mean Comp Time";			
		}
		return res;	
	}//getTaskPerfForNCSVHdr
	
	//return an array of comma sep strings per power for this task sweeping through team size
	public String[] getTaskPerfForNDataCSV(int minSz, int maxSz, double minP, double maxP, double pwrIncr) {
		ArrayList<String> tmpRes = new ArrayList<String>();
		//add header
		tmpRes.add(taskDesc.getTaskPerfForNCSVHdr(minSz,maxSz, minP, maxP, pwrIncr));
			//add results into string
		String resStr = "";
		for(double pwr=minP; pwr<=maxP; pwr+= pwrIncr) {
			resStr = "" + String.format("%2.2f", pwr)+","+optUAVTeamSize+","+String.format("%4.3f",timeForOptToCmp)+","+String.format("%4.3f",minTimeToComplete);
			for(int n=minSz;n<=maxSz;++n) {
				double mu = calcMeanCompTime(pwr, n);
				if(mu < 0) {
					System.out.println("neg completion time : " + n + " | " + pwr + " | ");
				}
				resStr +=","+n+","+ String.format("%4.3f",mu);
			}
			tmpRes.add(resStr);
		}
		return tmpRes.toArray(new String[0]);				
	}//getTaskPerfForNDataCSV
	
	//return a list of strings holding the results for this task description
	public String[] getTaskCompTimeData(int minSize, int maxSize, float sclFact, float pwr) {
		ArrayList<String> tmpRes = new ArrayList<String>();
		tmpRes.add("IDX : " + idx +" name : " + name + " | loc : " + taskLoc.toStrBrf() + " | is group task? " + isGroupTask + " | radius : " + String.format("%4.3f", rad));
		tmpRes.add("optUAVTeamSize : "+ optUAVTeamSize + " | timeForOptToCmp : " + String.format("%4.3f",timeForOptToCmp) + " | minTimeToComplete : " + String.format("%4.3f",minTimeToComplete));
		tmpRes.add("stdTCScaleFact : "+ String.format("%4.3f",stdTCScaleFact) + " | scaleForOptTeam : " + String.format("%4.3f",scaleForOptTeam));	
		for(int i=minSize;i<=maxSize;++i) {
			if(i<1) {continue;}//bad size
			double meanCT = calcMeanCompTime(pwr, i), std = (optUAVTeamSize * sclFact)/(1.0f*i);
			tmpRes.add("For "+i+" UAVs mean comp time : " +meanCT + " stdDev : "+std);		
		}		
		return tmpRes.toArray(new String[0]);		
	}//getExpRes
	
	//header for csv data
	public static String getTaskCompHeader_CSV(int minSize, int maxSize) {
		String res ="Idx, Name, LocX, LocY, LocZ, isGroupTask, Radius, Opt UAV Team Size, Comp Time For Opt Team, Min Time To Comp, stdTCScaleFact, scaleForOptTeam";
		for(int i=minSize;i<=maxSize;++i) {
			if(i<1) {continue;}//bad size
			res += ",sz "+i+", mean, stdev";		
		}	
		return res;
	}//getTaskCompHeader_CSV
	
	//return a string holding the results for this task description
	//same as above but in a single, comma sep string, without descriptors
	public String getTaskCompTimeDataCSV(int minSize, int maxSize, float sclFact, float pwr) {
		String res = ""+idx +"," + name + "," + String.format("%.4f",taskLoc.x) + ", " + String.format("%.4f",taskLoc.y) + ", " + String.format("%.4f",taskLoc.z) + "," 
				+ isGroupTask + "," + String.format("%4.3f", rad)+","+ optUAVTeamSize +"," + String.format("%4.3f",timeForOptToCmp) +"," 
				+ String.format("%4.3f",minTimeToComplete)+","+ String.format("%4.3f",stdTCScaleFact) +"," + String.format("%4.3f",scaleForOptTeam);
		for(int i=minSize;i<=maxSize;++i) {
			if(i<1) {continue;}//bad size
			double meanCT = calcMeanCompTime(pwr, i), std = (optUAVTeamSize * sclFact)/(1.0f*i);
			res += ","+i+"," +meanCT + ","+std;		
		}		
		return res;		
	}//getExpRes 
	
	public String toString() {
		String res = "IDX : " + idx +" name : " + name + " | loc : " + taskLoc.toStrBrf() + " | is group task? " + isGroupTask + " | radius : " + String.format("%4.3f", rad)+"\n";
		res+="\toptUAVTeamSize : "+ optUAVTeamSize + " | timeForOptToCmp : " + String.format("%4.3f",timeForOptToCmp) + " | minTimeToComplete : " + String.format("%4.3f",minTimeToComplete) +"\n";
		res+="\tstdTCScaleFact : "+ String.format("%4.3f",stdTCScaleFact) + " | scaleForOptTeam : " + String.format("%4.3f",scaleForOptTeam) +" numUAVs : " + numUAVs+"\n";
		res+="\tmeanCompTime : " + String.format("%4.3f",meanCompTime) + " | stdev " +  String.format("%4.3f",stdDev) +"\n";
		
		return res;
	}//toString()
	
}//taskDesc