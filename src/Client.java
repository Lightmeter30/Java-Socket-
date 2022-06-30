import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 这是客户端类
 *  @author 周文瑞 20373804
 */
public class Client {
    private static Socket socket;
    private static boolean connection_state = false;
    private static Scanner scanner = new Scanner(System.in);
    private static User My;
    private static UserDao userDao = new UserDao();
    private static Connection con;
    //private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    /**
     * 这是服务端入口
     *  @author 周文瑞 20373804
     * @param args
     */
    public static void main(String[] args){
        String str;
        System.out.println("欢迎您使用本聊天室!");
        while(true){
            System.out.print("输入1选择登录,输入2选择注册:");
            str = scanner.nextLine();
            if(str.equals("1")){//登录
                if(login()) break;
            }
            else if(str.equals("2")){//注册
                if(register()){
                    System.out.println("注册成功!现在将跳转到登录!");
                    if(login()) break;
                }
                else{
                    System.out.println("注册失败!请检查用户名和密码!");
                }
            }
            else{
                System.out.println("无效指令!");
            }
        }
        if(connect()){
            System.out.println("客户端连接成功......");
        }else{
            reConnect();
        }
    }

    /**
     * 这是用户端用来登录的函数login();
     * @return 当登录成功时返回true,否则返回false
     */
    public static boolean login(){
        String Uname,PassWord;
        System.out.print("请输入用户名:");
        Uname = scanner.nextLine();
        System.out.print("请输入密码:");
        PassWord = scanner.nextLine();
        User u = new User();
        u.setUname(Uname);
        u.setPassword(PassWord);
        try {
            con = DbUtil.getConnection();
            My = userDao.Login(con,u);
            if(My == null ){
                System.out.println("登录失败,账号不存在或密码错误!");
                return false;
            }else{
                System.out.println("登录成功!");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("登录失败,账号不存在或密码错误!");
            return false;
        }
    }

    public static boolean register(){
        String Uname,PassWord,PassWord1;
        System.out.print("请输入注册用户名:");
        Uname = scanner.nextLine();
        System.out.print("请输入注册密码:");
        PassWord = scanner.nextLine();
        System.out.print("请再次输入注册密码:");
        PassWord1 = scanner.nextLine();
        while(!PassWord.equals(PassWord1)){
            System.out.println("前后两次密码输入不一致,请重新输入!");
            System.out.print("请输入注册密码:");
            PassWord = scanner.nextLine();
            System.out.print("请再次输入注册密码:");
            PassWord1 = scanner.nextLine();
        }
        User u = new User();
        u.setUname(Uname);
        u.setPassword(PassWord);
        try{
            con = DbUtil.getConnection();
            int result = userDao.register(con,u);
            DbUtil.disConnection(con);
            if(result == 1)
                return true;
            else
                return false;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 这是客户端用来连接服务端的函数
     *  @author 周文瑞 20373804
     * @return 连接成功时返回true,连接失败时返回false
     */
    public static boolean connect(){
        try {
            socket = new Socket("127.0.0.1",9999);
            connection_state = true;
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            BufferedReader oin =new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            //executorService.submit(new ClientListen(socket,My,oin));

            //发送用户信息给客户端
            JSONObject obj = new JSONObject();
            obj.put("type","User");
            obj.put("Uname",My.getUname());
            obj.put("Status",My.getStatus());
            oout.writeObject(obj);
            oout.flush();
            new Thread(new ClientListen(socket,My,oin)).start();
            //executorService.submit(new ClientSend(socket,My,oout));
            new Thread(new ClientSend(socket,My,oout)).start();
            //executorService.submit(new ClientHeart(socket,oout));
            new Thread(new ClientHeart(socket,oout)).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            connection_state = false;
            return false;
        }

    }

    /**
     * 这是客户端重新连接连接服务器的函数
     *  @author 周文瑞 20373804
     */
    public static void reConnect(){
        while(!connection_state){
            System.out.println("正在尝试重新连接到服务器......");
            connect();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("服务器重新连接成功!");
    }

    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        Client.socket = socket;
    }

    public static boolean isConnection_state() {
        return connection_state;
    }

    public static void setConnection_state(boolean connection_state) {
        Client.connection_state = connection_state;
    }

}

/**
 * 这是监听客户端的类
 *  @author 周文瑞 20373804
 */
class ClientListen implements Runnable{
    private Socket socket;
    private User My;
    private BufferedReader oin;
    public ClientListen(Socket socket,User My,BufferedReader oin){
        this.socket = socket;
        this.My = My;
        this.oin = oin;
    }

    @Override
    public void run(){
        try {
            while(true){
                //JSONObject obj = (JSONObject) (oin.readObject());
                String str = oin.readLine();
                System.out.println(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 这是向客户端发送信息的类
 */
class ClientSend implements Runnable{
    private Socket socket;
    private User My;
    private ObjectOutputStream oout;
    private Scanner scan = new Scanner(System.in);
    public ClientSend (Socket socket,User My,ObjectOutputStream oout){
        this.socket = socket;
        this.My = My;
        this.oout = oout;
    }

    @Override
    public void run(){
        try {
            //Scanner scan = new Scanner(System.in);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while(true){
                String str = scan.nextLine();
                JSONObject obj = new JSONObject();
                if(str.equals("syscall")){
                    System.out.println("进入管理员模式!");
                    Admin();
                } else{
                    obj.put("type","chat");
                    obj.put("msg",str);
                    obj.put("Uname",My.getUname());
                    obj.put("Time",df.format(System.currentTimeMillis()));
                    oout.writeObject(obj);
                    oout.flush();
                }
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * 管理者模式
     * @throws IOException
     */
    public void Admin() throws IOException{
        while(true){
            String str = scan.nextLine();
        }
    }
}

class ClientHeart implements Runnable{
    private Socket socket;
    private ObjectOutputStream oout;
    public ClientHeart (Socket socket,ObjectOutputStream oout){
        this.socket = socket;
        this.oout =oout;
    }

    @Override
    public void run(){
        try {
            System.out.println("心跳包线程已经启动......");
            while(true){
                Thread.sleep(10000);
                JSONObject obj = new JSONObject();
                obj.put("type","heart");
                obj.put("msg","心跳包");
                oout.writeObject(obj);
                oout.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try{
                socket.close();
                Client.setConnection_state(false);
                Client.reConnect();
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }
    }
}