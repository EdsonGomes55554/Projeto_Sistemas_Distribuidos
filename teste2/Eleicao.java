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
import pacote.Barrier;
import pacote.Trava;

public class Eleicao extends SyncPrimitive {
    
    static String endereco;

    static Queue qPergunta;
    static Queue qPresentes;
    static Barrier barrier;
    static Leader leader;

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
            System.out.println("Esperando Pergunta");
            String pergunta = qPergunta.ler();
            if(souLider) {
                break;
            }
            System.out.println(pergunta);
            responder();
        }
    }

    static void responder() throws KeeperException, InterruptedException {
        System.out.println("aaaaaa");
        qPresentes.produce("1");
        Scanner scanner = new Scanner(System.in);
        String resposta = scanner.nextLine();
        barrier = new Barrier(endereco, "/b2", qPresentes.tamanhoLista());
        barrier.enter();
            if(souLider && !qPergunta.estaVazio()){
                qPergunta.consume();
            }
        barrier.leave();
        if(souLider){
            while(!qPresentes.estaVazio()){
                qPresentes.consume();
            }
        }
    }

    public static void startQueues() {
        qPergunta = new Queue(endereco, "/projeto/pergunta");
        qPresentes = new Queue(endereco, "/projeto/presentes");
    }

    public static void main(String args[]) {
        // Generate random integer
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        startQueues();
    	leader = new Leader(endereco,"/election","/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (!success) {
                ler(endereco);
                if(souLider) {
                    leader = new Leader(endereco,"/election","/leader",r);
                    leader.elect();
                }
        	} 
        } catch (KeeperException e){
        	e.printStackTrace();
        } catch (InterruptedException e){
        	e.printStackTrace();
        }
    }
}