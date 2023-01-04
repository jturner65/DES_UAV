package discreteEventSimProject.sim.layouts;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import base_Math_Objects.vectorObjs.floats.myPointf;
import discreteEventSimProject.sim.mySimExecutive;
import discreteEventSimProject.sim.base.mySimulator;

/**
 * randomly generate a simulation that is very complex
 * @author John Turner
 *
 */
public class ComplexDesSim extends mySimulator{

	public ComplexDesSim(mySimExecutive _exec, int _maxNumUAVs) {
		super(_exec, _maxNumUAVs);
	}
	//setup task and transit lane connections and locations, along with opt team size and opt team TTC in mins
	//initialize sizes and locations of tasks and transit lanes
	@Override
	protected void initSimPriv() {
		//locations of tasks - uses this to specify how large task array is
		//set tasks in a 3d grid
		myPointf stLoc = new myPointf(-600,-600,-600);
		myPointf endLoc = new myPointf(600,600,600);
		ArrayList<myPointf> tmpListLocs = new ArrayList<myPointf>();
		//idx in final array 
		//ArrayList<ArrayList<ArrayList<Integer>>> idxs = new ArrayList<ArrayList<ArrayList<Integer>>>();
		int idx = 1;
		tmpListLocs.add(stLoc);

		int[][][] locs = new int[6][][];
		int[][] locs_y = new int[6][];
		int[] locs_z = new int[6];
		//have a chance to make a task at each location
		for(int i=0; i<6; ++i) {	
			float xCoord = i*200 - 500.0f;
			locs_y = new int[6][];
			for(int j=0; j<6; ++j) {
				float yCoord = j*200 - 500.0f;
				locs_z = new int[6];
				for(int k=0; k<6; ++k) {
					float zCoord = k*200 -500.0f;
					locs_z[k] = idx++; 
					tmpListLocs.add(new myPointf(xCoord, yCoord, zCoord));
				}
				locs_y[j]=locs_z;
			}			
			locs[i]=locs_y;
		}
		tmpListLocs.add(endLoc);
		int[][] tl_idxs = new int[1500][];
		int idx_TL = 0;
		//connectstart to first 4
		tl_idxs[idx_TL++] = new int[] {0,locs[0][0][0]};					
		tl_idxs[idx_TL++] = new int[] {0,locs[1][0][0]};					
		tl_idxs[idx_TL++] = new int[] {0,locs[0][1][0]};					
		tl_idxs[idx_TL++] = new int[] {0,locs[0][0][1]};				
		
		//make links to all +1 coords (7 per node not counting bounds)
		for(int i=0; i<5; ++i) {	
			for(int j=0; j<5; ++j) {
				for(int k=0; k<5; ++k) {
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i][j][k+1]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i][j+1][k]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i][j+1][k+1]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i+1][j][k]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i+1][j][k+1]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i+1][j+1][k]}; 
					tl_idxs[idx_TL++] = new int[] {locs[i][j][k],locs[i+1][j+1][k+1]}; 
				}
				tl_idxs[idx_TL++] = new int[] {locs[i][j][5],locs[i][j+1][5]}; 
				tl_idxs[idx_TL++] = new int[] {locs[i][j][5],locs[i+1][j][5]}; 
				tl_idxs[idx_TL++] = new int[] {locs[i][j][5],locs[i+1][j+1][5]}; 				
			}	
			tl_idxs[idx_TL++] = new int[] {locs[i][5][5],locs[i+1][5][5]};			
		}
		for(int j=0; j<5; ++j) {
			for(int k=0; k<5; ++k) {
				tl_idxs[idx_TL++] = new int[] {locs[5][j][k],locs[5][j][k+1]};	
				tl_idxs[idx_TL++] = new int[] {locs[5][j][k],locs[5][j+1][k]};	
				tl_idxs[idx_TL++] = new int[] {locs[5][j][k],locs[5][j+1][k+1]};	
			}
			tl_idxs[idx_TL++] = new int[] {locs[5][j][5],locs[5][j+1][5]};	
		}
		for(int i=0;i<5;++i) {
			for(int k=0; k<5; ++k) {
				tl_idxs[idx_TL++] = new int[] {locs[i][5][k],locs[i][5][k+1]};				
				tl_idxs[idx_TL++] = new int[] {locs[i][5][k],locs[i+1][5][k]};				
				tl_idxs[idx_TL++] = new int[] {locs[i][5][k],locs[i+1][5][k+1]};				
			}
		}

		for(int k=0; k<5; ++k) {
			tl_idxs[idx_TL++] = new int[] {locs[5][5][k],locs[5][5][k+1]};					
		}
		for(int j=0;j<5; ++j) {
			tl_idxs[idx_TL++] = new int[] {locs[5][j][5],locs[5][j+1][5]};					
		}
		//connect last 4 to exit
		tl_idxs[idx_TL++] = new int[] {locs[5][5][5],tmpListLocs.size()-1};					
		tl_idxs[idx_TL++] = new int[] {locs[5][5][4],tmpListLocs.size()-1};					
		tl_idxs[idx_TL++] = new int[] {locs[5][4][5],tmpListLocs.size()-1};					
		tl_idxs[idx_TL++] = new int[] {locs[4][5][5],tmpListLocs.size()-1};	

		//System.out.println("final idx : " + idx_TL);
		TL_taskIdxs = new int[idx_TL][];		
		for(int i=0;i<TL_taskIdxs.length;++i) {
			TL_taskIdxs[i] = tl_idxs[i];
			//System.out.println("TL_taskIdxs : "+ TL_taskIdxs[i][0] +" | " + TL_taskIdxs[i][1]);			
		}
		
		//idxs holds list of list of lists of idxs in final list of each node
		
		taskLocs = tmpListLocs.toArray(new myPointf[0]);
		taskOptSize = new int[taskLocs.length];
		optTeamTTCMins = new float[taskLocs.length];
		stdDevTTCMult = new float[taskLocs.length];
		taskOptSize[0] = 3;
		optTeamTTCMins[0] = 3;
		stdDevTTCMult[0] = 0.0f;		
		for(int i=1;i<taskLocs.length;++i) {
			taskOptSize[i] = (int)Math.round((ThreadLocalRandom.current().nextDouble(0,1) * 5)+1);
			optTeamTTCMins[i] = (float)(ThreadLocalRandom.current().nextDouble(0,1) * 15) + 5.0f;
			stdDevTTCMult[i] = 0.0f;		
		}
	}//initSimPriv
	
	//determine which tasks are group tasks TODO change this to be some % of groups in map
	@Override
	protected boolean[] getIsGroupAra() {
		boolean [] isGrp = new boolean[taskLocs.length];
		for(int i=1;i<taskLocs.length;++i) {
			isGrp[i]= (ThreadLocalRandom.current().nextDouble(0,1) > .8);
			
		}
		//last is group
		isGrp[0] = false;
		isGrp[isGrp.length-2] = true;
		isGrp[isGrp.length-1] = true;
		return isGrp;
	}

}//complexDesSim
