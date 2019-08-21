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

public class SyncPrimitive implements Watcher {

    static ZooKeeper zk = null;
    static Integer mutexB;
    static Integer mutexL;
    static Integer mutexQ;
    static Integer mutexE;

    static Queue qVotos;
    static Queue qPergunta;
    static Queue qPerdedores;

    String address;
    String root;

    public static int numJogadoresMax = 4;

    SyncPrimitive(String address) {
        if(zk == null){
            try {
                this.address = address;
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutexB = new Integer(-1);
                mutexL = new Integer(-1);
                mutexE = new Integer(-1);
                mutexQ = new Integer(-1);
                Stat s = zk.exists("/projeto", false);
                if(s==null){
                    zk.create("/projeto", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }catch (KeeperException e) {
                System.out
                        .println("Keeper exception when instantiating queue: "
                                + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
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
        synchronized (mutexQ) {
            //System.out.println("Process: " + event.getType());
            mutexQ.notify();
        }

    }

    public void resetPergunta() {
        qPergunta.deletaPergunta();
    }
}