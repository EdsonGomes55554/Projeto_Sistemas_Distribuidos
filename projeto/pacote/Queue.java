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
import pacote.Eleicao;

public class Queue extends SyncPrimitive {

    /**
     * Constructor of producer-consumer queue
     *
     * @param address
     * @param name
     */
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
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] value;

        // Add child with value i
        //b.put(i);
        value = i.getBytes();
        zk.create(root + "/", value, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        return true;
    }


    /**
     * Remove first element from the queue.
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    String consume() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;

        // Get the first element available
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    System.out.println("Esperando pergunta");
                    mutex.wait();
                } else {
                    System.out.println("List: "+list.toString());
                    String minString = list.get(0);
                    byte[] b = zk.getData(root +"/"+ minString,false, stat);
                    //System.out.println("b: " + Arrays.toString(b)); 	
                    //zk.delete(root +"/"+ minString, 0);
                    //ByteBuffer buffer = ByteBuffer.wrap(b);
                    retvalue = new String (b);
                    return retvalue;
                }
            }
        }
    }

    String lerVotos() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;

        // Get the first element available
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    System.out.println("Esperando votos");
                    mutex.wait();
                } else {
                    String minString = list.get(0);
                    System.out.println("Temporary value: " + root +"/"+ minString);
                    byte[] b = zk.getData(root +"/"+ minString,false, stat);
                    //System.out.println("b: " + Arrays.toString(b)); 	
                    //zk.delete(root +"/"+ minString, 0);
                    //ByteBuffer buffer = ByteBuffer.wrap(b);
                    retvalue = new String (b);
                    return retvalue;
                }
            }
        }
    }

    void resetVotos(String ip) throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        // Get the first element available
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    produce("0");
                } else {
                    System.out.println("List: "+list.toString());
                    String minString = list.get(0);
                    System.out.println("Temporary value: " + root +"/"+ minString);
                    zk.delete(root +"/" + minString, 0);
                    produce("0");
                    return;
                }
            }
        }
    }

    public static String lerPergunta(String ip) {
        Queue q = new Queue(ip, "/app3/pergunta");
        try{
            String r = q.consume();
            System.out.println("Item: " + r);
            return r;
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
        return "Não foi possivel ler a pergunta.";
    }

    public static void perguntar(String ip, String pergunta) {
        Queue q = new Queue(ip, "/app3/pergunta");
        try{
            q.produce(pergunta);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public static void resetarVotos(String ip) {
        Queue q = new Queue(ip, "/app3/votos");
        try {
            q.resetVotos(ip);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public static int lerVotos(String ip) {
        Queue q = new Queue(ip, "/app3/votos");
        try {
            return Integer.parseInt(q.lerVotos());
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
        return -1;
    }
}