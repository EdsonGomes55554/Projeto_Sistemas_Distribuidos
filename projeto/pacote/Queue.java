package pacote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.Arrays;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Queue extends SyncPrimitive {

    /**
     * Constructor of producer-consumer queue
     *
     * @param address
     * @param name
     */

    Barrier barrier;
    

    Queue(String address, String name) {
        super(address);
        this.root = name;
        // Create ZK node name
        if (zk != null) {
            try {
                Stat s = zk.exists(root, false);
                
                if (s == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
    }

    /**
     * Add element to the queue.
     *
     * @param i
     * @return
     */

    boolean produce(String i) throws KeeperException, InterruptedException{
        byte[] value = i.getBytes();
        zk.create(root + "/", value, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        return true;
    }

    boolean produceNaoEfemero(String i) throws KeeperException, InterruptedException{
        byte[] value = i.getBytes();
        zk.create(root + "/", value, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
        return true;
    }

    public int getSize() {
        try {
            List<String> list = zk.getChildren(root, true);
            return list.size();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        
        
    }


    /**
     * Remove first element from the queue.
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    String read() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    if(souLider) {
                        return "";
                    }
                    mutex.wait();
                } else {
                    String minString = list.get(0);
                    byte[] b = zk.getData(root +"/"+ minString,false, stat);
                    retvalue = new String (b);
                    return retvalue;
                }
            }
        }
    }

    String consume() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        
        List<String> list = zk.getChildren(root, false);
        if (list.size() == 0) {
            return "";
        } else {
            String minString = list.get(0);
            byte[] b = zk.getData(root +"/"+ minString,false, stat);
            zk.delete(root +"/"+ minString, 0);
            retvalue = new String (b);
            return retvalue;
        }
    }

    
}