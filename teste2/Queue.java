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
import pacote.Eleicao;

public class Queue extends SyncPrimitive {
    

    Queue(String address, String name) {
        super(address);
        this.root = name;
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

    boolean produce(String i) throws KeeperException, InterruptedException{
        byte[] value = i.getBytes();
        zk.create(root + "/", value, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        return true;
    }

    void consume() throws KeeperException, InterruptedException{
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    mutex.wait();
                } else {
                    String minString = list.get(0);
                    zk.delete(root +"/"+ minString, 0);
                    return;
                }
            }
        }
    }

    String ler() throws KeeperException, InterruptedException{
        String retvalue = "";
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    mutex.wait();
                } else {
                    String minString = list.get(0);
                    byte[] b = zk.getData(root +"/"+ minString,false, null);
                    retvalue = new String (b);
                    return retvalue;
                }
            }
        }
    }

    /*String ler() throws KeeperException, InterruptedException{
        String retvalue = "";
        while (this.estaVazio()) {
            if(souLider){
                return retvalue;
            }
        }
        List<String> list = zk.getChildren(root, true);
        String minString = list.get(0);
        byte[] b = zk.getData(root +"/"+ minString,false, null);
        retvalue = new String (b);
        return retvalue;
    }*/

    boolean estaVazio() throws KeeperException, InterruptedException{
        List<String> list = zk.getChildren(root, true);
        if (list.size() == 0) {
            return true;
        } else {
            return false;
        }    
    }

    int tamanhoLista() throws KeeperException, InterruptedException{
        List<String> list = zk.getChildren(root, true);
        return (list.size());   
    }

}