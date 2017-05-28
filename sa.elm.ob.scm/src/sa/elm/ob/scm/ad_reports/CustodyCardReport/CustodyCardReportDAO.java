package sa.elm.ob.scm.ad_reports.CustodyCardReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class CustodyCardReportDAO {

  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(CustodyCardReportDAO.class);

  public CustodyCardReportDAO(Connection con) {
    this.conn = con;
  }

  public synchronized JSONObject getBeneficiaryList(String clientId, String searchTerm,
      int pagelimit, int page, String type, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    List<CustodyCardReportVO> list = null;
    int totalRecords = 0;
    try {
      list = new ArrayList<CustodyCardReportVO>();
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();

      countQuery.append(" select count(distinct ben.escm_beneficiary_v_id) as count ");
      selectQuery.append(" select distinct ben.escm_beneficiary_v_id as benId, name as value ");
      fromQuery
          .append(" from escm_beneficiary_v ben  where btype= ? and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
              + "and ad_client_id=?     ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and ben.name ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, type);
      st.setString(2, roleId);
      st.setString(3, clientId);
      st.setString(4, clientId);
      log4j.debug("benfilist:" + st.toString());
      log4j.debug("qry>>" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement((selectQuery.append(fromQuery)).toString()
            + " order by value limit ? offset ? ");
        st.setString(1, type);
        st.setString(2, roleId);
        st.setString(3, clientId);
        st.setString(4, clientId);
        st.setInt(5, pagelimit);
        st.setInt(6, (page - 1) * pagelimit);
        log4j.debug("benfilist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("benId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getBeneficiaryList :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getDepartmentList(String clientId, String searchTerm,
      int pagelimit, int page, String type, String Org, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    List<CustodyCardReportVO> list = null;
    int totalRecords = 0;
    try {
      list = new ArrayList<CustodyCardReportVO>();
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();

      countQuery.append(" select count(distinct ben.escm_beneficiary_v_id) as count ");
      selectQuery.append(" select distinct ben.escm_beneficiary_v_id as benId, name as value ");
      fromQuery.append(" from escm_beneficiary_v ben  where btype= ? and ad_org_id = ? "
          + "and ad_client_id=?     ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and ben.name ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, type);
      st.setString(2, Org);
      st.setString(3, clientId);
      log4j.debug("Deptlist:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement((selectQuery.append(fromQuery)).toString()
            + " order by value limit ? offset ? ");
        st.setString(1, type);
        st.setString(2, Org);
        st.setString(3, clientId);
        st.setInt(4, pagelimit);
        st.setInt(5, (page - 1) * pagelimit);
        log4j.debug("Deptlist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("benId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getDepartmentList :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getProductList(String clientId, String searchTerm, int pagelimit,
      int page, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    List<CustodyCardReportVO> list = null;
    int totalRecords = 0;
    try {
      list = new ArrayList<CustodyCardReportVO>();
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();
      countQuery.append(" select count(distinct pro.m_product_id) as count ");
      selectQuery
          .append(" select distinct pro.m_product_id as prdId, (value||'-'||name)  as value ");
      fromQuery.append(" from m_product pro  where ad_client_id= ? and isactive='Y'  ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and pro.value ||' - '|| pro.name ilike '%" + searchTerm.toLowerCase()
            + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);
      log4j.debug("getProductList:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement((selectQuery.append(fromQuery)).toString()
            + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);
        log4j.debug("getProductList:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("prdId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getProductList :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getWarehouse(String clientId, String searchTerm, int pagelimit,
      int page, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    List<CustodyCardReportVO> list = null;
    int totalRecords = 0;
    try {
      list = new ArrayList<CustodyCardReportVO>();
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();
      countQuery.append(" select count(distinct m_warehouse_id) as count ");
      selectQuery.append(" select distinct m_warehouse_id as whId, name as value ");
      fromQuery
          .append(" from m_warehouse   where ad_client_id= ? and em_escm_warehouse_type  =  'MAW' and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )    and isactive='Y' ");
      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and name ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);
      st.setString(2, roleId);
      st.setString(3, clientId);
      log4j.debug("getWarehouse:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement((selectQuery.append(fromQuery)).toString()
            + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setString(2, roleId);
        st.setString(3, clientId);
        st.setInt(4, pagelimit);
        st.setInt(5, (page - 1) * pagelimit);
        log4j.debug("getProductList:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("whId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
        if (jsonArray.length() > 0)
          jsob.put("data", jsonArray);
        else
          jsob.put("data", "");

      }
    } catch (final Exception e) {
      log4j.error("Exception in getWarehouse :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getWarehousename(String clientId, String searchTerm,
      int pagelimit, int page, String type, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    List<CustodyCardReportVO> list = null;
    int totalRecords = 0;
    try {
      list = new ArrayList<CustodyCardReportVO>();
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();
      countQuery.append(" select count(distinct m_warehouse_id) as count ");
      selectQuery.append(" select distinct m_warehouse_id as whId, name as value ");
      fromQuery
          .append(" from m_warehouse   where ad_org_id= ? and ad_client_id= ? and em_escm_warehouse_type  =  'MAW' ");
      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and name ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, type);
      st.setString(2, clientId);

      log4j.debug("getWarehousename:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = conn.prepareStatement((selectQuery.append(fromQuery)).toString()
            + " order by value limit ? offset ? ");
        st.setString(1, type);
        st.setString(2, clientId);
        st.setInt(3, pagelimit);
        st.setInt(4, (page - 1) * pagelimit);
        log4j.debug("getProduct:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("whId"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
        if (jsonArray.length() > 0)
          jsob.put("data", jsonArray);
        else
          jsob.put("data", "");

      }
    } catch (final Exception e) {
      log4j.error("Exception in getWarehousename :", e);
      return jsob;
    }
    return jsob;
  }

}