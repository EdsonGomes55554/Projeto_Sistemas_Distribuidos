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

public class Main{
    
    
    static String root;
    static String endereco;
    static String resposta;

    static Queue qVotos;
    static Queue qPergunta;
    static Queue qPresentes;
    static Queue qPerdedores;

    static boolean souLider;

    static Barrier barrier;

    static Lock lock;

    

    public static void main(String args[]) {
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        inicializar();
        resetNumPerdedores();
    	Leader leader = new Leader(endereco,"/election","/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (success) {
                jogarLider(endereco);
        	} else {
                jogarNaoLider();
                if(souLider) {
                    jogarLider(endereco);
                }
            }         
        } catch (KeeperException e){
        	e.printStackTrace();
        } catch (InterruptedException e){
        	e.printStackTrace();
        }
    }

    public static void inicializar() {
        qVotos = new Queue(endereco, "/projeto/votos");
        qPergunta = new Queue(endereco, "/projeto/pergunta");
        qPerdedores = new Queue(endereco, "/projeto/perdedores");
        qPresentes = new Queue(endereco, "/projeto/presentes");
        lock = new Lock(endereco,"/projeto/lock", resposta);
    }

    public static void resetNumPerdedores() {
        try {
            qPerdedores.consume();
            qPerdedores.produce("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    static void jogarLider(String ip) {
        while(true) {
            try {
                while(qPresentes.getSize() > 0) {
                    qPresentes.consume();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String pergunta = perguntar();
            resetVotos();
            responder(pergunta);
        }
    }

    static void jogarNaoLider() {
        while(true) {
            if(!souLider) {
                try {
                    String pergunta = qPergunta.read();
                    if(souLider) {
                        break;
                    }
                    responder(pergunta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
    }

    static String perguntar() {
        System.out.println("Faca uma pergunta");
        Scanner scanner = new Scanner(System.in);
        String pergunta = scanner.nextLine();
        try {
            qPergunta.produce(pergunta);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pergunta;
    }

    static void responder(String pergunta) {
        try {
            qPresentes.produce("Presente");
            System.out.println("Responda a pergunta: " + pergunta);
            Scanner scanner = new Scanner(System.in);
            boolean respostaValida = false;
            do {
                resposta = scanner.nextLine();
                if(resposta.equals("sim") || resposta.equals("nao")){
                    respostaValida = true;
                } else {
                    System.out.println("Resposta invalida, responda com apenas sim ou nao");
                }
            } while(!respostaValida);
            barrier = new Barrier(endereco, "/projeto/b1", qPresentes.getSize());
            System.out.println(qPresentes.getSize());
            barrier.enter();
            votar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void votar() {
        try{
            lock.lock();
            while(!lock.testMin()) {
            }
            continuaVotar();  
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void continuaVotar() {
        try {
            int voto;
            int votos = getVotos();
            if(resposta.equals("sim")) {
                voto = 1;
            } else {
                voto = -1;
            }
            votos += voto;
            qVotos.consume();
            qVotos.produce(String.valueOf(votos));
            lock.unlock();
            terminaRodada(voto);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public static void terminaRodada(int voto) {

        try{
            barrier.leave();
            barrier.enter();
            int maioria = getVotos();
            if(maioria > 0) {
                System.out.println("A Maioria votou sim!");
            } else if(maioria == 0) {
                System.out.println("Empate!");
            } else {
                System.out.println("A Maioria votou nao!");
            }
            if(souLider) {
                qPergunta.consume();
            }
            if((voto == 1 && maioria < 0) || (voto == -1 && maioria > 0)) {
                System.out.println("Voce perdeu!");
                new Thread().sleep(2000);
                System.exit(0);
            }
            barrier.leave();
            if(qPresentes.getSize() <= 2) {
                System.out.println("Parabens, voce venceu!");
                try {
                    new Thread().sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
            

        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void resetVotos() {
        try {
            qVotos.consume();
            qVotos.produce("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getVotos() {
        try {
            return Integer.parseInt(qVotos.read());
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


}