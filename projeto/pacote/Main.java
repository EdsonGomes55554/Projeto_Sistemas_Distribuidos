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

    static boolean souLider;

    static Barrier barrier;

    static Lock lock;

    

    public static void main(String args[]) {
        endereco = args[0];
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        inicializar();
    	Leader leader = new Leader(endereco,"/election","/leader",r);
        try{
            boolean success = leader.elect();
            souLider = success;
        	if (success) {
                jogarLider();
        	} else {
                jogarEleitor();
                if(souLider) {
                    jogarLider();
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
        qPresentes = new Queue(endereco, "/projeto/presentes");
        lock = new Lock(endereco,"/projeto/lock", resposta);
        barrier = new Barrier(endereco, "/projeto/b1", 0);
    }

    static void jogarLider() {
        while(true) {
            try {
                System.out.println("\n---------------------------------------\n");
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

    static void jogarEleitor() {
        while(true) {
            if(!souLider) {
                try {
                    System.out.println("\n---------------------------------------\n");
                    System.out.println("Aguardando proposta");
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
        System.out.println("Faca uma proposta");
        Scanner scanner = new Scanner(System.in);
        String pergunta = scanner.nextLine();
        while(pergunta.equals("")) {
            System.out.println("Proposta invalida");
            pergunta = scanner.nextLine();
        }
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
            System.out.println("Aprove(O) ou disaprove(X): " + pergunta);
            Scanner scanner = new Scanner(System.in);
            boolean respostaValida = false;
            do {
                resposta = scanner.nextLine();
                if(resposta.equals("O") || resposta.equals("X")){
                    respostaValida = true;
                } else {
                    System.out.println("Voto invalido");
                }
            } while(!respostaValida);
            barrier.setSize(qPresentes.getSize());
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
            int voto;
            int votos = getVotos();
            if(resposta.equals("O")) {
                voto = 1;
            } else {
                voto = -1;
            }
            votos += voto;
            qVotos.consume();
            qVotos.produce(String.valueOf(votos));
            lock.unlock();
            barrier.leave();
            terminaRodada(voto);
        } catch (KeeperException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    public static void terminaRodada(int voto) {
        try{
            barrier.enter();
            int maioria = getVotos();
            System.out.println(getVotos());

            int numSim = (int) Math.ceil(qPresentes.getSize()/2.0) + (int) Math.floor(maioria/2.0);
            double porcentagemMaioria = Math.abs(100 * (numSim/(double) (qPresentes.getSize())));

            System.out.println("---------------------------------------");
            System.out.println("O: " + (int) porcentagemMaioria + "%   X: " + ((int) (100 - porcentagemMaioria)) +"%");
            if(maioria > 0) {
                System.out.println("A Maioria aprovou!");
            } else if(maioria == 0) {
                System.out.println("Empate!");
            } else {
                System.out.println("A Maioria disaprovou!");
            }
            System.out.println("---------------------------------------");
            if(souLider) {
                qPergunta.consume();
            }
            if((voto == 1 && maioria < 0) || (voto == -1 && maioria > 0)) {
                System.out.println("Opniao impopular");
                new Thread().sleep(2000);
                System.exit(0);
            }
            barrier.leave();
            if(qPresentes.getSize() <= 2) {
                System.out.println("Impossivel continuar devido a quantidade!");
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