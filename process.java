import java.net.*;
import java.nio.channels.*;
import java.util.Random;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.FutureTask;
public class process{
static	public int dequeue(int [] queue)
	{
		int rettemp = queue[0];
		for(int ii=0;ii<queue.length-1;ii++)
		{
			queue[ii]=queue[ii+1];
		}
		return rettemp;
		
	}
	public static void main(String[] args) throws Exception
	{	
		int procnum=0,temp=0;
		if(args.length >= 1 && args[0].equals("-c"))
			{
				String pid=null;
				int interval=0;
				int terminate=0;
				int t1=0,t2=0,t3=0;
				int[][] nb = new int[100][100];
				
				//Reading Input from dsConfig File

				String str = System.getProperty("user.dir");
		 		str = str + "//dsConfig";
				File file = new File(str);
		 		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		 		String line = null;
		 		int i=0;
		 		while( (line = br.readLine())!= null )
		 		{
		               		String [] tokens = line.split("\\s+");
		 			if(i==0)
		 			{
		 				pid=tokens[1];
		 			}	
		 			else if(i==1)
		 			{
		 				procnum=Integer.parseInt(tokens[3]);
		 			}
		 			else if(i==2)
		 			{
		 				t1=Integer.parseInt(tokens[1]);	
		 				t2=Integer.parseInt(tokens[2]);	
		 			}
		 			else if(i==3)
		 			{
		 				t3=Integer.parseInt(tokens[1]);
		 			}	
		 			else if(i==4)
		 			{}
		 			else if((i>=5)&&(i<(5+procnum)))	
		 			{
		 				int tempj=0;
		 				while(tempj<tokens.length)
		 				{
		 					nb[i-5][tempj] = Integer.parseInt(tokens[tempj]);
		 					tempj++;
		 				}
		 			}
		 			i++;
		 			
		 		}

				//Output the Neighbour Table

		 		for(int tempi=0;tempi<procnum;tempi++)
	 			{
	 				for(int tempj=0;tempj<procnum;tempj++)
		 			{
	 					System.out.print(nb[tempi][tempj]);
		 			}
	 				System.out.println("");
	 			}
		
		
		//Coordinator is listening to processes

		ServerSocket ss = new ServerSocket(5056);
		int threads=0;
		Processtable pt = new Processtable();
		pt.putinfo(1, InetAddress.getByName(pid), 5056);
		while (threads<procnum-1) 
		{
			Socket s = null;
			
			try 
			{
				s = ss.accept();
				System.out.println("Register Message Received : " + s);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				
				//Coordinator assigns a Process ID to the register message

				Thread t = new Thread(new ClientHandler(s,dis,dos,pt,threads+2));
				t.start();
				threads++;
			}
			catch (Exception e){
				s.close();
				e.printStackTrace();
			}
		}
			TimeUnit.SECONDS.sleep(1);
			System.out.println("\n");
			NBInfo[] nblist = new NBInfo[procnum+1];
			
			//Creation of Neighbour Table
			
			for(int ii=1;ii<=procnum;ii++)
			{
				System.out.println(ii + " :  " + pt.getport(ii) + " : " + pt.gethostname(ii));
			}
			
			for(int ii=0;ii<procnum;ii++)
			{
				nblist[nb[ii][0]]=new NBInfo();
				for(int jj=1;jj<=procnum;jj++)
				{
					if(nb[ii][jj]!=0)
					{
						nblist[nb[ii][0]].putinfo(jj, nb[ii][jj], pt.gethostname(nb[ii][jj]), pt.getport(nb[ii][jj]));
					}
				}
			}
	
			//Output of the Neighbour Table
			for(int ii=1;ii<=procnum;ii++)
			{
				System.out.println(ii + " :-");
				for(int jo=1;jo<=nblist[ii].num;jo++)
				{
					System.out.println(nblist[ii].npid[jo] + " " + nblist[ii].neighbours[jo] + " " + nblist[ii].ports[jo]);
				}
			}
			
			//Send the Neighbour Table to all Processes
			for(int ii=2;ii<=procnum;ii++)
			{
				Socket serv = new Socket(pt.gethostname(ii),pt.getport(ii));
				ObjectOutputStream oos = new ObjectOutputStream(serv.getOutputStream());
				oos.flush();
				Thread t = new Thread(new ServerFirst(serv,ii,nblist,oos));
				t.start();					
			}
			int first=0;
			

			//Awaiting Ready Message
			for(int ii=1;ii<=nblist[1].num;ii++)
			{
				Socket sss = new Socket(nblist[1].getinetadd(ii),nblist[1].getport(ii));
				DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
				Thread th = new Thread(new ProcessComm(sss,dss,1,first,0,0));
			    th.start();
			}
			RetInt r = new RetInt();
			first++;
			int nbcount =0,retval=1;
			
			//Sending and Receiving First Compute Message
			while(true)
			{
				Socket servp = ss.accept();
				if(nbcount<nblist[1].num)
				{
				System.out.println("New Connection accepted : " + servp);	
				ProcessListener tt2 = new ProcessListener(servp,1);
				r=tt2.call();
				nbcount++;
				}
				else
				{
					Thread tt3 = new Thread(new ServerReady(servp,retval));
					tt3.start();
					retval++;
					if(retval==procnum)
					{
						System.out.println("\nReady Received From all");
						break;
					}
				}
			 }
			
						
			//3 Phase Commit Begins
			int choice =0;
			String request= null;
			int commitnum=1;
			int acknum=1;
			int flag=0;
			String yn =null;
			int value;
			int transnum=0;
			String filename = "localoutput_0";
			
			Writer wri = new FileWriter(filename,true);
			PrintWriter pwri = new PrintWriter(wri,true);			
			
			
			flag=0;
			transnum++;
			Scanner scm = new Scanner(System.in);
			Scanner scm2 = new Scanner(System.in);
			System.out.println("\n\nTHREE PHASE COMMIT :" +transnum + "\n");
			System.out.println("ENTER VALUE TO SEND TO THE COHORTS");
			value = scm2.nextInt();
			System.out.println("\nSELECT CONDITION:\n1. NORMAL THREE PHASE COMMIT WITHOUT FAILURE\n2. FAILURE DURING INITIALIZATION\n3. FAILURE DURING WAIT PHASE\n4. FAILURE DURING PREPARE PHASE\n");
			System.out.println("PRESS ANY KEY TO START");
			pwri.println("\n\nTRANSACTION ID : " + transnum);
			pwri.println("VALUE SENT: " + value +"\n");
			yn = scm.nextLine();			
			System.out.println("\nENTER CHOICE :?");
			Future<Integer> futureTask = Executors.newSingleThreadExecutor().submit(new InitThread());
			
			//Timeout Check Initial Condition
			try 
			{
	            choice = futureTask.get(5, TimeUnit.SECONDS);
	        }
			catch (TimeoutException ex) 
			{
	        	System.out.println("TIMEOUT DURING INITIALIZATION\n\n!!ABORT!!\n\n");
	            futureTask.cancel(true);
	            pwri.println("TIMEOUT DURING INITIALIZATION\n\n!!ABORT!!\n\n");	           
				System.exit(0);
	        }	
			
			if(choice==2)
			{
				System.out.println("COORDINATOR FAILED \n\n!!ABORT!!\n\n");
				pwri.println("COORDINATOR FAILED \n\n!!ABORT!!\n\n");
				System.exit(0);
			}
			
			
			try
				{
					if(flag==0)
					{
						Thread smi = new Thread(new SendMessage(nblist[1],"CommitRequest " + transnum +  " " + value));
						smi.start();
						System.out.println("COMMIT REQUEST SENT\n\nWAITING..\n");						
						flag=1;
					}
					
					
			//Begin Wait State
					
					//Failure Condition Wait State
					if(choice==3)
					{
						TimeUnit.SECONDS.sleep(2);
						System.out.println("COORDINATOR FAIL IN WAITING PHASE\n\n!!ABORT!!\n\n");
						pwri.println("COORDINATOR FAIL IN WAITING PHASE\n!!ABORT!!\n");
						System.exit(0);
					}
					
					//Timeout Check Wait State
					Future<String> waitth = Executors.newSingleThreadExecutor().submit(new WaitThread(ss,transnum));
					try
					{
			           request = waitth.get(15, TimeUnit.SECONDS);
			        }
					catch (TimeoutException ex) 
					{
			        	System.out.println("\nTIMEOUT DURING WAITING PHASE\n\n!!ABORT!!\n");
			        	pwri.println("\nTIMEOUT DURING WAITING PHASE\n\n!!ABORT!!\n");
			        	waitth.cancel(true);
			            Thread smi = new Thread(new SendMessage(nblist[1],"Abort " + transnum));
			            smi.start();
						System.out.println("ABORT SENT..\n");						
						TimeUnit.SECONDS.sleep(2);
						System.exit(0);
			        }
					
					
					//If All Processes Commit
					if(request.equals("Commit"))
					{		
							System.out.println("All Commit Received\n\n");							
							TimeUnit.SECONDS.sleep(2);
							Thread smi = new Thread(new SendMessage(nblist[1],"Prepare " + transnum));
				            smi.start();
							System.out.println("PREPARE SENT..\n\n");
							
					}
					else
					{
						System.out.println("ABORT RECEIVED\n\n!!ABORT!!\n\n");
						pwri.println("ABORT RECEIVED\n\n!!ABORT!!");
						Thread smi = new Thread(new SendMessage(nblist[1],"Abort "+ transnum));
			            smi.start();
						System.out.println("ABORT SENT..\n");						
						System.exit(0);
					}
					
					
			//Begin Prepare State
					
					//Failure Condition Prepare State
					if(choice==4)
					{
						TimeUnit.SECONDS.sleep(2);
						System.out.println("COORDINATOR FAIL IN PREPARE PHASE\n\n!!COMMIT!!\n\n");
						pwri.println("COORDINATOR FAIL IN PREPARE PHASE\n\n!!COMMIT!!");
						pwri.println("COMMITED VALUE : " + value);
						System.exit(0);
					}
					
					//Timeout Checking Prepare State
					Future<String> prepareth = Executors.newSingleThreadExecutor().submit(new PrepareThread(ss));
					try
					{
			           request = prepareth.get(5, TimeUnit.SECONDS);
			        }
					catch (TimeoutException ex) {
			            // 5 seconds expired, we cancel the job !!!
			        	System.out.println("TIMEOUT DURING PREPARE PHASE\n\n!!ABORT!!\n\n");
			        	pwri.println("TIMEOUT DURING PREPARE PHASE\n\n!!ABORT!!");
			            prepareth.cancel(true);
			            Thread smi = new Thread(new SendMessage(nblist[1],"Abort " + transnum));
			            smi.start();
						System.out.println("ABORT SENT..\n");						
						System.exit(0);
			        }
					
					//If All Processes Acknowledge
					if(request.equals("Acknowledge"))
					{		
							System.out.println("ACK RECEIVED\n\n");							
							TimeUnit.SECONDS.sleep(2);
							Thread smi = new Thread(new SendMessage(nblist[1],"Commit " + transnum));
				            smi.start();
				            System.out.println("FINAL COMMIT SENT..\n\n");				           
				            TimeUnit.SECONDS.sleep(2);
				            System.out.println("!!COMMIT!!\n\n");
				            pwri.println("!!COMMIT!!");
				            pwri.println("COMMITED VALUE : " + value);
				            System.exit(0);
					}
					
				}
				catch(Exception e)
				{
					System.out.println(e);
				}
			
			
		}
	
		
		//Client Half of the Program
				
		else
		{
			try
			{
				String pid=null;
				String str = System.getProperty("user.dir");
		 		str = str + "//dsConfig";
				File file = new File(str);
		 		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		 		String line = null;
		 		int i=0; int t1=0,t2=0,t3=0;
		 		while( (line = br.readLine())!= null )
		 		{
		               		String [] tokens = line.split("\\s+");
		 			if(i==0)
		 			{
		 				pid=tokens[1];
		 			}	
		 			else if(i==1)
		 			{
		 				procnum=Integer.parseInt(tokens[3]);
		 			}
		 			else if(i==2)
		 			{
		 				t1=Integer.parseInt(tokens[1]);	
		 				t2=Integer.parseInt(tokens[2]);	
		 			}
		 			else if(i==3)
		 			{
		 				t3=Integer.parseInt(tokens[1]);
		 			}	
		 			i++;
		 			
		 		}
		 				 		
				//Register and Get ID from Server

				InetAddress ip = InetAddress.getByName(pid);
				Socket s = new Socket(ip, 5056);
				s.setReuseAddress(true);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				int myid = dis.readInt();
				System.out.println("The Process ID assigned is : " + myid);
				System.out.println(s.getLocalPort());
				s.close();
				NBInfo[] nbm = null;
				ServerSocket ss = new ServerSocket(s.getLocalPort());
				int tempi=0;
				int a=0;
				RetInt r = new RetInt();
				while(true)
				{
				Socket servs = null,servp=null;
				try 
				{
					
					if(tempi==0)
					{
						servs = ss.accept();
						ProcessHandler tt = new ProcessHandler(servs,myid);
						nbm = tt.call();
						for(int ii=1;ii<=nbm[myid].num;ii++)
						{
							Socket sss = new Socket(nbm[myid].getinetadd(ii),nbm[myid].getport(ii));
							DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
							Thread th = new Thread(new ProcessComm(sss,dss,myid,tempi,0,0));
							th.start();
						}
						tempi=1;
					}
				
					else
					{
						servp = ss.accept();
						System.out.println("New Connection accepted : " + servp);	
						ProcessListener tt2 = new ProcessListener(servp,myid);
						r=tt2.call();
						a++;
						if(a==nbm[myid].num)
						{
							Socket servtemp = new Socket(ip, 5056);
							DataOutputStream doss = new DataOutputStream(servtemp.getOutputStream());
							doss.writeUTF("Ready");
							break;
						}
					}
					
				 }
				
				 catch (Exception e)
				 {
					s.close();
					e.printStackTrace();
				 }						
			}
				
				
		//3 Phase Commit Client Begins
				
			String Option = null;
			int wanttocommit=1;
			int wanttoprepare=1;
			int prepare =0;
			int choice=0;
			String yn=null;
			int value=0;
			int transnum=0;
			String[] inputbreak1 =null;
			String input;
			String filename = "localoutput_" + myid;
			Writer wri = new FileWriter(filename,true);
			PrintWriter pwri = new PrintWriter(wri,true);
			
			
			prepare=0;
			transnum++;
			Scanner scm = new Scanner(System.in);			
			System.out.println("\n\nTHREE PHASE COMMIT :" +transnum + "\n");			
			System.out.println("\nSELECT CONDITION:\n1. NORMAL THREE PHASE COMMIT WITHOUT FAILURE\n2. FAILURE DURING INITIALIZATION\n3. FAILURE DURING WAIT PHASE\n4. FAILURE DURING PREPARE PHASE\n");
			System.out.println("PRESS ANY KEY TO START");
			yn = scm.nextLine();	
			pwri.println("\n\nTRANSACTION ID : " + transnum);
			System.out.println("\nENTER CHOICE ?\n");
			Future<Integer> futureTask = Executors.newSingleThreadExecutor().submit(new InitThread());
			
			//Timeout Check Initial Condition
			try 
			{
	            choice = futureTask.get(10, TimeUnit.SECONDS);
	        }
			catch (TimeoutException ex) 
			{
				System.out.println("TIMEOUT DURING INITIALIZATION\n\n!!ABORT!!\n\n");	           
	            pwri.println("TIMEOUT DURING INITIALIZATION\n\n!!ABORT!!\n\n");		        	
	            futureTask.cancel(true);
	            System.exit(0);
	        }	
			
			//Failure Condition Check
			if(choice==2)
			{
				System.out.println("COHORT FAILED BEFORE COMMIT\n\n!!ABORT!!\n\n");
				pwri.println("COHORT FAILED BEFORE COMMIT\n\n!!ABORT!!");
				System.exit(0);
			}
			
			inner: while(true)
			{
				try
				{
				
					//Timeout Check
					Future<String> clientth = Executors.newSingleThreadExecutor().submit(new ClientReceive(ss,transnum,nbm[myid]));
					try
					{
						input = clientth.get(15, TimeUnit.SECONDS);
						inputbreak1 = input.split(" ");
						Option = inputbreak1[0];
			        }
					catch (TimeoutException ex) 
					{
			        	System.out.println("TIMEOUT\n");
			            clientth.cancel(true);
			            if(prepare==0)
					    {
					       	System.out.println("\n!!ABORT!!\n\n");
					        pwri.println("!!ABORT!!");
					    }
					    else
					    {
					        	System.out.println("\n!!COMMIT!!");
					        	System.out.println("COMMITED VALUE: "+value);
					        	pwri.println("!!COMMIT!!");
					        	pwri.println("COMMITED VALUE: " + value);
					    }
			            System.exit(0);
			            	
			            	
			        }
					
					//If signal received before timeout
					System.out.println(Option + " FROM COORDINATOR");					
					TimeUnit.SECONDS.sleep(2);
					
					if(Option.equals("Abort"))	
					{
						System.out.println("!!ABORT\n\n");
						pwri.println("!!ABORT!!");
						System.exit(0);
					}
					
					if(Option.equals("CommitRequest"))
					{
						try
						{
							value = Integer.parseInt(inputbreak1[2]);
							System.out.println("RECEIVER VALUE: " + value);
							pwri.println("RECEIVED VALUE: " + value);
							Socket sss = new Socket(nbm[myid].getinetadd(1),nbm[myid].getport(1));
							DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
							dss.writeUTF("Commit " + myid + " " + transnum);
							dss.close();
							System.out.println("AGREE SENT..\n\nIN WAIT PHASE..\n\n");							
						}
						catch(ConnectException e)
						{
							System.out.println("RECONNETING..\n\n");							
							continue inner;
						}
						
						if(choice==3)
						{
							System.out.println("COHORT FAILED IN WAITING PHASE\n\n!!ABORT!!\n\n");
							pwri.println("COHORT FAILED IN WAITING PHASE\n\n!!ABORT!!");
							System.exit(0);
						}
						continue inner;
					}
					
					if(Option.equals("Prepare"))
					{
						try
						{
							Socket sss = new Socket(nbm[myid].getinetadd(1),nbm[myid].getport(1));
							DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
							dss.writeUTF("Acknowledge "+myid + " " + transnum);
							dss.close();
							System.out.println("ACKNOWLEDGEMENT SENT..\n\nIN PREPARE PHASE..\n\n");							
						}
						catch(ConnectException e)
						{
							System.out.println("RECONNECTING...");
							
							continue inner;
						}
						prepare = 1;
						if(choice==4)
						{
							System.out.println("COHORT FAILED IN PREPARE PHASE\n\n!!COMMIT!!\n\n");
							pwri.println("COHORT FAILED IN PREPARE PHASE\n\n!!COMMIT!!");
							pwri.println("COMMITED VALUE: " + value);
							System.exit(0);
						}
						continue inner;
					}
					if(Option.equals("Commit"))	
					{
						System.out.println("FINAL COMMIT..\n\n!!COMMIT!!");
						System.out.println("COMMITED VALUE: "+value+"\n\n");
						pwri.println("!!COMMIT!!");
						pwri.println("COMMITED VALUE: " + value);
						System.exit(0);
					}
					
				}
				catch(Exception e)
				{
					System.out.println(e);
				}
			}
			
				
				
				
				
				
				
				
				
				
				
				
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
			
	}
}
}


class ClientHandler implements Runnable
{
	final DataInputStream dis;
	final DataOutputStream dos;
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	
	public ClientHandler(Socket s,DataInputStream dis, DataOutputStream dos,Processtable ppt,int pid) 
	{
		this.s = s;
		this.pt = ppt;
		this.pid=pid;
		this.dis = dis;
		this.dos=dos;
	}
	
	public void  run() 
	{
		
		try {
				dos.writeInt(pid);
				//int port = dis.readInt();
				InetAddress addr = s.getInetAddress();
				int port = s.getPort();
				System.out.print(" Addr : " + addr + " port : " + port + "\n\n");
				pt.putinfo(pid, addr, port);						
			} catch (IOException e) {
				e.printStackTrace();
			}
				
		try
		{
			// closing resources
			dis.close();
			dos.close();
			
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
}

class NBInfo implements Serializable {
	int[] npid = new int[30];
	int num;
	InetAddress[] neighbours = new InetAddress[30];
	int[] ports= new int[30];
	public void putinfo(int num, int ppid, InetAddress nb, int pport)
	{
		this.num=num;
		this.npid[num]=ppid;
		this.neighbours[num]=nb;
		this.ports[num]=pport;
		
	}
	public InetAddress getinetadd(int num)
	{		
		return neighbours[num];
	}
	public int getport(int num)
	{		
		return ports[num];
	}
}


class ProcessComm implements Runnable{
	Socket sss;
	DataOutputStream dss;
	int a=0,pid,first=0,lclock=0,fmc=0;  
	public ProcessComm(Socket sss, DataOutputStream dss, int myid,int num,int LCLOCK,int finalmarkercount)
	{
		this.dss = dss;
		this.sss = sss;
		this.pid = myid;
		this.first = num;
		this.lclock = LCLOCK;
		this.fmc = finalmarkercount;
	}
	
	public void  run() 
	{
		
		try {
				if(first==0)
				{
					dss.writeUTF("Hello from " + pid);
				//	dss.flush();
				
				}
				if(first==-1)
				{
					dss.writeUTF("Marker " + fmc);
				}
				if(first!=0)
				{
					dss.writeUTF("Compute from " + pid + " " + lclock);
				//	dss.flush();
					
				}
				
								
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}


}

class ProcessHandler implements Callable{
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	ObjectInputStream ois;
	NBInfo[] nb;
	public ProcessHandler(Socket s,int myid) 
	{
		this.s = s;
		this.pid = myid;
	}
	
	public NBInfo[] call() throws Exception
	{
		
		try {
				ObjectInputStream ooois = new ObjectInputStream(s.getInputStream());
				nb = (NBInfo[])ooois.readObject();
						ooois.close();		
			} catch (Exception e) {
				e.printStackTrace();
			}
		return nb;	
	}
}


class ProcessListener implements Callable {
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	RetInt r = new RetInt();
	public ProcessListener(Socket ss,int myid) 
	{
		this.s = ss;
		this.pid = myid;
	}
	
	public RetInt call() throws Exception
	{
		try {
				DataInputStream ds = new DataInputStream(s.getInputStream());
				String input = ds.readUTF();
				String[] input2 = input.split(" ",4);
				String[] input3 = input.split(" ", 2);
				//Incase the input message is Compute
				if(input2[0].equals("Compute"))
				{
					System.out.println("\n" + input);
					r.val=Integer.parseInt(input2[2]);
					r.clock = Integer.parseInt(input2[3]);
					return r;
				}
				
				//Incase the message is Marker
				else if(input3[0].equals("Marker"))
				{
					r.val = -1;
					r.clock = Integer.parseInt(input3[1]);
					return r;
				}
				
				//Incase the Input Message is Ready
				else
				{
					System.out.println("\n"+ input);
					
				}
				
				
				
						
			} catch (Exception e) {
				e.printStackTrace();
			}
		r.val=0;
		return r;
					
	}

}

class Processtable {
	int[] pid = new int[100];
	int[] port = new int[100];
	InetAddress hname[] = new InetAddress[100];
	
	public void putinfo(int ppid, InetAddress hhname, int pport)
	{
		
		hname[ppid] = hhname;
		port[ppid]=pport;
			
	}
	public int getport(int ppid)
	{
		return port[ppid];
	}
	public InetAddress gethostname(int ppid)
	{
		return hname[ppid];
	}
}

class RetInt {
	int val;
	int clock;
}

class ServerFirst implements Runnable{
	final Socket s;
	int a=0,pid;	
	NBInfo[] nblist = new NBInfo[10];
	ObjectOutputStream oos;
	public ServerFirst(Socket s, int myid, NBInfo[] nblist,ObjectOutputStream oos) 
	{
		this.s = s;
		this.pid = myid;
		this.oos = oos;
		this.nblist = nblist;
	}
	
	public void  run() 
	{
		
		try {
				oos.writeObject(nblist);
				oos.flush();
				oos.close();
				System.out.println("\nObject Sent to Process");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}
}

class ServerReady implements Runnable{
	final Socket s;
	int processnum;
	public ServerReady(Socket s,int processnum) 
	{
		this.s = s;
		this.processnum = processnum;
	}
	
	public void  run() 
	{
		
		try {
				System.out.println("Ready Received from Process : " + processnum);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}
}

class InitThread implements Callable<Integer>{
	Scanner sc;
	int val=0;
	public InitThread() 
	{
		sc = new Scanner(System.in);
	}
	
	public Integer call()
	{
		int choice = sc.nextInt();
		return choice;
	}

}

class PrepareThread implements Callable<String>{
	int commitnum=1;
	int procnum=5;
	String request;
	ServerSocket ss;
	public PrepareThread(ServerSocket ss) 
	{
		this.ss = ss;
	}
	
	@Override public String call() throws IOException
	{
		int breakval=0;
		for(commitnum=1;commitnum<procnum;commitnum++)
		{
			Socket servp = ss.accept();	
			DataInputStream os = new DataInputStream(servp.getInputStream());
			request = os.readUTF();
			String[] inputbreak = request.split(" ");
			if(!inputbreak[0].equals("Acknowledge"))
			{
				breakval=1;
				break;
			}
			System.out.println(request);
			
		}
		if(breakval==1)
		{
			request = "Abort";
			return request;
		}
		request ="Acknowledge";
		return request;
	}

}

class WaitThread implements Callable<String>{
	int commitnum=1;
	int procnum=5;
	String request;
	ServerSocket ss;
	int exit =1; int transnum;
	public WaitThread(ServerSocket ss, int transnum) 
	{
		this.transnum = transnum;
		this.ss = ss;
	}
	
	@Override public String call() throws IOException
	{
		int breakval=0;
		for(commitnum=1;commitnum<procnum;commitnum++)
		{
			Socket servp = ss.accept();	
			DataInputStream os = new DataInputStream(servp.getInputStream());
			request = os.readUTF();
			String[] inputbreak = request.split(" ");
			if(!inputbreak[0].equals("Commit"))
			{
				breakval=1;
				break;
			}
			System.out.println(request);
		}

		if(breakval==1)
		{
			request = "Abort";
			return request;
		}
		
		request ="Commit";
		return request;
		
	}

}
class ClientReceive implements Callable<String>{
	String request;
	ServerSocket ss;
	int thisval;
	String[] inputbreak1 = null;
	int Option =0;
	NBInfo nb; 
	public ClientReceive(ServerSocket ss, int thisval,NBInfo nb) 
	{
		this.ss = ss;
		this.thisval = thisval;
		this.nb = nb;
	}
	
	public String call() throws IOException
	{
			while(true)
			{
			Socket servp = ss.accept();	
			DataInputStream os = new DataInputStream(servp.getInputStream());
			request = os.readUTF();
			inputbreak1 = request.split(" ");
			Option = Integer.parseInt(inputbreak1[1]);
			break;
			}
		    return request;
	}

}
class SendMessage implements Runnable{
	NBInfo nb; 
	String m;
	public SendMessage(NBInfo nb, String Message)
	{
		this.nb = nb;
		m = Message;
	}
	
	public void  run() 
	{
		
		try {
				
			for(int ii=1;ii<=nb.num;ii++)
			{
				try
				{
					Socket sss = new Socket(nb.getinetadd(ii),nb.getport(ii));
					DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
					dss.writeUTF(m);
					dss.close();
				}
				catch(ConnectException e)
            	{
            		continue;
            	}
				
			}
								
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}


}
