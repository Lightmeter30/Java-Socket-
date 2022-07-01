import org.json.simple.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 顾名思义,这是服务端
 * @author takune
 */
public class Server {
    public static HashMap<String,Socket> socketHashMap = new HashMap<>();
    public static HashMap<String,User> userHashMap = new HashMap<>();
    public static int count = 0;
    /**
     * 这是服务端的入口
     *  @author takune
     * @param args
     */
    public static void main(String[] args){
        try{
            System.out.println("Server端开始启动...");
            ServerSocket serverSocket = new ServerSocket(9999);
            //创建线程池
            ExecutorService executorService = Executors.newFixedThreadPool(100);

            while(true){
                Socket socket = serverSocket.accept();
                executorService.submit( new ListenServer(count,socket) );

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
 *  @author  takune
 */
class ListenServer implements Runnable{
    private Socket socket;
    private int thisCount;
    private String Uname;
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
                    if((Integer) obj.get("chatType") == 1)  {//私聊模式
                        String toUname = (String) obj.get("toUname");
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(toUname).getOutputStream()));
                        out.write("私聊 "+obj.get("Time")+" "+obj.get("Uname")+":"+obj.get("msg"));
                        out.write("\n");
                        out.flush();
                    }else if((Integer) obj.get("chatType") == 2)  {//群聊模式
                        for(String i: Server.socketHashMap.keySet())
                        {
                            if(!i.equals(Uname)){
                                BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(i).getOutputStream()));
                                out.write("群聊 "+obj.get("Time")+" "+obj.get("Uname")+":"+obj.get("msg"));
                                out.write("\n");
                                out.flush();
                                //out.close();
                            }
                        }
                    }
                }else if(obj.get("type").equals("User")){
                    //System.out.println(obj);//测试obj是否发送成功
                    User u = new User();
                    u.setUname(obj.get("Uname").toString());
                    u.setStatus((Integer) obj.get("Status"));
                    u.setIsAdmin((Integer) obj.get("isAdmin"));
                    Uname = u.getUname();
                    Server.userHashMap.put(u.getUname(),u);
                    Server.socketHashMap.put(u.getUname(),socket);
                    //System.out.println(u.getUname());//测试User类1
                    //System.out.println(u.getStatus());//测试User类2
                }else if(obj.get("type").equals("syscall")){//系统调用
                    //System.out.println(obj);//测试能否正常收到syscall
                    String command = obj.get("command").toString();
                    String content;
                    switch (command){
                        case "AddAdmin":
                            content = obj.get("Uname").toString();
                            AddAdmin(content);
                            break;
                        case "Ban":
                            content = obj.get("Uname").toString();
                            Ban(content);
                            break;
                        case "DisBan":
                            content = obj.get("Uname").toString();
                            DisBan(content);
                            break;
                        case "BroadCast":
                            content = obj.get("msg").toString();
                            BroadCast(content);
                            break;
                    }
                }else if(obj.get("type").equals("getUserList")) {//发送当前在线的用户名单
                    String UserNameList = "当前在线的用户名单:";
                    for(String i : Server.userHashMap.keySet()){
                        UserNameList = UserNameList.concat(i+",");
                    }
                    BufferedWriter out =new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    out.write(UserNameList);
                    out.write("\n");
                    out.flush();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                socket.close();
                Server.socketHashMap.remove(Uname);
                Server.userHashMap.remove(Uname);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 这是用来添加管理员的方法,将改变数据库中的相关信息,Server类中的userHashMap中与之匹配的用户信息,以及对应的Client类中的User信息
     * @param name 添加为管理员的用户名
     * @throws Exception
     */
    public void AddAdmin(String name) throws Exception{
        User u = new User();
        u.setIsAdmin(1);
        u.setUname(name);
        Connection con = DbUtil.getConnection();
        if(!UserDao.changeIsAdmin(con,u))//更新数据库信息
            System.out.println("用户不存在!");
        DbUtil.disConnection(con);
        if(Server.socketHashMap.containsKey(name)){//更新对应用户的客户端信息
            BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(name).getOutputStream()));
            out.write("AddAdmin");
            out.write("\n");
            out.flush();
        }
    }

    /**
     * 这是用来禁言用户的方法,将改变数据库中的相关信息,Server类中的userHashMap中与之匹配的用户信息,以及对应的Client类中的User信息
     * @param name 被禁言的用户名
     * @throws Exception
     */
    public void Ban(String name) throws Exception{
        User u = new User();
        u.setUname(name);
        u.setStatus(0);
        Connection con = DbUtil.getConnection();
        if(!UserDao.changeStatus(con,u))
            System.out.println("用户不存在");
        DbUtil.disConnection(con);
        if(Server.socketHashMap.containsKey(name)){//更新对应用户的客户端信息
            BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(name).getOutputStream()));
            out.write("Ban");
            out.write("\n");
            out.flush();
        }
    }

    /**
     * 这是用来解除禁言的方法,将改变数据库中的相关信息,Server类中的userHashMap中与之匹配的用户信息,以及对应的Client类中的User信息
     * @param name 接触禁言的用户名
     * @throws Exception
     */
    public void DisBan(String name) throws Exception{
        User u = new User();
        u.setUname(name);
        u.setStatus(1);
        Connection con = DbUtil.getConnection();
        if(!UserDao.changeStatus(con,u))
            System.out.println("用户不存在");
        DbUtil.disConnection(con);
        if(Server.socketHashMap.containsKey(name)){//更新对应用户的客户端信息
            BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(name).getOutputStream()));
            out.write("DisBan");
            out.write("\n");
            out.flush();
        }
    }

    /**
     * 这是管理员用来发送广播的方法
     * @param msg 将要发送的广播内容
     * @throws Exception
     */
    public void BroadCast(String msg) throws Exception{
        for(String i: Server.socketHashMap.keySet())
        {
            BufferedWriter out =new BufferedWriter(new OutputStreamWriter(Server.socketHashMap.get(i).getOutputStream()));
            out.write("公告:"+msg);
            out.write("\n");
            out.flush();
            //out.close();
        }
    }
}