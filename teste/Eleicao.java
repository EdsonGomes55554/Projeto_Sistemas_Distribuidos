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

public class Eleicao extends SyncPrimitive{

    static ZooKeeper zk = null;
    static String endereco;
    String root;

    static boolean souLider;
    static Queue qJogadores;
    static Queue qPalavraEscondida;
    static Queue qPalavra;
    static Queue qLetraEscolhida;
    static Queue qRespostaLider;
    static Barrier b1 ;

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
 
    static void ler()  throws KeeperException, InterruptedException {
        while(true) {
            String palavraImp = qPalavraEscondida.lerPalavra();
            System.out.println(palavraImp);
            b1.enter();
            responder(); 
            b1.leave();
        } 
    }

    static void responder() {
        Scanner scanner = new Scanner(System.in);
        String resposta = scanner.nextLine();
        while(resposta.length() > 1){
            System.out.println("Mais que uma letra digitada!");
            scanner = new Scanner(System.in);
            resposta = scanner.nextLine();
        }
        qLetraEscolhida.escolheLetra(resposta);
    }

    public static void startQueues() {
        qPalavra = new Queue(endereco, "/forca/palavra");
        qPalavraEscondida = new Queue(endereco, "/forca/palavraescodida");
        qLetraEscolhida = new Queue(endereco, "/forca/letraescolhida");
        qRespostaLider = new Queue(endereco, "/forca/respostalider");
        qJogadores = new Queue(endereco, "/forca/jogadores");
        b1 = new Barrier(endereco, "/forca/barreira", 3);
    }

    public static void main(String args[]) {
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        startQueues();
        Leader leader = new Leader(endereco,"/forca/election","/forca/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (success) {
                leader.escolhePalavra();
        	} else {
                ler();
                if(souLider) {
                    leader = new Leader(endereco,"/forca/election","/forca/leader",r);
                    leader.escolhePalavra();
                }
            }         
        } catch (KeeperException e){
        	e.printStackTrace();
        } catch (InterruptedException e){
        	e.printStackTrace();
        }
    }


    static public class Leader extends Eleicao {
    	String leader;
    	String id; 
        String pathName;
        String palavra = "";
        String palavraEscondida = "";
    	
        Leader(String address, String name, String leader, int id) {
            super(address);
            this.root = name;
            this.leader = leader;
            this.id = new Integer(id).toString();
            if (zk != null) {
                try {
                    Stat s1 = zk.exists(root, false);
                    if (s1 == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }  
                    Stat s2 = zk.exists(leader, false);
                    if (s2 != null) {
                        byte[] idLeader = zk.getData(leader, false, s2);
                    }  
                } catch (KeeperException e) {
                    System.out.println("Keeper exception when instantiating queue: " + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }
        
        boolean elect() throws KeeperException, InterruptedException{
        	this.pathName = zk.create(root + "/n-", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        	return check();
        }
        
        boolean check() throws KeeperException, InterruptedException{
        	Integer suffix = new Integer(pathName.substring(18));
           	while (true) {
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
        		if (suffix.equals(min)) {
        			this.leader();
        			return true;
                }
                Integer max = min;
        		String maxString = minString;
        		for(String s : list){
        			Integer tempValue = new Integer(s.substring(5));
        			if(tempValue > max && tempValue < suffix)  {
        				max = tempValue;
        				maxString = s;
        			}
        		}
        		Stat s = zk.exists(root+"/"+maxString, this);
        		if (s != null) {
        			break;
        		}
        	}
        	return false;
        }
        
        synchronized public void process(WatchedEvent event) {
            if (event.getType() == Event.EventType.NodeDeleted) {
                try {
                    boolean success = check();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }  
        }
        
        void leader() throws KeeperException, InterruptedException {
			System.out.println("Voce e o lider!");
            souLider = true;
            Stat s2 = zk.exists(leader, false);
            if (s2 == null) {
                zk.create(leader, id.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            } else {
            	zk.setData(leader, id.getBytes(), 0);
            }
        }
        
        void escolhePalavra()  throws KeeperException, InterruptedException {
            if(qPalavra.estaVazia()){
                System.out.println("Escolha a Palavra:");
                Scanner scanner = new Scanner(System.in);
                String escolha = scanner.nextLine();
                palavra = escolha;
                for(int i = 0; i < palavra.length(); i++){
                    palavraEscondida+="_";
                }
                qPalavraEscondida.atualizaPalavra(palavraEscondida);
                qPalavra.atualizaPalavra(palavra);
            }
            while(true) {
                b1.enter();
                VerificaPalavra();
                b1.leave();
            }
        }

        void VerificaPalavra() throws KeeperException, InterruptedException{
            while(!qLetraEscolhida.estaVazia()){
                int acertos = 0;
                String palavraTemp = "";
                String resp = qLetraEscolhida.getLetraEscolhida();
                for(int i = 0; i < palavra.length(); i++){
                    if((qPalavra.lerPalavra()).charAt(i) == resp.charAt(0))
                    {
                        System.out.println(resp.charAt(0));
                        palavraTemp+=resp.charAt(0);
                        acertos++;
                    }else{
                        palavraTemp+=(qPalavraEscondida.lerPalavra()).charAt(i);
                    }
                }
                palavraEscondida = palavraTemp;
                System.out.println(palavraEscondida);
                if(palavraEscondida.equals(palavra)){
                    //Codição de vitoria
                } else {
                    qPalavraEscondida.consume();
                    qPalavraEscondida.atualizaPalavra(palavraEscondida);
                }
                qLetraEscolhida.consume();
            }
        }
    }
}