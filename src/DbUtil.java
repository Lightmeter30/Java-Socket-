import java.sql.*;

/**
 * 数据库工具类
 * @author takune
 */
public class DbUtil {
    /**
     * 连接数据库
     * @return con
     * @throws Exception
     */
    public static Connection getConnection()throws Exception{
        String dbUrl = "jdbc:mysql://43.138.40.55:3306/JavaPersonal";//数据库连接地址
        String dbUsername = "javauser"; //数据库用户名字
        String dbPassWord = "123456";   //数据库用户密码
        //String jdbcName = "com.mysql.jdbc.Driver";  //mysql驱动;
        //Class.forName(jdbcName);
        Connection con = DriverManager.getConnection(dbUrl,dbUsername,dbPassWord);
        return con;
    }
    /**
     * 断开数据库,建议不用数据库时可以断开
     * @param con
     */
    public static void disConnection(Connection con)throws Exception{
        con.close();
    }

    /**
     * 数据库连接测试
     * @param args
     */
    public static void main(String[] args){
        try{
            Connection con = DbUtil.getConnection();
            System.out.println("数据库连接成功!");
            DbUtil.disConnection(con);
            System.out.println("数据库断开连接!");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("数据库连接失败!");
        }
    }
}