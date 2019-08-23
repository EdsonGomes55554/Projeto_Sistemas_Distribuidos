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

public class Eleicao extends SyncPrimitive{
    
    String root;
    static String endereco;

    static Queue qVotos;
    static Queue qPergunta;
    static Queue qPerdedores;
    static Barrier barrier;

    static boolean souLider;

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
    }
    
    static void ler(String ip) throws KeeperException, InterruptedException{
        while(true) {
            if(!souLider) {
                String pergunta = qPergunta.ler(souLider);
                if(souLider) {
                    break;
                }
                System.out.println(pergunta);
                responder();
            } else {
                break;
            }
        }
    }

    static void responder() {
        try{
            Scanner scanner = new Scanner(System.in);
            String resposta = scanner.nextLine();
            barrier = new Barrier(address, "/project/barrier", 2);
            barrier.enter();
            qVotos.votar(resposta, 2);
            barrier.leave();
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void startQueues() {
        qVotos = new Queue(endereco, "/project/votos");
        qPergunta = new Queue(endereco, "/project/pergunta");
        qPerdedores = new Queue(endereco, "/project/perdedores");
    }

    public static void main(String args[]) {
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        startQueues();
    	Leader leader = new Leader(endereco,"/election","/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (success) {
                leader.fazerPergunta(endereco);
        	} else {
                ler(endereco);
                if(souLider) {
                    leader = new Leader(endereco,"/election","/leader",r);
                    leader.fazerPergunta(endereco);
                }
            }         
        } catch (KeeperException e){
        	e.printStackTrace();
        } catch (InterruptedException e){
        	e.printStackTrace();
        }
    }
}