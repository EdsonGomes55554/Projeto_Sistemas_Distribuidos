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

public class SyncPrimitive implements Watcher{

    static ZooKeeper zk = null;

    String address;
    String root;
    public static Integer mutex;
    public static int numJogadoresMax = 4;

    SyncPrimitive(String address) {
        if(zk == null){
            try {
                this.address = address;
                mutex = new Integer(-1);
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                Stat s = zk.exists("/forca", false);
                if(s==null){
                    zk.create("/forca", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }catch (KeeperException e) {
                System.out
                        .println("Keeper exception when instantiating queue: "
                                + e.toString());
            }
        }
    }
    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            mutex.notify();
        }
    }
}