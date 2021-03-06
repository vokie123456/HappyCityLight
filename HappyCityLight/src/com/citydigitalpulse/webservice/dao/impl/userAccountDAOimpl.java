/** 
 * Copyright (C) 2016 City Digital Pulse - All Rights Reserved
 *  
 * Author: Zhongli Li
 *  
 * Design: Zhongli Li and Shiai Zhu
 *  
 * Concept and supervision Abdulmotaleb El Saddik
 *
 */
package com.citydigitalpulse.webservice.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.el.ELException;

import org.glassfish.hk2.utilities.reflection.Logger;

import com.citydigitalpulse.webservice.dao.UserAccountDAO;
import com.citydigitalpulse.webservice.model.user.Privilege;
import com.citydigitalpulse.webservice.model.user.Role;
import com.citydigitalpulse.webservice.model.user.UserAccount;
import com.citydigitalpulse.webservice.model.user.UserDetail;
import com.citydigitalpulse.webservice.model.user.UserReg;
import com.citydigitalpulse.webservice.tool.Tools;

public class userAccountDAOimpl implements UserAccountDAO {
	private MySQLHelper_User userDB;
	private HashMap<Integer, Role> role_map;
	private long EXPIRE_TIME = 3000000;

	public userAccountDAOimpl() {
		this.userDB = new MySQLHelper_User();
		role_map = new HashMap<Integer, Role>();
	}

	@Override
	public boolean createUser(String email, String password) {
		if (getUserIDbyEmail(email) != -1) {
			// 用户名重复
			return false;
		}
		if (!Tools.emailFormat(email)) {
			// Email格式错误
			return false;
		}
		UserAccount user = new UserAccount(email, password);
		// insert into mark_records
		PreparedStatement ps = null;
		Connection conn = null;
		try {
			conn = userDB.getConnection();

			String sqlString = "INSERT INTO user_account "
					+ "(email, password, enable,create_at) VALUES"
					+ "(?, ?, ?, ?);";
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, user.getEmail());
			ps.setString(2, user.getPassword());
			ps.setBoolean(3, user.isEnabled());
			ps.setLong(4, new Date().getTime());
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		// 设置初始的角色
		ArrayList<Role> roles = new ArrayList<Role>();
		roles.add(getRoleByRoleId(1));// ROLE_USER
		roles.add(getRoleByRoleId(2));// ROLE_GUEST
		long user_id = getUserIDbyEmail(email);
		initUserProfile(user_id, email);
		if (setUserRoles(user_id, roles)) {
			return true;
		} else {
			// 回滚？
			return false;
		}
	}

	/**
	 * @Author Zhongli Li Email: lzl19920403@gmail.com
	 * @param user_id
	 * @param email
	 */
	private void initUserProfile(long user_id, String email) {
		// 1.添加新的用户信息数据
		Connection conn = null;
		Statement statement = null;
		try {
			conn = userDB.getConnection();
			// 指定在事物中提交
			conn.setAutoCommit(false);
			statement = conn.createStatement();
			statement.executeUpdate("INSERT INTO user_detail "
					+ "(user_id,email) VALUES" + "(" + user_id + ", '" + email
					+ "');");
			// 2.设置账户为启用
			statement
					.executeUpdate("UPDATE user_account SET enable = 1 WHERE user_id = "
							+ user_id + ";");
			// 提交更改
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			// 有错误发生回滚修改
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
				Logger.printThrowable(e1);
				throw new RuntimeException(e1);
			}
			throw new RuntimeException(e);
		} finally {
			try {
				if (statement != null) {

					statement.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public long getUserIDbyEmail(String email) {
		PreparedStatement ps = null;
		Connection conn = null;
		long res = -1;
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT user_id FROM user_account WHERE email = ?;";
			ps = conn.prepareStatement(sqlString);

			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res = rs.getLong("user_id");
				return res;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public int getRoleIdByRoleName(String roleName) {
		PreparedStatement ps = null;
		Connection conn = null;
		int res = -1;
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT role_id FROM role WHERE role_name = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, roleName);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res = rs.getInt("role_id");
				return res;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public UserAccount getUserAccountByUserID(long userID) {
		UserAccount res = new UserAccount();
		PreparedStatement ps = null;
		Connection conn = null;
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT * FROM user_account WHERE user_id = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res.setUser_id(rs.getLong("user_id"));
				res.setEmail(rs.getString("email"));
				res.setCreated_on(new Date(rs.getLong("create_at")));
				res.setPassword(rs.getString("password"));
				res.setEnabled(rs.getBoolean("enable"));
				res.setLogin_token(rs.getString("login_token"));
				res.setToken_expire_date(rs.getLong("token_expire_date"));
				ArrayList<Role> roles = getUserRolesByUserId(userID);
				res.setRoles(roles);
				return res;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public UserAccount getUserAccountByEmail(String email) {
		UserAccount res = null;
		PreparedStatement ps = null;
		Connection conn = null;
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT * FROM user_account WHERE email = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res = new UserAccount();
				res.setUser_id(rs.getLong("user_id"));
				res.setEmail(rs.getString("email"));
				res.setCreated_on(new Date(rs.getLong("create_at")));
				res.setPassword(rs.getString("password"));
				res.setEnabled(rs.getBoolean("enable"));
				res.setLogin_token(rs.getString("login_token"));
				res.setToken_expire_date(rs.getLong("token_expire_date"));
				ArrayList<Role> roles = getUserRolesByUserId(res.getUser_id());
				res.setRoles(roles);
				return res;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public ArrayList<Role> getUserRolesByUserId(long userID) {
		// 1. 在用户-角色表里面找到用户对应的角色编号
		PreparedStatement ps = null;
		Connection conn = null;
		ArrayList<Role> res = new ArrayList<Role>();
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT role_id FROM user_role WHERE user_id = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Role r = new Role(rs.getInt("role_id"));
				res.add(r);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		// 通过角色编号在角色表中找到角色的名称和对应的权限
		return updateRoles(res);
	}

	private ArrayList<Role> updateRoles(ArrayList<Role> raw) {
		ArrayList<Role> res = new ArrayList<Role>();
		for (int i = 0; i < raw.size(); i++) {
			// 根据角色ID查询角色名称以及权限
			res.add(getRoleByRoleId(raw.get(i).getId()));
		}
		return res;
	}

	/**
	 * 通过角色ID更新角色的详细信息
	 * 
	 * @param role_id
	 * @return
	 */
	private Role getRoleByRoleId(int role_id) {
		// 如果角色在缓存中则直接调用缓存中数据，若不在缓存中则重新从数据库中读取
		if (role_map.containsKey(role_id)) {
			return role_map.get(role_id);
		} else {
			Role res = null;
			PreparedStatement ps1 = null;
			PreparedStatement ps2 = null;
			PreparedStatement ps3 = null;
			Connection conn = null;
			try {
				conn = userDB.getConnection();
				// 1. 通过角色ID得到角色名称
				String sqlString1 = "SELECT role_name FROM role WHERE role_id = ?;";
				ps1 = conn.prepareStatement(sqlString1);
				ps1.setLong(1, role_id);
				ResultSet rs1 = ps1.executeQuery();
				while (rs1.next()) {
					res = new Role(rs1.getString("role_name"));
					res.setId(role_id);
					// 2. 通过角色ID获得角色权限ID
					ArrayList<Privilege> privileges = new ArrayList<Privilege>();
					String sqlString2 = "SELECT privilege_id FROM role_privilege WHERE role_id = ?;";
					ps2 = conn.prepareStatement(sqlString2);
					ps2.setLong(1, role_id);
					ResultSet rs2 = ps2.executeQuery();
					while (rs2.next()) {
						Privilege pri = new Privilege();
						pri.setId(rs2.getInt("privilege_id"));
						privileges.add(pri);
					}
					for (int i = 0; i < privileges.size(); i++) {
						// 3.通过权限ID获得权限名称并补充权限对象
						String sqlString3 = "SELECT privilege_name FROM privilege WHERE privilege_id = ?;";
						ps3 = conn.prepareStatement(sqlString3);
						ps3.setLong(1, privileges.get(i).getId());
						ResultSet rs3 = ps3.executeQuery();
						while (rs3.next()) {
							privileges.get(i).setName(
									rs3.getString("privilege_name"));
							privileges.get(i).setDescription(
									rs3.getString("privilege_description"));
						}
					}
					res.setPrivileges(privileges);
					role_map.put(role_id, res);
					System.out.println(res);
					return res;
				}

			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
				// return null;
			} finally {
				try {
					ps1.close();
					ps2.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						e.printStackTrace();
						Logger.printThrowable(e);
						throw new RuntimeException(e);
					}
				}
			}
			return null;
		}
	}

	@Override
	public boolean changePassword(long userID, String password) {
		PreparedStatement ps = null;
		String sqlString = "UPDATE user_account SET password = ? WHERE user_id = ?;";
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, password);
			ps.setLong(2, userID);
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
			// return false;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public boolean isUserAvailable(long userID) {
		// 1. 在用户-角色表里面找到用户对应的角色编号
		PreparedStatement ps = null;
		Connection conn = null;
		boolean res = false;
		try {
			conn = userDB.getConnection();

			String sqlString = "SELECT enable FROM user_account WHERE user_id = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res = rs.getBoolean("enable");
				break;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public boolean userActive(long userID) {
		// 去除用户的GUEST属性
		Connection conn = null;
		Statement statement = null;
		try {
			conn = userDB.getConnection();
			// 更改权限
			statement = conn.createStatement();
			statement.executeUpdate("DELETE FROM user_role WHERE userID="
					+ userID + " and role_id=2;");
			// 提交更改
			conn.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			// 有错误发生回滚修改
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
				Logger.printThrowable(e1);
				throw new RuntimeException(e1);
			}
			throw new RuntimeException(e);
		} finally {
			try {
				if (statement != null) {

					statement.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public boolean isPasswordMatch(long userID, String password) {
		// 1. 在用户-角色表里面找到用户对应的角色编号
		PreparedStatement ps = null;
		Connection conn = null;
		boolean res = false;
		try {
			conn = userDB.getConnection();
			String sqlString = "SELECT password FROM user_account WHERE user_id = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (password.equals(rs.getString("password"))) {
					return true;
				} else {
					return false;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	/**
	 * 应用事务一次提交多个操作
	 */
	@Override
	public boolean setUserRoles(long userID, ArrayList<Role> roles) {
		Connection conn = null;
		Statement statement = null;
		try {
			conn = userDB.getConnection();
			// 指定在事物中提交
			conn.setAutoCommit(false);
			statement = conn.createStatement();
			// 1.删除之前的角色
			statement.executeUpdate("DELETE FROM user_role WHERE user_id="
					+ userID);
			// 2.添加新的角色
			for (int i = 0; i < roles.size(); i++) {
				statement
						.executeUpdate("INSERT INTO user_role (user_id, role_id) VALUES ("
								+ userID + ", " + roles.get(i).getId() + ");");
			}
			// 提交更改
			conn.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			// 有错误发生回滚修改
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
				Logger.printThrowable(e1);
				throw new RuntimeException(e1);
			}
			throw new RuntimeException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean addUserRole(long userID, Role role) {
		// 1.获得已有的角色
		ArrayList<Role> oldRoles = getUserRolesByUserId(userID);
		if (role.getName() == "") {
			role = getRoleByRoleId(role.getId());
			if (role == null) {
				// 角色编号错误
				return false;
			}
		}
		if (oldRoles.contains(role)) {
			// 已经有这个角色
			return true;
		}
		if (getRoleIdByRoleName(role.getName()) == -1) {
			// 该角色名称不存在
			return false;
		}
		// 2.若以前没有，则添加角色
		PreparedStatement ps = null;
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			String sqlString = "INSERT INTO user_role (user_id, role_id) VALUES ( ? , ? )";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ps.setInt(2, role.getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public boolean delUserRole(long userID, Role role) {
		// 1.获得已有的角色
		ArrayList<Role> oldRoles = getUserRolesByUserId(userID);
		if (role.getName() == "") {
			role = getRoleByRoleId(role.getId());
			if (role == null) {
				// 角色编号错误
				return false;
			}
		}
		if (getRoleIdByRoleName(role.getName()) == -1) {
			// 该角色名称不存在
			return false;
		}
		if (!oldRoles.contains(role)) {
			// 如果本来就没有有这个角色，则无更改
			return true;
		}
		// 2.如果有这个角色，则删除
		PreparedStatement ps = null;
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			String sqlString = "DELETE FROM users_role WHERE user_id = ? and role_id = ? ";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ps.setInt(2, role.getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public String updateToken(long userID) {
		String token = "";
		try {
			token = Tools.HmacSHA256Encrypt(java.util.UUID.randomUUID()
					.toString(), "authword");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		}
		PreparedStatement ps = null;
		String sqlString = "UPDATE user_account SET login_token = ? , token_expire_date= ? WHERE user_id = ?;";
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, token);
			// 过期时间为一个月
			ps.setLong(2, (new Date().getTime() + EXPIRE_TIME));
			ps.setLong(3, userID);
			ps.execute();
			return token;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public boolean tokenCheck(long userID, String token) {
		try {

			long time = new Date().getTime();
			UserAccount user = getUserAccountByUserID(userID);
			System.out.println(user.getRoles());
			// 测试账户
			if (user.getRoles().contains(new Role("ROLE_GUEST"))) {
				return true;
			} else {
				if (user.getLogin_token().equals(token)
						&& user.getToken_expire_date() > time
						&& !user.getLogin_token().equals("logout")
						&& !user.getLogin_token().equals("")) {
					System.out.println(user.getLogin_token() + " <> " + token);
					return true;
				} else {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new ELException("Auth error");
		}
	}

	@Override
	public UserDetail getUserDetailByUserId(long userID) {
		// 1. 在用户-角色表里面找到用户对应的角色编号
		PreparedStatement ps = null;
		Connection conn = null;
		UserDetail res = null;
		try {
			conn = userDB.getConnection();
			String sqlString = "SELECT * FROM user_detail WHERE user_id = ?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				res = new UserDetail();
				res.setAge(rs.getInt("age"));
				res.setFirstname(rs.getString("firstname"));
				res.setMidlename(rs.getString("midlename"));
				res.setLastname(rs.getString("lastname"));
				res.setGender(rs.getInt("gender"));
				res.setEmail(rs.getString("email"));
				res.setNickname(rs.getString("nickname"));
				res.setUser_id(userID);
				res.setUser_picture(rs.getString("user_picture"));
				res.setRoles(getUserRolesByUserId(userID));
				return res;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
		return res;
	}

	@Override
	public boolean tokenReset(long userID) {
		String token = "logout";
		PreparedStatement ps = null;
		String sqlString = "UPDATE user_account SET login_token = ? WHERE user_id = ?;";
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			ps = conn.prepareStatement(sqlString);
			ps.setString(1, token);
			ps.setLong(2, userID);
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * 获取该用户收藏的地区列表
	 * 
	 * @Author Zhongli Li Email: lzl19920403@gmail.com
	 * @param user_id
	 * @return
	 */
	@Override
	public List<UserReg> getUserRegInfo(long user_id) {
		// 根据type获取大区域的信息
		String sqlString = "SELECT * FROM user_reg where user_id =" + user_id;
		ArrayList<UserReg> result = new ArrayList<UserReg>();
		// 查询数据库，获取结果
		Connection conn = null;
		try {
			conn = userDB.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlString);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				UserReg ur = new UserReg();
				ur.setUser_id(user_id);
				ur.setAdd_date(rs.getLong("add_date"));
				ur.setReg_id(rs.getInt("reg_id"));
				ur.setReg_name(rs.getString("reg_name"));
				result.add(ur);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		// System.out.println(result.size());
		return result;
	}

	@Override
	public int getUserRegionRel(long userID, int reg_id) {
		PreparedStatement ps = null;
		Connection conn = null;
		int res = -1;
		try {
			conn = userDB.getConnection();
			String sqlString = "SELECT rel_id FROM user_reg where user_id=? and reg_id=?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ps.setInt(2, reg_id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				return rs.getInt("rel_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return res;
	}

	@Override
	public void addUserRegion(long userID, int reg_id, String reg_name) {
		PreparedStatement ps = null;
		Connection conn = null;
		if (getUserRegionRel(userID, reg_id) != -1) {
			// Arready have
		} else {
			try {
				conn = userDB.getConnection();
				String sqlString = "INSERT INTO user_reg (user_id, reg_id, add_date, reg_name) VALUES (?, ?, ?, ?);";
				ps = conn.prepareStatement(sqlString);
				ps.setLong(1, userID);
				ps.setInt(2, reg_id);
				ps.setLong(3, new Date().getTime());
				ps.setString(4, reg_name);
				ps.execute();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			} finally {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
					Logger.printThrowable(e);
					throw new RuntimeException(e);
				}
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
					}
				}
			}
		}
	}

	@Override
	public int getUserRegRef(long userID, String reg_name) {
		PreparedStatement ps = null;
		Connection conn = null;
		int res = -1;
		try {
			conn = userDB.getConnection();
			String sqlString = "SELECT rel_id FROM user_reg where user_id=? and reg_name=?;";
			ps = conn.prepareStatement(sqlString);
			ps.setLong(1, userID);
			ps.setString(2, reg_name);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				return rs.getInt("rel_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Logger.printThrowable(e);
			throw new RuntimeException(e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				Logger.printThrowable(e);
				throw new RuntimeException(e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return res;
	}

}
