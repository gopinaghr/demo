package sa.elm.ob.scm.ad_reports.custodybarcodelabel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class CustodyBarcodeLabelDAO {
  private static Logger log4j = Logger.getLogger(CustodyBarcodeLabelDAO.class);
  private Connection connection = null;

  public CustodyBarcodeLabelDAO(Connection conn) {
    this.connection = conn;
  }

  /*
   * public JSONObject getMIRNos(String strOrgId) { JSONArray mirNos= new JSONArray(); JSONObject
   * mirNoObj = new JSONObject(), issueRequest; try { OBContext.setAdminMode();
   * OBQuery<MaterialIssueRequest> requestQuery =
   * OBDal.getInstance().createQuery(MaterialIssueRequest.class,
   * " where organization.id='"+strOrgId+
   * "' and alertStatus in ('ESCM_TR', 'ESCM_AP') and beneficiaryType in ('S', 'D', 'E') order by documentNo desc"
   * ); if(requestQuery!=null){ List<MaterialIssueRequest> requestList = requestQuery.list();
   * if(requestList.size() > 0){ for( MaterialIssueRequest mir : requestList){ issueRequest = new
   * JSONObject();
   * 
   * issueRequest.put("Id", mir.getId()); issueRequest.put("MIRNo", mir.getDocumentNo());
   * issueRequest.put("Description", mir.getDescription()==null?"":mir.getDescription());
   * issueRequest.put("Beneficiary",
   * mir.getBeneficiaryIDName()==null?"":mir.getBeneficiaryIDName().getCommercialName());
   * mirNos.put(issueRequest); } } } mirNoObj.put("RequestNos", mirNos); } catch (Exception e) {
   * log4j.error("Exception while getMIRNosL "+e); e.printStackTrace(); } finally{
   * OBContext.restorePreviousMode(); } return mirNoObj; }
   */

  public synchronized JSONObject getMIRNos(String strOrgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();

      countQuery.append(" select count(mreq.documentno) as count  ");
      selectQuery
          .append(" select mreq.escm_material_request_id, mreq.documentno, mreq.description, benf.name as beneficiary ");
      fromQuery
          .append(" from escm_material_request mreq left join escm_beneficiary_v benf on benf.escm_beneficiary_v_id=mreq.beneficiary_name "
              + " where mreq.ad_org_id ='"
              + strOrgId
              + "' and mreq.status in ('ESCM_TR', 'ESCM_AP') and mreq.beneficiary_type in ('S', 'D', 'E') group by mreq.documentno, mreq.escm_material_request_id, benf.name order by mreq.documentno desc ");

      st = connection.prepareStatement(countQuery.append(fromQuery).toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = connection.prepareStatement((selectQuery.append(fromQuery)).toString());
        log4j.debug("mirls:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();

          jsonData.put("Id", rs.getString("escm_material_request_id"));
          jsonData.put("MIRNo", rs.getString("documentno"));
          jsonData.put("Description",
              rs.getString("description") == null ? "" : rs.getString("description"));
          jsonData.put("Beneficiary",
              rs.getString("beneficiary") == null ? "" : rs.getString("beneficiary"));

          jsonArray.put(jsonData);
        }
      }
      jsob.put("RequestNos", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getMIRNos :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getBeneficiaryTypeList() {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();

      countQuery.append(" select count(name) as count  ");
      selectQuery.append(" select value, name ");
      fromQuery
          .append(" from ad_ref_list  where ad_reference_id ='E585F9EEA3024736B3E30F9F6A7C9A09' and value not in ('MA') and isactive='Y' ");

      /*
       * if (searchTerm != null && !searchTerm.equals("")) fromQuery.append(" and name ilike '%" +
       * searchTerm.toLowerCase() + "%'");
       */

      st = connection.prepareStatement(countQuery.append(fromQuery).toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = connection.prepareStatement((selectQuery.append(fromQuery)).toString());
        log4j.debug("benftypelist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("benftypevalue", rs.getString("value"));
          jsonData.put("benftypename", rs.getString("name"));
          jsonArray.put(jsonData);
        }
      }
      jsob.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getBeneficiaryType :", e);
      return jsob;
    }
    return jsob;
  }

  public synchronized JSONObject getTagsList(String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(), fromQuery = new StringBuilder();

      countQuery.append(" select count(documentno) as count  ");
      selectQuery
          .append(" select escm_mrequest_custody_id, documentno, case when status='LD' then 'Lost and Damaged' when status='SA' then 'Sale' "
              + " when status='RW' then 'Reward' when status='IU' then 'In Use' when status='N' then 'Gen Tag' when status='RET' then 'Returned' "
              + " when status='OB' then 'Obsolete' when status='RI' then 'ReIssued' when status='MA' then 'Maintenance' else status end as status, description  ");
      fromQuery
          .append(" from escm_mrequest_custody mcus where ad_org_id in ( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ?) group by mcus.documentno, mcus.escm_mrequest_custody_id order by documentno desc ");

      /*
       * if (searchTerm != null && !searchTerm.equals("")) fromQuery.append(" and name ilike '%" +
       * searchTerm.toLowerCase() + "%'");
       */

      st = connection.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, roleId);
      log4j.debug(st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = connection.prepareStatement((selectQuery.append(fromQuery)).toString());
        st.setString(1, roleId);
        log4j.debug("taglist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("TagId", rs.getString("escm_mrequest_custody_id"));
          jsonData.put("DocumentNo", rs.getString("documentno"));
          jsonData.put("Status", rs.getString("status"));
          jsonData.put("Description",
              rs.getString("description") == null ? "" : rs.getString("description"));

          jsonArray.put(jsonData);
        }
      }
      jsob.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getTagList :", e);
      return jsob;
    }
    return jsob;
  }

  public JSONObject getBenfDetails(String mirId) {
    final JSONObject benfObj = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      String sql = "select beneficiary_type, beneficiary_name from escm_material_request  where escm_material_request_id  = ? ";

      st = connection.prepareStatement(sql);
      st.setString(1, mirId);
      rs = st.executeQuery();
      if (rs.next()) {
        benfObj.put("BenfType", rs.getString("beneficiary_type"));
        benfObj.put("BenfName", rs.getString("beneficiary_name"));
      }
    } catch (Exception e) {
      e.printStackTrace();
      return benfObj;
    }
    return benfObj;
  }
}
