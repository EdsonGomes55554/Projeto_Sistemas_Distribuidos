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
            synchronized (mutexQ) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    if(souLider) {
                        return "";
                    }
                    System.out.println("Esperando pergunta");
                    mutexQ.wait();
                } else {
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
            synchronized (mutexQ) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    mutexQ.wait();
                } else {
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

    void resetVotos() throws KeeperException, InterruptedException{
        String retvalue = "";
        Stat stat = null;
        // Get the first element available
        while (true) {
            synchronized (mutexQ) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() == 0) {
                    produce("0");
                } else {
                    String minString = list.get(0);
                    zk.delete(root +"/" + minString, 0);
                    produce("0");
                    return;
                }
            }
        }
    }

    public void votar(String resposta, Trava lock) throws KeeperException, InterruptedException{
        int voto;
        int votos = getVotos();
        if(resposta.equals("sim")) {
            voto = 1;
        } else {
            voto = -1;
        }
        votos += voto;
        List<String> list = zk.getChildren(root, true);
        String minString = list.get(0);
        zk.delete(root +"/" + minString, 0);
        produce(String.valueOf(votos));
        lock.unlock();
        termina(voto);
    }

    public void votar(String resposta, int numJogadores) {
        barrier = new Barrier(address, "/b1", numJogadores);

        
        Trava lock = new Trava(address,"/lock", this, resposta);
        try{
            
            boolean success = lock.lock();
            if (success) {
                //System.out.println("Estou com a lock");
                
                lock.compute();
            } else {
                while(true) {

                }
            }       
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void termina(int voto) {

        try{
            barrier.enter();
            int maioria = getVotos();
            if(maioria > 0) {
                System.out.println("A Maioria votou sim!");
            } else if(maioria == 0) {
                System.out.println("Empate!");
            } else {
                System.out.println("A Maioria votou nao!");
            }
            resetPergunta();
            if((voto == 1 && maioria < 0) || (voto == -1 && maioria > 0)) {
                leave();
            }
            barrier.leave();
            
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void deletaPergunta() {
        try{
            List<String> list = zk.getChildren(root, true);
            if(list.size() != 0) {
                String minString = list.get(0);
                zk.delete(root +"/"+ minString, 0);
            }
        } catch (KeeperException e){
        } catch (InterruptedException e){
        }
    }


    public void leave() {
        System.out.println("Voce perdeu!");
        qPerdedores.somar();
        System.exit(0);
    }

    public void somar() {
        try{
            Stat stat = null;
            List<String> list = zk.getChildren(root, true);
            if(list.size() == 0) {
                produceNaoEfemero("1");
            } else {
                String minString = list.get(0);
                byte[] b = zk.getData(root +"/"+ minString,false, stat);
                int n = Integer.parseInt(new String (b));
                zk.delete(root +"/"+ minString, 0);
                produceNaoEfemero(String.valueOf(n + 1));
            }
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void resetNumPerdedores() {
        try{
            Stat stat = null;
            List<String> list = zk.getChildren(root, true);
            if(list.size() != 0) {
                String minString = list.get(0);
                byte[] b = zk.getData(root +"/"+ minString,false, stat);
                zk.delete(root +"/"+ minString, 0);
            }
            produceNaoEfemero("0");
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        
    }

    public int getNumPerdedores() {
        try{
            Stat stat = null;
            List<String> list = zk.getChildren(root, false);
            if(list.size() == 0) {
                produce("0");
                return 0;
            }
            String minString = list.get(0);
            byte[] b = zk.getData(root +"/"+ minString,false, stat);
            int n = Integer.parseInt(new String (b));
            return n;
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return -1;
    }

    public String lerPergunta() {
        try{
            String r = consume();
            return r;
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
        return "Não foi possivel ler a pergunta.";
    }


    public void perguntar(String pergunta) {
        try{
            produce(pergunta);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public void resetarVotos() {
        try {
            resetVotos();
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
    }

    public int getVotos() {
        try {
            return Integer.parseInt(lerVotos());
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
        e.printStackTrace();
        }
        return -1;
    }
}