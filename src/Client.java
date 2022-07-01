import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Scanner;


/**
 * 这是客户端类
 *  @author takune
 */
public class Client {
    private static Socket socket;
    private static boolean connection_state = false;
    private static Scanner scanner = new Scanner(System.in);
    private static User My;
    private static Connection con;
    private static int chatType;//1为私聊模式,2为群聊模式
    public static String privateChatName;
    public static String aliveUnameList;
    //private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    /**
     * 这是服务端入口
     *  @author takune
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
        if(!connect())
            reConnect();
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
            My = UserDao.Login(con,u);
            if(My == null ){
                System.out.println("登录失败,账号不存在或密码错误!");
                DbUtil.disConnection(con);
                return false;
            }else{
                System.out.println("登录成功!");
                DbUtil.disConnection(con);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("登录失败,账号不存在或密码错误!");
            return false;
        }
    }

    /**
     * 这是用户端用来注册的函数register(),注册成功后会直接跳转到login()
     * @return 当注册成功时返回true,注册失败时返回false
     */
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
            int result = UserDao.register(con,u);
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
     *  @author takune
     * @return 连接成功时返回true,连接失败时返回false
     */
    public static boolean connect(){
        try {
            socket = new Socket("127.0.0.1",9999);
            connection_state = true;
            System.out.println("客户端连接成功......");
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            BufferedReader oin =new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            //executorService.submit(new ClientListen(socket,My,oin));

            //发送用户信息给客户端
            JSONObject obj = new JSONObject();
            obj.put("type","User");
            obj.put("Uname",My.getUname());
            obj.put("Status",My.getStatus());
            obj.put("isAdmin",My.getIsAdmin());
            oout.writeObject(obj);
            oout.flush();

            new Thread(new ClientListen(socket,My,oin)).start();
            //executorService.submit(new ClientSend(socket,My,oout));
            //executorService.submit(new ClientHeart(socket,oout));
            new Thread(new ClientHeart(socket,oout)).start();
            //设置聊天相关配置
            while(true){
                System.out.print("输入1进入1对1私聊模式,输入2进入群聊模式:");
                String chatCommand = scanner.nextLine();
                if(chatCommand.equals("1")) {//进入一对一私聊模式
                    chatType = 1;
                    privateChat(oout);//从服务端获取当前在线用户名单
                    break;
                }
                else if(chatCommand.equals("2")) {//进入群聊模式
                    chatType = 2;
                    break;
                }
                else {
                    System.out.println("无效指令!");
                }
            }
            //发送信息的线程
            new Thread(new ClientSend(socket,My,oout)).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            connection_state = false;
            return false;
        }
    }

    /**
     * privateChat():用来从服务端获取当前在线用户名单,并且设置私聊用户名
     * @param oout 输出流
     * @throws IOException
     */
    public static void privateChat(ObjectOutputStream oout) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("type","getUserList");
        oout.writeObject(obj);
        oout.flush();
        //System.out.print("请输入你想要私聊的用户名:");
        privateChatName = scanner.nextLine();
        while(true){
            if(privateChatName.equals(My.getUname())){
                System.out.println("不能输入自己的用户名!");
                System.out.print("请输入你想要私聊的用户名:");
                privateChatName = scanner.nextLine();
            }else if(!aliveUnameList.contains(privateChatName)){
                System.out.println("已登录用户中没有"+privateChatName);
                System.out.print("请输入你想要私聊的用户名:");
                privateChatName = scanner.nextLine();
            }else if(aliveUnameList.contains(privateChatName)){
                break;
            }
        }
    }

    /**
     * 这是客户端重新连接连接服务器的函数
     *  @author takune
     */
    public static void reConnect(){
        while(!connection_state){
            System.out.println("正在尝试重新连接到服务器......");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connect();
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

    public static int getChatType() {
        return chatType;
    }

    public static void setChatType(int chatType) {
        Client.chatType = chatType;
    }
}

/**
 * 这是监听客户端的类
 *  @author takune
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
                if(str.contains(":")){//打印聊天消息和通告和用户名单
                    System.out.println(str);
                    if(str.contains("当前在线的用户名单:"))  {
                        String[] Inf = str.split(":");
                        Client.aliveUnameList = Inf[1];
                        System.out.print("请输入你想要私聊的用户名:");
                    }
                }
                else
                {
                    switch (str){
                        case "AddAdmin":
                            if(My.getIsAdmin() == 0){
                                My.setIsAdmin(1);
                                System.out.println("您已被设置为管理员!");
                                //System.out.println(My.getIsAdmin());//测试一下是否设置成功
                            }
                            break;
                        case "Ban":
                            if(My.getStatus() == 1){
                                My.setStatus(0);
                                System.out.println("您已经被管理员禁言!");
                                //System.out.println(My.getStatus());//测试一下是否设置成功
                            }
                            break;
                        case "DisBan":
                            if(My.getStatus() == 0){
                                My.setStatus(1);
                                System.out.println("您已经被管理员解除禁言");
                                //System.out.println(My.getStatus());//测试一下是否设置成功
                            }
                    }
                }
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
                    System.out.println("进入Syscall模式!");
                    if(My.getIsAdmin() == 1 && Client.getChatType() == 2)//管理员在群聊状态下的syscall指令
                        System.out.print("请输入以下六条命令(AddAdmin,Ban,DisBan,Exit,BroadCast,ChangeChatType):");
                    else
                        System.out.print("请输入一下一条指令(ChangeChatType):");
                    Syscall();
                }else if(Client.getChatType() == 1){//私聊
                    obj.put("type","chat");
                    obj.put("chatType",1);
                    obj.put("toUname",Client.privateChatName);
                    obj.put("msg",str);
                    obj.put("Uname",My.getUname());
                    obj.put("Time",df.format(System.currentTimeMillis()));
                    oout.writeObject(obj);
                    oout.flush();
                }
                else if(Client.getChatType() == 2){//群聊
                    if(My.getStatus() == 1) {
                        obj.put("type","chat");
                        obj.put("chatType",2);
                        obj.put("msg",str);
                        obj.put("Uname",My.getUname());
                        obj.put("Time",df.format(System.currentTimeMillis()));
                        oout.writeObject(obj);
                        oout.flush();
                    }
                    else {
                        System.out.println("您已被禁言!");
                    }

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
    public void Syscall() throws IOException{
        while(true){
            String str = scan.nextLine();
            String name ;
            JSONObject obj = new JSONObject();
            obj.put("type","syscall");
            switch (str){
                case "AddAdmin":
                    if(My.getIsAdmin() == 1 && Client.getChatType() == 2 ){
                        System.out.print("请输入你要设置成管理员的用户名:");
                        name = scan.nextLine();
                        obj.put("command","AddAdmin");
                        obj.put("Uname",name);
                        oout.writeObject(obj);
                        oout.flush();
                    }
                    break;
                case "Ban":
                    if(My.getIsAdmin() == 1 && Client.getChatType() == 2 ){
                        System.out.print("请输入你要禁言的用户名:");
                        name = scan.nextLine();
                        obj.put("command","Ban");
                        obj.put("Uname",name);
                        oout.writeObject(obj);
                        oout.flush();
                    }
                    break;
                case "DisBan":
                    if(My.getIsAdmin() == 1 && Client.getChatType() == 2 ){
                        System.out.print("请输入你要解禁的用户名:");
                        name = scan.nextLine();
                        obj.put("command","DisBan");
                        obj.put("Uname",name);
                        oout.writeObject(obj);
                        oout.flush();
                    }
                    break;
                case "BroadCast":
                    if(My.getIsAdmin() == 1 && Client.getChatType() == 2 ){
                        System.out.print("请输入广播内容:");
                        name = scan.nextLine();
                        obj.put("command","BroadCast");
                        obj.put("msg",name);
                        oout.writeObject(obj);
                        oout.flush();
                    }
                    break;
                case "ChangeChatType":
                    if(Client.getChatType() == 1){
                        Client.setChatType(2);
                        System.out.println("切换为群聊模式!");
                    }else if(Client.getChatType() == 2){
                        Client.privateChat(oout);
                        Client.setChatType(1);
                        System.out.println("切换为私聊模式!");
                    }
                case "Exit":
                    System.out.println("退出syscall模式!");
                    return;
            }
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
            //System.out.println("心跳包线程已经启动......");
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