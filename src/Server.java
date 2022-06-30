import org.json.simple.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 顾名思义,这是服务端
 * @author 周文瑞 20373804
 */
public class Server {
    public static HashMap<Integer,Socket> socketHashMap = new HashMap<>();
    public static int count = 0;
    /**
     * 这是服务端的入口
     *  @author 周文瑞 20373804
     * @param args
     */
    public static void main(String[] args){
        try{
            System.out.println("Server端开始启动...");
            ServerSocket serverSocket = new ServerSocket(9999);
            //创建线程池
            ExecutorService executorService = Executors.newFixedThreadPool(100);

            while(true){
                boolean t = true;
                Socket socket = serverSocket.accept();
                socketHashMap.put(count,socket);
                executorService.submit( new ListenServer(count,socket) );
                //executorService.submit(new ServerSend(count,socket));
                //new Thread(new ListenServer(count,socket)).start();
                //if(t){
                    //new Thread(new ServerSend()).start();
                    //executorService.submit(new ServerSend(count,socket));
                    //t = false;
                //}
                System.out.println(count);
                count++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

/**
 * 这个类是用来监听是否有客户端接入服务端
 *  @author 周文瑞 20373804
 */
class ListenServer implements Runnable{
    private Socket socket;
    private int thisCount;
    public ListenServer(int thisCount,Socket socket){
        this.thisCount = thisCount;
        this.socket = socket;
    }
    @Override
    public void run(){
        try{
            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            //ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            while(true){
                JSONObject obj = (JSONObject) (oin.readObject());
                if(obj.get("type").equals("chat")){
                    System.out.println(obj.get("Time")+" "+obj.get("Uname")+":"+obj.get("msg"));
                    //BufferedWriter out =new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    for(Integer i: Server.socketHashMap.keySet())
                    {
                        BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(i).getOutputStream()));
                        out.write(obj.get("Time")+" "+obj.get("Uname")+":"+obj.get("msg"));
                        out.write("\n");
                        out.flush();
                        //out.close();
                    }
                    //out.writeObject(obj);
                    //out.flush();
/*
                    //向客户端写
                    JSONObject obj1 = new JSONObject();
                    obj1.put("type","chat");
                    obj1.put("Time",obj.get("Time"));
                    obj1.put("Uname",obj.get("Uname"));
                    obj1.put("msg",obj.get("msg"));
                    for(Integer i: Server.socketHashMap.keySet())
                    {
                        ObjectOutputStream oout1 = new ObjectOutputStream(Server.socketHashMap.get(i).getOutputStream());
                        oout1.writeObject(obj1);
                        oout1.flush();
                        oout1.close();
                    }*/
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                socket.close();
                Server.socketHashMap.remove(thisCount);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

/**
 * 这个类是服务端给客户端发送信息
 *  @author 周文瑞 20373804
 */
class ServerSend implements Runnable{
    private Socket socket;
    private int thiscount;
    public ServerSend(int thiscount,Socket socket){
        this.socket = socket;
        this.thiscount = thiscount;
    }
    @Override
    public void run(){
        try {
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            Scanner scan = new Scanner(System.in);
            while(true){
                System.out.print("请输入要发送的内容:");
                String str = scan.nextLine();
                JSONObject obj = new JSONObject();
                obj.put("type","chat");
                obj.put("msg",str);
                oout.writeObject(obj);
                oout.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}