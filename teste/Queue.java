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
import pacote.Barrier;
import pacote.Trava;

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

    void produce(String i) throws KeeperException, InterruptedException{
        byte[] value = i.getBytes();
        String x = zk.create(root + "/", value, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    void consume() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        while (true) {
            synchronized (mutex){
                List<String> list = zk.getChildren(root, true);
                if (list.size() != 0) {
                    String minString = list.get(0);
                    zk.delete(root +"/"+ minString, 0);
                    return;
                } else{
                    mutex.wait();
                }
            }
        }
    }

    public boolean estaVazia () throws KeeperException, InterruptedException{
        List<String> list = zk.getChildren(root, true);
        if (list.size() == 0) {
            return true;
        }else{
            return false;
        }
    }

    public String lerPalavra() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        while (true) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() != 0) {
                    String minString = list.get(0);
                    byte[] b = zk.getData(root +"/"+ minString,false, stat);
                    retvalue = new String (b);
                    return retvalue;
                } else {
                    return "";
                }
            }
        }
    

    public void retiraPalavra() {
        try{
            consume();
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public void escolheLetra(String letra) {
        try{
            produce(letra);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public void atualizaPalavra(String palavra) {
        try{
            produce(palavra);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public String getLetraEscolhida() {
        try {
            String r = lerPalavra();
            return r;
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
        return "";
    }

}