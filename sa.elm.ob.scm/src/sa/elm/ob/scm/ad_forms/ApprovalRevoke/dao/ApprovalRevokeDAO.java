package sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.ad_forms.ApprovalRevoke.vo.ApprovalRevokeVO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class ApprovalRevokeDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(ApprovalRevokeDAO.class);

  public ApprovalRevokeDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param clientId
   * @param windowId
   * @param searchFlag
   * @param vo
   * @return records waiting for approval records
   */
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int totalRecord = 0;
    String sqlQuery = "", fromClause = "", whereClause = "";

    try {
      whereClause = " where req.ad_client_id = '" + clientId + "'";
      whereClause += " and req.status='ESCM_IP' and req.eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";
      if (windowId.equals("MIR")) {
        whereClause += " and req.issiteissuereq <> 'Y'";
      } else if (windowId.equals("SIR")) {
        whereClause += " and req.issiteissuereq ='Y'";
      }
      // for other transaction empty rows
      else {
        whereClause += " and 1=3 ";
      }
      fromClause = " select count(*) as totalRecord from ( select req.escm_material_request_id  ,"
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
          + "  from escm_materialrequest_hist  history,ad_user ur,ad_ref_list st  "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
          + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
          + " where history.escm_material_request_id  = req.escm_material_request_id "
          + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
          + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction "
          + " from escm_material_request req "
          + " left join ad_user rr on rr.ad_user_id=req.createdby "
          + " left join ad_org org on org.ad_org_id=req.ad_org_id "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id "
          + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.eut_next_role_id ";
      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and req.status ='ESCM_IP'";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      sqlQuery += fromClause + whereClause + " ) main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      if (rs.next())
        totalRecord = rs.getInt("totalRecord");
    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      try {
        st.close();
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return totalRecord;
  }

  /**
   * 
   * @param clientId
   * @param windowId
   * @param vo
   * @param limit
   * @param offset
   * @param sortColName
   * @param sortColType
   * @param searchFlag
   * @return getRevokeRecordsList
   */
  public List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars, String clientId,
      String windowId, ApprovalRevokeVO vo, int limit, int offset, String sortColName,
      String sortColType, String searchFlag, String lang) {
    log4j.debug("sort" + sortColType);
    PreparedStatement st = null;
    ResultSet rs = null;
    List<ApprovalRevokeVO> ls = new ArrayList<ApprovalRevokeVO>();
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String sqlQuery = "", fromClause = "", whereClause = "", orderClause = "";
    OBContext.setAdminMode();

    try {
      whereClause = " where req.ad_client_id = '" + clientId + "'";

      whereClause += " and req.status='ESCM_IP' and req.eut_next_role_id is not null and req.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";
      if (windowId.equals("MIR")) {
        whereClause += " and req.issiteissuereq <> 'Y'";
      } else if (windowId.equals("SIR")) {
        whereClause += " and req.issiteissuereq ='Y'";
      }
      // for other transaction empty rows
      else {
        whereClause += " and 1=3 ";
      }

      fromClause = " select * from (select org.name as org ,req.documentno,req.status,req.escm_material_request_id as id ,rr.name as requester, "
          + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) "
          + "  from escm_materialrequest_hist  history,ad_user ur,ad_ref_list st  "
          + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and  "
          + " trl.ad_language=(select lang.ad_language from ad_language lang where lang.ad_language_id='112') "
          + " where history.escm_material_request_id  = req.escm_material_request_id "
          + " and st.value  =history.requestreqaction and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50'  "
          + " and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction,pen.pending,req.eut_next_role_id  "

          + " from escm_material_request req "
          + " left join ad_user rr on rr.ad_user_id=req.createdby "
          + " left join ad_org org on org.ad_org_id=req.ad_org_id "
          + " left join (select array_to_string(array_agg(role.name),' / ') as pending,lin.eut_next_role_id from eut_next_role rl "
          + " join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id "
          + " join ad_role role on role.ad_role_id=lin.ad_role_id "
          + " group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=req.eut_next_role_id ";

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and req.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and req.status ='ESCM_IP'";
          } else {
            whereClause += " and 1=2";
          }
        }

      }
      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno"))
        orderClause += " order by req.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status"))
        orderClause += " order by req.status " + sortColType + " limit " + limit + " offset "
            + offset;
      else
        orderClause += " order by req.documentno desc " + " limit " + limit + " offset " + offset;

      sqlQuery = fromClause + whereClause + orderClause + ") main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      if (sortColName != null && sortColName.equals("lastperformer"))
        sqlQuery += " order by main.lastaction  " + sortColType + " limit " + limit + " offset "
            + offset;

      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        String nextRole = "";
        ApprovalRevokeVO apVO = new ApprovalRevokeVO();
        apVO.setRecordId(Utility.nullToEmpty(rs.getString("id")));
        apVO.setOrgName(Utility.nullToEmpty(rs.getString("org")));
        apVO.setRequester(Utility.nullToEmpty(rs.getString("requester")));
        apVO.setDocno(Utility.nullToEmpty(rs.getString("documentno")));
        apVO.setLastperfomer(Utility.nullToEmpty(rs.getString("lastaction")));

        EutNextRole objNxtRole = OBDal.getInstance().get(EutNextRole.class,
            Utility.nullToEmpty(rs.getString("eut_next_role_id")));
        if (objNxtRole != null && objNxtRole.getEutNextRoleLineList().size() > 0) {
          if (objNxtRole.getEutNextRoleLineList().size() == 1) {
            OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                "role.id='" + objNxtRole.getEutNextRoleLineList().get(0).getRole().getId() + "'");
            if (ur != null && ur.list().size() > 0) {
              if (ur.list().size() == 1) {
                if (StringUtils.isEmpty(nextRole))
                  nextRole += (ur.list().get(0).getUserContact().getName());
                else
                  nextRole += (" / " + ur.list().get(0).getUserContact().getName());
              } else {
                if (StringUtils.isEmpty(nextRole))
                  nextRole += (ur.list().get(0).getRole().getName());
                else
                  nextRole += (" / " + ur.list().get(0).getRole().getName());
              }

            }
          } else {
            for (EutNextRoleLine objLine : objNxtRole.getEutNextRoleLineList()) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + objLine.getRole().getId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getUserContact().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getUserContact().getName());
                } else {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getRole().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getRole().getName());
                }

              }

            }
          }
        }
        apVO.setNextrole(nextRole);
        if (Utility.nullToEmpty(rs.getString("status")).equals("ESCM_IP")) {
          apVO.setStatus("In Progress");
        } else {
          apVO.setStatus("");
        }

        ls.add(apVO);
      }

    } catch (final SQLException e) {
      log4j.error("", e);
    } catch (final Exception e) {
      log4j.error("", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        st.close();
        rs.close();
      } catch (final SQLException e) {
        log4j.error("", e);
      }
    }
    return ls;
  }

  public String updateRevoke(VariablesSecureApp var, String selectIds, String inpWindowId) {
    String Result = "Success";
    String alertWindow = "", alertRuleId = "", appstatus = "", Description = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    try {
      if (inpWindowId.equals("MIR")) {
        alertWindow = AlertWindow.IssueRequest;
      } else if (inpWindowId.equals("SIR")) {
        alertWindow = AlertWindow.SiteIssueRequest;
      }
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(
          AlertRule.class,
          "as e where e.client.id='" + var.getClient() + "' and e.eSCMProcessType='" + alertWindow
              + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {

        MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
            result.get(i));

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("DR");
        if (header.isSiteissuereq()) {
          header.setEscmSmirAction("CO");

        } else {
          header.setEscmAction("CO");
        }
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);

        if (!StringUtils.isEmpty(header.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", var.getClient());
          historyData.put("OrgId", var.getOrg());
          historyData.put("RoleId", var.getRole());
          historyData.put("UserId", var.getUser());
          historyData.put("HeaderId", header.getId());
          historyData.put("Comments", "Mass Revoke");
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log4j.debug("headerId:" + header.getId());
        log4j.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (header.isSiteissuereq()) {
            alertWindow = AlertWindow.SiteIssueRequest;
          } else {
            alertWindow = AlertWindow.IssueRequest;
          }
          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              OBDal.getInstance().remove(objAlert);
            }
          }
          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
              AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          includeRecipient.add(objCreatedRole.getId());
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, var.getClient(), alertWindow);
          }
          NextRoleByRuleVO nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance()
              .getConnection(), var.getClient(), header.getOrganization().getId(), header.getRole()
              .getId(), var.getUser(), Resource.MATERIAL_ISSUE_REQUEST, header.getRole().getId());
          EutNextRole nextRole = null;
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
          // set alert for next approver
          if (header.isSiteissuereq()) {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.revoked",
                var.getLanguage())
                + " " + header.getCreatedBy().getName();
          } else {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.revoked",
                var.getLanguage())
                + " " + header.getCreatedBy().getName();
          }
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility
                .alertInsertionRole(header.getId(), header.getDocumentNo(), objNextRoleLine
                    .getRole().getId(), "", header.getClient().getId(), Description, "NEW",
                    alertWindow);
          }
          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();

        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log4j.error("", e);
      Result = "Error";
    }
    return Result;
  }

  public String validateRecord(String selectIds) {
    String ids = null;
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    for (int i = 0; i < result.size(); i++) {
      MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
          result.get(i));
      if (header.getAlertStatus().equals("DR") || header.getAlertStatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = header.getDocumentNo();
        } else {
          ids = ids + ", " + header.getDocumentNo();
        }
      }
    }
    return ids;
  }

}
