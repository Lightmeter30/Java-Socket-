import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 用户Dao类,包含用户注册函数和用户登录函数
 * @author takune
 */
public class UserDao {

    /**
     * 用户登录函数
     * @param con 连接数据库的Connection类
     * @param u     用户类
     * @return resultUser 用户类,登录成功时返回用户类(内包含用户信息),失败时返回null
     * @throws Exception
     */
    public static User Login(Connection con,User u) throws Exception{
        User resultUser = null;
        String sql = "SELECT * FROM User WHERE Uname = ? AND PassWord = ?";
        PreparedStatement pstmt =  con.prepareStatement(sql);
        pstmt.setString(1,u.getUname()); //设置问号1内容
        pstmt.setString(2,u.getPassword()); //设置问号2内容
        ResultSet rs =  pstmt.executeQuery();   //执行sql语句,返回一个结果集
        if(rs.next()) {
            resultUser = new User();
            resultUser.setUID(rs.getInt("UID"));
            resultUser.setUname(rs.getString("Uname"));
            resultUser.setPassword(rs.getString("PassWord"));
            resultUser.setStatus(rs.getInt("Status"));
            resultUser.setIsAdmin(rs.getInt("isAdmin"));
        }
        pstmt.close();
        return resultUser;
    }

    /**
     * 用户注册函数
     * @param con 连接数据库的Connection类
     * @param u 用户类
     * @return 返回值为1时注册成功;返回0时注册失败
     * @throws Exception
     * @author: takune
     */
    public static int register (Connection con, User u)throws Exception{
        String sql = "INSERT INTO User(Uname,PassWord) VALUES(?,?)";
        String UID;
        int result;
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1,u.getUname());
        pstmt.setString(2,u.getPassword());
        result = pstmt.executeUpdate();
        pstmt.close();
        //插入新用户↑
        return result;
    }

    /**
     * 管理员修改用户状态
     * @param con 连接数据库的Connection类
     * @param u 用户类
     * @return 修改状态成功时返回true,否则返回false
     * @throws Exception
     */
    public static boolean changeStatus(Connection con,User u) throws Exception{
        String sql = "UPDATE User SET Status = ? WHERE Uname = ?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1,u.getStatus());
        pstmt.setString(2,u.getUname());
        int result = pstmt.executeUpdate();
        pstmt.close();
        if ( result <= 0 ) return false;
        return true;
    }

    /**
     * 管理员添加新的管理员
     * @param con 连接数据库的Connection类
     * @param u 用户类
     * @return 修改状态成功时返回true,否则返回false
     * @throws Exception
     */
    public static boolean changeIsAdmin(Connection con,User u)throws Exception{
        String sql = "UPDATE User SET isAdmin = ? WHERE Uname = ?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1,u.getIsAdmin());
        pstmt.setString(2,u.getUname());
        int result = pstmt.executeUpdate();
        pstmt.close();
        if ( result <= 0 ) return false;
        return true;
    }

    /**
     * changePassWord()函数用来修改用户自己的密码
     * @param con 连接数据库的Connection类
     * @param u 用户类
     * @return 修改密码成功时返回true,否则返回false
     * @throws Exception
     */
    public static boolean changePassWord(Connection con, User u) throws Exception{
        String sql = "UPDATE User SET PassWord = ? WHERE Uname = ?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1,u.getPassword());
        pstmt.setString(2,u.getUname());
        int result = pstmt.executeUpdate();
        pstmt.close();
        if ( result <= 0 ) return false;
        return true;
    }
}