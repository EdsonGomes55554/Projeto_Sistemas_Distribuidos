package pacote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Eleicao implements Watcher {

    static ZooKeeper zk = null;
    static Integer mutex;
    static Queue qVotos;
    static Queue qPergunta;
    static int numJogadores = 4;
    static Scanner scanner;


    String root;

    Eleicao(String address) {
        if(zk == null){
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            mutex.notify();
        }
    }

    static public class Leader extends Eleicao {
    	String leader;
    	String id; //Id of the leader
    	String pathName;
    	
   	 /**
         * Constructor of Leader
         *
         * @param address
         * @param name Name of the election node
         * @param leader Name of the leader node
         * 
         */
        Leader(String address, String name, String leader, int id) {
            super(address);
            this.root = name;
            this.leader = leader;
            this.id = new Integer(id).toString();
            // Create ZK node name
            if (zk != null) {
                try {
                	//Create election znode
                    Stat s1 = zk.exists(root, false);
                    if (s1 == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }  
                    //Checking for a leader
                    Stat s2 = zk.exists(leader, false);
                    if (s2 != null) {
                        byte[] idLeader = zk.getData(leader, false, s2);
                    }  
                    
                } catch (KeeperException e) {
                    System.out.println("Keeper exception when instantiating queue: " + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }
        
        boolean elect() throws KeeperException, InterruptedException{
        	this.pathName = zk.create(root + "/n-", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        	return check();
        }
        
        boolean check() throws KeeperException, InterruptedException{
        	Integer suffix = new Integer(pathName.substring(12));
           	while (true) {
        		List<String> list = zk.getChildren(root, false);
        		Integer min = new Integer(list.get(0).substring(5));
        		String minString = list.get(0);
        		for(String s : list){
        			Integer tempValue = new Integer(s.substring(5));
        			if(tempValue < min)  {
        				min = tempValue;
        				minString = s;
        			}
        		}
        		if (suffix.equals(min)) {
        			this.leader();
        			return true;
        		}
        		Integer max = min;
        		String maxString = minString;
        		for(String s : list){
        			Integer tempValue = new Integer(s.substring(5));
        			if(tempValue > max && tempValue < suffix)  {
        				max = tempValue;
        				maxString = s;
        			}
        		}
        		//Exists with watch
        		Stat s = zk.exists(root+"/"+maxString, this);
        		//Step 5
        		if (s != null) {
        			//Wait for notification
        			break;
        		}
        	}
        	return false;
        	
        }
        
        synchronized public void process(WatchedEvent event) {
            synchronized (mutex) {
            	if (event.getType() == Event.EventType.NodeDeleted) {
            		try {
            			boolean success = check();
            			if (success) {
            				fazerPergunta("localhost");
            			}
            		} catch (Exception e) {e.printStackTrace();}
            	}
            }
        }
        
        void leader() throws KeeperException, InterruptedException {
			System.out.println("Voce e o lider!");
            //Create leader znode
            Stat s2 = zk.exists(leader, false);
            if (s2 == null) {
                zk.create(leader, id.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            } else {
            	zk.setData(leader, id.getBytes(), 0);
            }
        }
        
        void fazerPergunta(String ip) {
            //while(true) {
                System.out.println("Faca uma pergunta");
                String pergunta = scanner.nextLine();
                qPergunta.perguntar(pergunta);
                qVotos.resetarVotos();
                System.out.println("Agora responda a pergunta");
                responder();
            //}
            
        }
    }
    
    static void ler(String ip) {
        //while(true) {
            String pergunta = qPergunta.lerPergunta();
            System.out.println(pergunta);
            responder();
        //}
        
    }

    static void responder() {
        String resposta = scanner.nextLine();
        qVotos.votar(resposta, numJogadores);
    }



    public static void main(String args[]) {
        // Generate random integer
        scanner = new Scanner(System.in);
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        qVotos = new Queue(args[0], "/app3/votos");
        qPergunta = new Queue(args[0], "/app3/pergunta");
    	Leader leader = new Leader(args[0],"/election","/leader",r);
        try{
        	boolean success = leader.elect();
        	if (success) {
                leader.fazerPergunta(args[0]);
                while(true) {

                }
        	} else {
                ler(args[0]);
        		while(true) {
        			//Waiting for a notification
        		}
            }         
        } catch (KeeperException e){
        	e.printStackTrace();
        } catch (InterruptedException e){
        	e.printStackTrace();
        }
    }
}