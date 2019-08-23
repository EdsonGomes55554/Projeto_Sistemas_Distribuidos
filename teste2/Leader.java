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

public class Leader extends Eleicao {
    String leader;
    String id; 
    String pathName;
    
    Leader(String address, String name, String leader, int id) {
        super(address);
        this.root = name;
        this.leader = leader;
        this.id = new Integer(id).toString();
        if (zk != null) {
            try {
                Stat s1 = zk.exists(root, false);
                if (s1 == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }  
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
            Stat s = zk.exists(root+"/"+maxString, this);
            if (s != null) {
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
                } catch (Exception e) {e.printStackTrace();}
            }
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
    
    void fazerPergunta(String ip) throws KeeperException, InterruptedException{
        while(true) {
            //verificaVitoria();
            System.out.println("Faca uma pergunta");
            Scanner scanner = new Scanner(System.in);
            String pergunta = scanner.nextLine();
            qPergunta.produce(pergunta);
            System.out.println("Agora responda a pergunta");
            responder();
            qPergunta.consume();
        }
        
    }
}