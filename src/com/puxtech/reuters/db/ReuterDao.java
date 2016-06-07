package com.puxtech.reuters.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.puxtech.reuters.model.ContractOffset;
import com.puxtech.reuters.model.T_offset;
import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.SpreadConsumer;
import com.puxtech.reuters.rfa.Common.SubNode;

public class ReuterDao {
	private static final Log reuterdaoLog = LogFactory.getLog("reuterdao");
	private static ReuterDao cm;

	public static ReuterDao getInstance() {
		if (cm == null) {
			cm = new ReuterDao();
			return cm;
		} else {
			return cm;
		}
	}

	/**
	 * 更新价差表：t_contractoffset 主键：SRC, OCCURTIME, WORLDCOMMODITYID
	 * 
	 * @param co
	 */
	public void insertT_contractoffset(ContractOffset co) {
		try {
			Connection con = DBConnUitl.INSTANCE.GetConnection();
			reuterdaoLog.info("连接数据库成功，开始insert t_contractoffset " + co);
			String sql = "insert into t_contractoffset (OFFSET, OCCURTIME, SRC, OLDCONTRACT, NEWCONTRACT,worldcommodityid,switchdate) values (?, ?, ?, ?, ?,? ,?)";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setDouble(1, co.getOffset());
			ps.setTimestamp(2, new Timestamp(co.getOccurtime().getTime()));
			ps.setString(3, co.getSrc());
			ps.setString(4, co.getOldContract());
			ps.setString(5, co.getNewContract());
			ps.setString(6, co.getWorldCommodityId());
			ps.setString(7, co.getSwitchDate());
			ps.executeUpdate();
			if (ps != null) {
				ps.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (SQLException e) {
			reuterdaoLog.info(e.getMessage());
			e.printStackTrace();
		} finally{
			reuterdaoLog.info("insert t_contractoffset结束");
		}
	}


	/**
	 * 读取t_spread表，拼装成List<ConfigNode>
	 * 
	 * @param quoteSource
	 *            行情源名字
	 * @return
	 */
	public static List<ConfigNode> querySpread(String quoteSource) {
		List<ConfigNode> configList = new ArrayList<ConfigNode>();
		try {
			Connection con = DBConnUitl.INSTANCE.GetConnection();
			String sql = "select "
					+ "a.exchangecode,"
					+ "a.quotesource,"
					+ "a.spreadconsumer,"
					+ "a.spreadindex,"
					+ "a.contractcode,"
					+ "a.attenbegintime,"
					+ "a.changecontracttime,"
					+ "a.attenendtime,"
					+ "(a.attenendtime - a.attenbegintime) * 24 * 60 * 60 as diffPricePeriod  "
					+ "from t_spread a "
					+ "where a.quotesource = ? "
					+ "and (systimestamp < a.attenbegintime or systimestamp = a.attenbegintime) "
					+ "order by a.exchangecode,a.spreadindex";
			reuterdaoLog.info("连接数据库成功，开始select t_spread，sql=" + sql);
			reuterdaoLog.info("连接数据库成功，开始select t_spread，sql参数=" + quoteSource);
			System.out.println(sql);
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, quoteSource);
			ResultSet rs = ps.executeQuery();
			boolean continueFlag = true;
			SimpleDateFormat formattime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (rs.next()) {
				continueFlag = true;
				for (ConfigNode cnd : configList) {
					if (cnd.getExchangeCode().equals(rs.getString("exchangeCode")) && cnd.getQuoteSourceName().equals(rs.getString("quoteSource"))) {
						SubNode sn = new SubNode();
						sn.setChangeContractTime(new Date(rs.getTimestamp("changecontracttime").getTime()));
						sn.setAttenBeginTime((new Date(rs.getTimestamp("attenbegintime").getTime())));
						sn.setIndex(rs.getInt("spreadindex"));
						sn.setContractCode(rs.getString("contractcode"));
						sn.setAttenEndTime((new Date(rs.getTimestamp("AttenEndTime").getTime())));
						sn.setDiffPricePeriod(rs.getInt("diffPricePeriod"));
						cnd.getNodeList().add(sn);
						reuterdaoLog.info("select t_spread结果:" + sn.toString());
						continueFlag = false;
					}
				}
				if (continueFlag) {
					ConfigNode cn = new ConfigNode();
					cn.setExchangeCode(rs.getString("exchangeCode"));
					cn.setQuoteSourceName(rs.getString("quoteSource"));
					Object consObject = null;
					try {
						consObject = Class.forName(rs.getString("spreadConsumer")).newInstance();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (consObject != null && consObject instanceof SpreadConsumer) {
						cn.setSpreadConsumer((SpreadConsumer) consObject);
					}
					List<SubNode> subNodeList = new ArrayList<SubNode>();
					SubNode sn = new SubNode();
					sn.setChangeContractTime(new Date(rs.getTimestamp("changecontracttime").getTime()));
					sn.setAttenBeginTime((new Date(rs.getTimestamp("attenbegintime").getTime())));
					sn.setIndex(rs.getInt("spreadindex"));
					sn.setContractCode(rs.getString("contractcode"));
					sn.setAttenEndTime((new Date(rs.getTimestamp("AttenEndTime").getTime())));
					sn.setDiffPricePeriod(rs.getInt("diffPricePeriod"));
					subNodeList.add(sn);
					cn.setNodeList(subNodeList);
					configList.add(cn);
				}
			}
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (Exception e) {
			reuterdaoLog.info(e.getMessage());
			e.printStackTrace();
		} finally {
			reuterdaoLog.info("select t_spread成功");
			return configList;
		}
	}
	
	
	/**
	 * 
	 * @param quoteSource
	 * @param flag 1:第一次查询价差表。2：第n（n>=2)次查询价差表 3：查询所有价差，建立定时任务
	 * @return
	 */
	public static List<SubNode> queryDiffPeriod(String quoteSource,Integer flag) {
		List<SubNode> result = new ArrayList<SubNode>();
		try {
			Connection con = DBConnUitl.INSTANCE.GetConnection();
			reuterdaoLog.info("连接数据库成功，开始select t_spread");
			String sql = "";
			if(flag == 1){
				sql	= "select a.exchangecode," +
			"       a.quotesource," + 
			"       a.spreadconsumer," + 
			"       a.spreadindex," + 
			"       a.contractcode," + 
			"       a.attenbegintime," + 
			"       a.changecontracttime," + 
			"       a.attenendtime," + 
			"       (a.attenendtime - a.attenbegintime) * 24 * 60 * 60 as diffPricePeriod" + 
			"  from t_spread a" + 
			" where a.quotesource = ? and systimestamp between a.attenbegintime+numtodsinterval(-30,'minute') and a.attenendtime";
			}else if(flag == 2){
				sql	= 
					"SELECT *\n" +
					"  FROM (SELECT TEMP.*, rownum RN\n" + 
					"          FROM (select a.exchangecode,\n" + 
					"                       a.quotesource,\n" + 
					"                       a.spreadconsumer,\n" + 
					"                       a.spreadindex,\n" + 
					"                       a.contractcode,\n" + 
					"                       a.attenbegintime,\n" + 
					"                       a.changecontracttime,\n" + 
					"                       a.attenendtime,\n" + 
					"                       (a.attenendtime - a.attenbegintime) * 24 * 60 * 60 as diffPricePeriod\n" + 
					"                  from t_spread a\n" + 
					"                 where a.quotesource = ?\n" + 
					"                   and systimestamp < a.attenbegintime\n" + 
					"                 order by a.spreadindex) TEMP) where rn = 1";
			}else if(flag == 3){
				sql	= 
					"SELECT *\n" +
					"  FROM (SELECT TEMP.*, rownum RN\n" + 
					"          FROM (select a.exchangecode,\n" + 
					"                       a.quotesource,\n" + 
					"                       a.spreadconsumer,\n" + 
					"                       a.spreadindex,\n" + 
					"                       a.contractcode,\n" + 
					"                       a.attenbegintime,\n" + 
					"                       a.changecontracttime,\n" + 
					"                       a.attenendtime,\n" + 
					"                       (a.attenendtime - a.attenbegintime) * 24 * 60 * 60 as diffPricePeriod\n" + 
					"                  from t_spread a\n" + 
					"                 where a.quotesource = ?\n" + 
					"                  order by a.spreadindex) TEMP)";
			}
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, quoteSource);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				SubNode sn = new SubNode();
				sn.setExchangeCode(rs.getString("ExchangeCode"));
				sn.setContractCode(rs.getString("contractcode"));
				sn.setDiffPricePeriod(rs.getInt("diffPricePeriod"));
				sn.setAttenBeginTime(rs.getTimestamp("AttenBeginTime"));
				sn.setAttenEndTime(rs.getTimestamp("AttenEndTime"));
				result.add(sn);
			}
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (Exception e) {
			reuterdaoLog.info(e.getMessage());
			e.printStackTrace();
		} finally {
			reuterdaoLog.info("select t_spread成功，关闭连接");
			return result;
		}
	}

	public static ConcurrentHashMap<String, Vector<ContractOffset>> queryT_contractoffset() {
		ConcurrentHashMap<String, Vector<ContractOffset>> result = new ConcurrentHashMap<String, Vector<ContractOffset>>();
		try {
			Connection con = DBConnUitl.INSTANCE.GetConnection();
			reuterdaoLog.info("连接数据库成功，开始select t_spread");
			String sql =
				"select a.offset,\n" +
				"       a.occurtime,\n" + 
				"       a.worldcommodityid,\n" + 
				"       a.switchdate,\n" + 
				"       a.oldcontract,\n" + 
				"       a.newcontract,\n" + 
				"       a.src\n" + 
				"  from t_contractoffset a inner join t_spread b\n" + 
				" on a.worldcommodityid = b.exchangecode\n" + 
				"   and a.occurtime between b.attenbegintime and b.attenendtime\n" + 
				"   and a.src = b.quotesource" +
				" and a.occurtime=(select max(occurtime) from t_contractoffset)";

			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ContractOffset ca = new ContractOffset();
				ca.setNewContract(rs.getString("NewContract"));
				ca.setOldContract(rs.getString("oldContract"));
				ca.setOffset(rs.getDouble("offset"));
				ca.setOccurtime(new Date(rs.getTimestamp("occurtime").getTime()));
				ca.setSrc(rs.getString("src"));
				ca.setSwitchDate(rs.getString("switchdate"));
				ca.setWorldCommodityId(rs.getString("worldcommodityid"));
				if (result.containsKey(ca.getWorldCommodityId())) {
					result.get(ca.getWorldCommodityId()).add(ca);
				} else {
					Vector vector = new Vector<ContractOffset>();
					vector.add(ca);
					result.put(ca.getWorldCommodityId(), vector);
				}
			}
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (Exception e) {
			reuterdaoLog.error("select t_spread失败",e);
			e.printStackTrace();
		}finally{
			reuterdaoLog.info("select t_spread成功，关闭连接");
		}
		return result;
	}
	
	
	/**
	 * 价差衰减情况
	 */
	public static void updateT_OFFSET(T_offset co) {
		/*不落库
		try {
			Connection con = DBConnUitl.INSTANCE.GetConnection();
			String sql = "insert into T_OFFSET (COMMODITYID, OFFSET, RECTIME, NOWTIME, DIFFTIME, DOWNSET,diffPricePeriod,src) values (?,?,?,?,?,?,?,?) ";
			reuterdaoLog.info("连接数据库成功，开始insert T_OFFSET");
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, co.getCommodityId());
			ps.setDouble(2, co.getOffset());
			ps.setTimestamp(3, new Timestamp(co.getRecTime().getTime()));
			ps.setTimestamp(4, new Timestamp(co.getNowTime().getTime()));
			ps.setLong(5, co.getDiffTime());
			ps.setDouble(6, co.getDownset());
			ps.setInt(7, co.getDiffPricePeriod());
			ps.setString(8, co.getSrc());
			ps.executeUpdate();
			if (ps != null) {
				ps.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			reuterdaoLog.info(e.getMessage());
			e.printStackTrace();
		} finally{
			reuterdaoLog.info("insert T_OFFSET成功");
		}*/
	}
}
