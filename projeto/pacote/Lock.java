package pacote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import pacote.SyncPrimitive;
import pacote.Queue;

public class Lock extends SyncPrimitive {
    String pathName;
    boolean sucesso;
        /**
     * Constructor of lock
     *
     * @param address
     * @param name Name of the lock node
     */
    Lock(String address, String name, String resposta) {
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

    boolean lock() throws KeeperException, InterruptedException{
        //Step 1
        pathName = zk.create(root + "/lock-", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        //Steps 2 to 5
        return testMin();
    }

    void unlock() throws KeeperException, InterruptedException {
       // System.out.println("Soltei a trava");
        zk.delete(pathName, 0);
    }

    boolean testMin() throws KeeperException, InterruptedException{
		Integer suffix = new Integer(pathName.substring(20));
	         //Step 2 
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
        //Step 3
        if (suffix.equals(min)) {
            return true;
        }
	    return false;
	}
}