import java.awt.List;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.swing.plaf.synth.SynthSeparatorUI;
import javax.swing.plaf.synth.SynthToggleButtonUI;

class Point{
	int x;
	int y;
	Point(int a, int b){
		x = a;
		y = b;
	}
} 

class Board {
	//boolean copyArray[];
	int sortIndex[];
	int remCount[];
	boolean neighbor[][];
	boolean cl[][];														// for maintaining the constraint easily
	int bvec[];
	boolean clueFlag[];

	Hashtable<Integer, Vector<Integer>> ht;								// for constraint assoc with each square
	Hashtable<Integer, Vector<Integer>> htTemp;
	ArrayList<HashSet<Integer>> relCell;								// for related cells to check for a single square

	Board(String init)
	{
		clueFlag = new boolean[81];
		sortIndex = new int[81];
		for (int i = 0; i < 81; i++) {
			sortIndex[i]=i;
		}
		remCount = new int[81];
		//copyArray = new boolean[81];
		neighbor = new boolean[81][81];
		//Arrays.fill(neighbor, false);
		cl = new boolean[81][9];
		bvec = new int[81];
		ht = new Hashtable<Integer, Vector<Integer>>();
		htTemp = new Hashtable<Integer, Vector<Integer>>();
		relCell = new ArrayList<HashSet<Integer>>();

		for (int i = 0; i < init.length(); i++) {
			if(init.charAt(i)=='.'||init.charAt(i)=='0')
			{
				bvec[i] = 0;
				clueFlag[i] = false;
			}
			else
			{
				bvec[i] = Character.getNumericValue(init.charAt(i));
				clueFlag[i]=true;
			}
		}
		ht.clear();
		relCell.clear();

	}
	Point pointFromId(int id){
		return new Point((id)/9, (id)%9);
	}

	int idFromPoint(Point p){
		return p.x*9 + p.y;
	}
	void Move(int value, int id)
	{
		bvec[id]=value;
	}
	void Remove(int id)
	{
		bvec[id]=0;
	}
	int getValue(int id){
		return bvec[id];
	}
	/*
	 *
	 * This method creates the possible moves at each position
	 * and populate a hashtable whose key is id number of square and value is a 
	 * vector containing all the possible values that could be placed on that square
	 * 
	 * Also it populates an arraylist of hashset where each hash set contains the squares associated
	 * with each square in sudoku constraints
	 */
	void GenerateConstraints()
	{
		Integer a[]={1,2,3,4,5,6,7,8,9};
		boolean constraintFlag[]={true,true,true,true,true,true,true,true,true};

		// generating constraint list
		Vector<Integer> cvct = new Vector<>();
		HashSet<Integer> hs = new HashSet<>();

		for (int i = 0; i < 81; i++) {
			cvct.addAll(Arrays.asList(a));
			Arrays.fill(constraintFlag, true);
			if(bvec[i]==0)
			{
				// removing row & column elements
				int rowStart = 9*(i/9);
				int colStart = i%9;
				for (int j = rowStart, k = colStart ; j < rowStart+9; j++,k+=9) {
					if(bvec[j]!=0)											// if the square already filled, then update constraints
					{
						cvct.removeElement(bvec[j]);		
						constraintFlag[bvec[j]-1]=false;

					}
					else 						// if the square not filled, take and edge in constraint graph
					{
						if(i!=j)												// only keeping
						{
							hs.add(j);
							neighbor[i][j]=true;
						}
					}

					if(bvec[k]!=0)
					{
						cvct.removeElement(bvec[k]);
						constraintFlag[bvec[k]-1]=false;

					}
					else
					{
						if(i!=k)
						{
							hs.add(k);
							neighbor[i][k]=true;
						}

					}




				}

				// removing mini-square unit elements
				Point p = pointFromId(i);
				int startId = idFromPoint(new Point((p.x/3)*3,(p.y/3)*3));
				int count = 0;
				for (int j = startId; count<3 ; j+=9,count++) {
					for (int j2 = 0; j2 < 3; j2++) {
						int index = j+j2;
						if(bvec[index]!=0)
						{
							cvct.removeElement(bvec[index]);
							constraintFlag[bvec[index]-1]=false;

						}
						else
						{
							if(i!=index)
							{
								hs.add(index);
								neighbor[i][index]=true;
							}
						}

					}
				}	
				remCount[i] = 0;
				for (int j = 0; j < 9; j++) {
					if(constraintFlag[j]==true) remCount[i]++;
				}
				//System.out.println(cvct.size()+" ### "+rem);
				cl[i] = constraintFlag.clone();
				ht.put(i,(Vector<Integer>)(cvct.clone()));
				relCell.add((HashSet<Integer>)hs.clone());
				hs.clear();
				cvct.clear();

			}
			else
			{
				remCount[i]=-1;
				cvct.clear();
				ht.put(i,(Vector<Integer>)(cvct.clone()));
				relCell.add((HashSet<Integer>)hs.clone());
			}

		}


		// sorting the indexes based on MRV, bubble sort as 81 elements
		for (int j = 0; j < 81; j++) {
			for (int j2 = 80; j2 > 0; j2--) {

				if(remCount[sortIndex[j2-1]]>remCount[sortIndex[j2]])
				{	
					int temp = sortIndex[j2-1];
					sortIndex[j2-1] = sortIndex[j2];
					sortIndex[j2] = temp;
				}


			}
		}

		/*for (int j = 0; j < 81; j++) {
			if(remCount[j]>0)
			{
				System.out.println((sortIndex[j]) + " **** "+remCount[sortIndex[j]]);
			}
			System.out.println(j+" "+ht.get(j));
		}*/

	}
	/*
	 * This method implements simple constraints search to find solution 
	 * Does not work if there is no square of a single value constraint at initial setting
	 */
	void ConstraintSearch()
	{

		GenerateConstraints();
		int iteration = 0;
		while(true)
		{
			boolean bf = false;

			for (int j = 0; j < 81; j++) {

				if(ht.get(j).size()==1)
				{
					System.out.println("single valued id : "+j);
					bf = true;
				}
			}
			if(bf == false) break;
			System.out.println("*******Grid state after " + ++iteration +" iteration*****");

			for (int j = 0; j < 81; j++) {

				if(ht.get(j).size()==1)
				{

					int val = ht.get(j).get(0);
					ht.get(j).remove(0);
					Move(val, j);

					Iterator itr = relCell.get(j).iterator();

					while (itr.hasNext()) {
						int p = (Integer)itr.next();
						if(ht.get(p).contains(val)){
							ht.get(p).removeElement(val);
						}
					}

				}
			}

		}
		int count = 0;
		for (int j = 0; j < 81; j++) {
			System.out.print(bvec[j]+" ");
			count++;
			if(count>=9)
			{
				System.out.println("");
				count=0;
			}
		}
		System.out.println("");
	}

	void printSolution()
	{
		// print the grid 
		int count = 0;
		for (int i = 0; i < 81; i++) {
			System.out.print(getValue(i)+" ");
			count++;
			if(count==9) {
				System.out.println("");
				count=0;
			}
		}
	}
	boolean checkValidity(int value, int id)
	{
		boolean valid = true;

		/*Iterator itr = relCell.get(id).iterator();

		while(itr.hasNext()){
			int p = (Integer)itr.next();
			if(id!=p && value == getValue(p))
			{
				valid = false;
				break;
			}
		}*/

		for (int i = 0; i < 81; i++) {
			if(value == getValue(i) && neighbor[id][i]==true)
			{
				valid = false;
				break;
			}
		}


		return valid;
	}
	boolean findSolutionMRV(int id)
	{
		//System.out.println("id "+id);
		if(id==81){			
			return true;
		}
		else if(getValue(sortIndex[id])==0){
			boolean retFlag = false;
			for (int i = 0; i < 9; i++) {
				if(cl[sortIndex[id]][i]==true)
				{
					int val = i+1;
					if(checkValidity(val, sortIndex[id])){
						//if(sortIndex[id]==13)
						//System.out.println("putting "+val+" at "+sortIndex[id]);
						Move(val,sortIndex[id]);
						retFlag = findSolutionMRV(id+1);
						if(retFlag==true) break;
						//System.out.println("backtracking at "+sortIndex[id]+" value "+getValue(sortIndex[id]));
						Remove(sortIndex[id]);
					}

				}
			}
			return retFlag;
		}
		else if(id<80) {
			return findSolutionMRV(id+1);
		}
		else return false;


	}
	/*
	 * Recursive method to find out the solution
	 * Return type is boolean because it helps breaking recursion after having as solution
	 */
	boolean findSolution(int id)
	{
		if(id==81){			
			return true;

		}
		else if(getValue(id)==0){
			boolean retFlag = false;
			for (int i = 0; i < 9; i++) {
				if(cl[id][i]==true)
				{
					int val = i+1;
					if(checkValidity(val, id)){
						Move(val,id);
						retFlag = findSolution(id+1);
						if(retFlag==true) break;
						Remove(id);
					}

				}
			}
			return retFlag;
		}
		else {
			return findSolution(id+1);
		}


	}
	boolean findSolutionFC(int id)
	{
		if(id==81){
			return true;
		}
		else if(getValue(id)==0){
			boolean retFlag = false;

			for (int i = 0; i < 9; i++) {

				if(cl[id][i]==true)
				{
					int val = i+1;
					Move(val,id);
					boolean copyArray[] = new boolean[81];
					Arrays.fill(copyArray, false);

					/*
					Iterator itr = relCell.get(id).iterator();
					while (itr.hasNext()) {
						int p = (Integer)itr.next();
						if(cl[p][val-1]==true){
							cl[p][val-1]=false;
							copyArray[p]=true;
						}
					}
					 */

					for (int j = 0; j < 81; j++) {
						if(neighbor[id][j]==true){
							if(cl[j][val-1]==true){
								cl[j][val-1] = false;
								copyArray[j]=true;
							}
						}
					}

					cl[id][val-1]=false;

					retFlag = findSolutionFC(id+1);
					if(retFlag==true) break;
					Remove(id);

					for (int j = 0; j < 81; j++) {
						if(copyArray[j]==true){
							cl[j][val-1]=true;
						}
					}

					cl[id][val-1]=true;
				}

			}

			return retFlag;
		}
		else return findSolutionFC(id+1);
	}
	void BacktrackSearch()
	{
		GenerateConstraints();
		findSolution(0);
	}
	void BacktrackSearchFC()
	{
		GenerateConstraints();

		/*for (int i = 0; i < 5; i++) {
			System.out.println(relCell.get(i));
		}

		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 81; j++) {

				if(neighbor[i][j]==true)
				{
					System.out.print(" "+j);
				}
			}
			System.out.println("");
		}*/

		findSolutionFC(0);
	}
	/*
	 * This function implements the AC3 algorithm on the constraints
	 */
	void MakeArchConsistence()
	{
		Queue<Point> q = new LinkedList<Point>();
		for (int i = 0; i < 81; i++) {
			Iterator it = relCell.get(i).iterator();	
			while(it.hasNext()){
				int val = (Integer)it.next();
				if(getValue(i)==0 && getValue(val)==0)
					q.add(new Point(i, val));
			}
		}


		int count = 0;
		while(true)
		{
			Point p = q.poll();
			if(p==null){

				break;
			}
			boolean remFlag;

			for (int i = 0; i < 9; i++) {
				remFlag = true;
				for (int j = 0; j < 9; j++) {
					if(cl[p.x][i]==true && cl[p.y][j]==true && i!=j)
					{
						remFlag = false;
						break;
					}
				}
				if(remFlag==true){
					cl[p.x][i] = false;
					//System.out.println("removing "+(i+1)+" from "+p.x);
					Iterator it = relCell.get(p.x).iterator();	
					while(it.hasNext()){
						int val = (Integer)it.next();
						if(getValue(p.x)!=0 && getValue(val)!=0)
							q.add(new Point(p.x, val));
					}
				}
			}

		}

	}
	void BacktrackSearchMRV()
	{
		GenerateConstraints();
		findSolutionMRV(0);
		/*if(findSolutionMRV(0)==true) System.out.println("solution"); else System.out.println("no solution");
		if(TestSolution()==true) System.out.println("VALID"); else System.out.println("INVALID");*/
	}
	void BacktrackSearchAC3()
	{
		GenerateConstraints();
		MakeArchConsistence();
		findSolution(0);
	}
	boolean CheckBoxValidity(int idx, int val)
	{
		int count = 0;
		Point p = pointFromId(idx);
		int startId = idFromPoint(new Point((p.x/3)*3,(p.y/3)*3));
		for (int j = startId; count<3 ; j+=9,count++) {
			for (int j2 = 0; j2 < 3; j2++) {
				int index = j+j2;
				if(val==getValue(index)){
					return false;
				}
			}
		}
		return true;
	}
	int Cost()
	{	int sum = 0;

	boolean touch[] = new boolean[9];
	boolean touch2[] = new boolean[9];

	for (int i = 0; i < 9; i++) {
		Arrays.fill(touch, false);
		Arrays.fill(touch2, false);
		for (int j = 0,k=0; j < 9; j++,k++) {
			int val = getValue(idFromPoint(new Point(i, j)));
			val--;
			//System.out.println(val);
			if(touch[val]==false)
			{
				touch[val]=true;
			}
			else sum++;

			val = getValue(idFromPoint(new Point(k, i)));
			val--;
			if(touch2[val]==false)
			{
				touch2[val]=true;
			}
			else sum++;
		}
	}
	return sum;
	}
	boolean AmongSameBox(int id1, int id2)
	{
		Point p = pointFromId(id1);
		int startId1 = idFromPoint(new Point((p.x/3)*3,(p.y/3)*3));
		p = pointFromId(id2);
		int startId2 = idFromPoint(new Point((p.x/3)*3,(p.y/3)*3));

		if(startId1==startId2) return true;
		return false;
	}
	void Swap(int id1, int id2)
	{
		int temp = getValue(id1);
		Move(getValue(id2),id1);
		Move(temp,id2);
	}
	void SimulatedAnnealing()
	{
		int cellStart[] = {0,3,6,27,30,33,54,57,60};


		Random r = new Random();
		for (int i = 0; i < 81; i++) {
			if(getValue(i)==0){

				while(true){
					int rand = r.nextInt(10);
					if(CheckBoxValidity(i, rand))
					{
						Move(rand, i);
						break;
					}
				}
			}
		}

		int count = 0;
		int counter = 1000000;
		double temp = 500;
		double cutOffProb = 0.05;
		Random r2 = new Random();
		while(++count<counter)
		{
			int rand = r.nextInt(9);

			//System.out.println("*****choosing block "+rand+"*****");
			int cell1 = r2.nextInt(9) ;
			if(cell1<=2)
			{
				cell1 += cellStart[rand];
			}
			else if(cell1<=5)
			{
				cell1 = (9+(cell1%3)+cellStart[rand]);
			}
			else 
			{
				cell1 = (18+(cell1%3)+cellStart[rand]);
			}
			int cell2 = r2.nextInt(9) ;


			if(cell2<=2)
			{
				cell2 += cellStart[rand];
			}
			else if(cell2<=5)
			{
				cell2 = (9+(cell2%3)+cellStart[rand]);
			}
			else 
			{
				cell2 = (18+(cell2%3)+cellStart[rand]);
			}
			if(clueFlag[cell1]==false && clueFlag[cell2]==false)
			{
				int cost1 = Cost();
				Swap(cell1, cell2);
				int cost2 = Cost();
				if(cost2==0) {
					System.out.println("solution");
					break;
				}
				//if(++count%50000==0) System.out.println(cost1 +" *** "+cost2);
				if(cost2>=cost1) 
				{

					double diff = (cost1 - cost2);
					//System.out.println("diff "+diff+"temp "+ temp);
					//System.out.println(diff/temp);
					double prob = Math.exp(diff/temp);

					//System.out.println(prob);
					if(prob<cutOffProb)
					{
						Swap(cell1, cell2);
						//System.out.println("discarded "+ Cost());
					}


				}
			}

			temp = temp*0.99999;
			//if(++count > 5000) break;

			//if(++count%50000==0) System.out.println(Cost()+" "+rand+" "+cell1+" "+cell2+" "+temp);
			/*if(temp==0.0) 
			{
				System.out.println("no solution, try again");
				break;
			}*/

		}

		if(count==counter)
		{
			System.out.println("No solution this time, try again. This a random algorithm");
		}

	}

	/*
	 * This method tests a solution if it is a valid solution or not
	 * Traverse each of 20 neighboring squares of each square and tests if all numbers are unique or not
	 */
	boolean TestSolution()
	{
		// testing
		boolean failFlag = false;

		for (int i = 0; i < 81; i++) {
			int rowStart = 9*(i/9);
			int colStart = i%9;
			for (int j = rowStart, k = colStart ; j < rowStart+9; j++,k+=9) {
				if(getValue(i)==getValue(j) && i!=j){
					failFlag = true;
					break;
				}

				if(getValue(i)==getValue(k) && i!=k){
					failFlag = true;
					break;
				}

			}
			if(failFlag) break;
			Point p = pointFromId(i);
			int startId = idFromPoint(new Point((p.x/3)*3,(p.y/3)*3));
			int count = 0;
			for (int j = startId; count<3 ; j+=9,count++) {
				for (int j2 = 0; j2 < 3; j2++) {
					int index = j+j2;
					if(getValue(i)==getValue(index) && i!=index){
						failFlag = true;
						break;
					}

				}
				if(failFlag) break;
			}	

			if(failFlag) break;
		}

		if(failFlag) {
			return false;
		}
		else {
			return true;
		}

	}
}
class Game {
	
	static void printAlgoSelect()
	{
		System.out.println("**Select an algorithm**");
		System.out.println("1. Simple backtracking");
		System.out.println("2. Backtracking with Forward Chaining");
		System.out.println("3. Backtracking after AC3");
		System.out.println("4. Backtracking with MRV heuristics");
		System.out.println("5. Simulated Annealing");
		System.out.println("Enter an option:");
	}
	static void printMenu()
	{	System.out.println("\n\n####MENU####");
		System.out.println("1. Run Easy Test");
		System.out.println("2. Run Medium Test");
		System.out.println("3. Run Hard Test");
		System.out.println("4. Exit");
		System.out.println("Enter an option:");
	}

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		Scanner s = null;

		String sinput[]={"","easy_tests.txt","medium_tests.txt","hard_tests.txt"};		

		while(true)
		{
			printMenu();
			int ip = input.nextInt();
			if(ip==4) break;
			else if(ip>4)
			{
				System.out.println("**Choice Not Allowed!**");
				continue;
			}
			printAlgoSelect();
			int alip = input.nextInt();
			
			try {
				s = new Scanner(new BufferedReader(new FileReader(sinput[ip])));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String str = "";
			Board b;
			int counter = 1;
			while(s.hasNextLine())
			{
				str = s.nextLine();
				b = new Board(str);
				long startTime = System.currentTimeMillis();
				if(alip==1){
					b.BacktrackSearch();
				}else if (alip==2){
					b.BacktrackSearchFC();
				}else if (alip==3){
					b.BacktrackSearchAC3();
				}else if (alip==4){
					b.BacktrackSearchMRV();
				}else if (alip==5){
					b.SimulatedAnnealing();
				}
				else 
				{
					System.out.println("**Choice Not Allowed!**");
					printAlgoSelect();
					alip = input.nextInt();
				}
				

				long elaspedTime = System.currentTimeMillis()-startTime;

				System.out.println("############# Summary test case "+ counter +"##############");
				System.out.println("Puzzle:");
				System.out.println(str);
				++counter;
				System.out.println("Elasped Time in millis: "+elaspedTime);
				System.out.println("******SOLUTION******");
				b.printSolution();
				System.out.println("Checking Solution...");
				if(b.TestSolution()==true) System.out.println("VALID SOLUTION");
				else System.out.println("INVALID SOLUTION");
			}
		}
		


	}



}
