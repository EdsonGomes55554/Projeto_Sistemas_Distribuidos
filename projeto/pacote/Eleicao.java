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

public class Eleicao extends SyncPrimitive implements Watcher {

    static ZooKeeper zk = null;
    
    static boolean souLider;
    String root;
    static String endereco;



    Eleicao(String address) {
        super(address);
        if(zk == null){
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutexE = new Integer(-1);
    }

    /*synchronized public void process(WatchedEvent event) {
        synchronized (mutexE) {
            mutexE.notify();
        }
    }*/

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
        	Integer suffix = new Integer(pathName.substring(21));
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
            synchronized (mutexQ) {
            	if (event.getType() == Event.EventType.NodeDeleted) {
            		try {
            			boolean success = check();
            			if (success) {
            				fazerPergunta(endereco);
            			}
            		} catch (Exception e) {e.printStackTrace();}
            	}
            }
            synchronized (mutexB) {
                //System.out.println("Process: " + event.getType());
                mutexB.notifyAll();
            }
            synchronized (mutexL) {
                //System.out.println("Process: " + event.getType());
                mutexL.notify();
            }
            synchronized (mutexE) {
                //System.out.println("Process: " + event.getType());
                mutexE.notify();
            }
        }
        
        void leader() throws KeeperException, InterruptedException {
			System.out.println("Voce e o lider!");
            //Create leader znode
            souLider = true;
            Stat s2 = zk.exists(leader, false);
            if (s2 == null) {
                zk.create(leader, id.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            } else {
            	zk.setData(leader, id.getBytes(), 0);
            }
        }
        
        void fazerPergunta(String ip) {
            while(true) {
                verificaVitoria();
                System.out.println("Faca uma pergunta");
                Scanner scanner = new Scanner(System.in);
                String pergunta = scanner.nextLine();
                qPergunta.perguntar(pergunta);
                qVotos.resetarVotos();
                System.out.println("Agora responda a pergunta");
                responder();
            }
            
        }
    }

    public static void verificaVitoria() {
        if(getNumJogadores() <= 2) {
            System.out.println("Parabens, voce ganhou!");
            System.exit(0);
        }
    }

    public static int getNumJogadores() {
        System.out.println(numJogadoresMax -  qPerdedores.getNumPerdedores());
        return numJogadoresMax - qPerdedores.getNumPerdedores();

    }
    
    static void ler(String ip) {
        while(true) {
            if(!souLider) {
                verificaVitoria();
                String pergunta = qPergunta.lerPergunta();
                System.out.println(pergunta);
                responder();
            } else {
                break;
            }
            
        }
        
    }

    static void responder() {
        Scanner scanner = new Scanner(System.in);
        String resposta = scanner.nextLine();
        qVotos.votar(resposta, getNumJogadores());
    }

    public static void startQueues() {
        qVotos = new Queue(endereco, "/projeto/votos");
        qPergunta = new Queue(endereco, "/projeto/perguntas");
        qPerdedores = new Queue(endereco, "/projeto/perdedores");
    }

    public static void main(String args[]) {
        // Generate random integer
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        startQueues();
        qPerdedores.resetNumPerdedores();
    	Leader leader = new Leader(endereco,"/projeto/election","/projeto/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (success) {
                leader.fazerPergunta(endereco);
                while(true) {

                }
        	} else {
                ler(endereco);
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