package sa.elm.ob.scm.ad_process.Requisition;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 13/02/2017
 */

public class RequisitionSubmit extends DalBaseProcess {

  /**
   * This servlet class was responsible for Requisition Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(RequisitionSubmit.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn1 = OBDal.getInstance().getConnection();
    ConnectionProvider conn = bundle.getConnection();
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;

    log.debug("entering into Requisition Submit");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);
      String DocStatus = objRequisition.getEscmDocStatus();
      String DocAction = objRequisition.getEscmDocaction(), p_instance_id = null, sql = "";
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequisition.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Date currentDate = new Date();
      String comments = (String) bundle.getParams().get("notes").toString();
      PreparedStatement ps = null;
      ResultSet rs = null;
      Boolean allowDelegation = false, chkRoleIsInDocRul = false;
      int count = 0;
      // check lines to submit
      if (objRequisition.getProcurementRequisitionLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_No_Requisition_Lines@");
        bundle.setResult(result);
        return;
      }
      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_RJD")) {
        if (objRequisition.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objRequisition.getEutNextRole()
              .getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objRequisition.getEutNextRole() != null) {
          sql = "";
          Connection con = OBDal.getInstance().getConnection();
          PreparedStatement st = null;
          ResultSet rs1 = null;
          sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
              + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_111'";
          st = con.prepareStatement(sql);
          rs1 = st.executeQuery();
          while (rs1.next()) {
            String roleid = rs1.getString("ad_role_id");
            if (roleid.equals(roleId)) {
              allowDelegation = true;
              break;
            }
          }
        }
        if (!allowUpdate && !allowDelegation) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(objRequisition.getCreatedBy().getId()))
          && DocStatus.equals("ESCM_RJD")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // throw an error in case if approver try to approving the record while the submit User is
      // already revoked the record
      if ((!vars.getUser().equals(objRequisition.getCreatedBy().getId())) && DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // check role is present in document rule or not
      if (objRequisition.getDocumentStatus().equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.PURCHASE_REQUISITION, BigDecimal.ZERO);
        log.debug("chkRoleIsInDocRul:" + chkRoleIsInDocRul);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;

          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
      }
      // check mandatory field for budget manager
      String preferenceValue = "";
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
            vars.getClient(), objRequisition.getOrganization().getId(), vars.getUser(),
            vars.getRole(), "800092");
      } catch (PropertyException e) {
      }
      if (preferenceValue.equals("Y")) {
        if (objRequisition.getEscmManualEncumNo() == null
            || objRequisition.getEscmManualEncumNo().equals("")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_ManualencumNo@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
        OBQuery<RequisitionLine> lines = OBDal.getInstance().createQuery(RequisitionLine.class,
            "requisition.id = '" + objRequisition.getId() + "'");
        if (lines.list() != null && lines.list().size() > 0) {
          for (RequisitionLine RLines : lines.list()) {
            if (RLines.getEscmAccountnoAmt() == null || RLines.getEscmAccountnoAmt().equals("")) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_AccountNoAmt@");// ESCM_RoleIsNotIncInDocRule
              bundle.setResult(result);
              return;
            }
          }
        }
      }

      if (!errorFlag) {
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn1, clientId, orgId, roleId, userId, objRequisition,
            appstatus, comments, currentDate, vars);
        log.debug("count:" + count);
        if (count == 2) {
          log.debug("entering into instance Id:");
          p_instance_id = SequenceIdData.getUUID();
          String error = "", s = "";

          log.debug("p_instance_id:" + p_instance_id);
          sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
              + "  VALUES ('"
              + p_instance_id
              + "', '1004400003','"
              + objRequisition.getId()
              + "', 'Y','"
              + userId
              + "','"
              + clientId
              + "','"
              + orgId
              + "', now(),'"
              + userId
              + "', now(),'" + userId + "','Y')";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps:" + ps.toString());
          count = ps.executeUpdate();
          log.debug("count:" + count);

          String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
          PreparedStatement pr = conn.getPreparedStatement(instanceqry);
          pr.setString(1, p_instance_id);
          ResultSet set = pr.executeQuery();

          if (set.next()) {

            sql = " select * from  m_requisition_post(?)";
            ps = conn.getPreparedStatement(sql);
            ps.setString(1, p_instance_id);
            // ps.setString(2, invoice.getId());
            ps.executeQuery();

            log.debug("count12:" + set.getString("ad_pinstance_id"));

            sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='"
                + p_instance_id + "'";
            ps = conn.getPreparedStatement(sql);
            log.debug("ps12:" + ps.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
              log.debug("result:" + rs.getString("result"));

              if (rs.getString("result").equals("0")) {
                error = rs.getString("errormsg").replace("@ERROR=", "");
                log.debug("error:" + error);
                s = error;
                int start = s.indexOf("@");
                int end = s.lastIndexOf("@");

                if (log.isDebugEnabled()) {
                  log.debug("start:" + start);
                  log.debug("end:" + end);
                }

                if (end != 0) {
                  sql = " select  msgtext from ad_message where value ='"
                      + s.substring(start + 1, end) + "'";
                  ps = conn.getPreparedStatement(sql);
                  log.debug("ps12:" + ps.toString());
                  rs = ps.executeQuery();
                  if (rs.next()) {
                    if (rs.getString("msgtext") != null)
                      throw new OBException(error);
                  }
                }
              } else if (rs.getString("result").equals("1")) {

              }
            }
          }
        }
        if (count > 0) {
          OBError result = OBErrorBuilder
              .buildMessage(null, "success", "@Escm_Requisition_Submit@");
          bundle.setResult(result);
          return;
        } else {
          errorFlag = false;
        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed");
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(objRequisition);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.PURCHASE_REQUISITION);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exeception in Requisition Submit:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, Requisition objRequisition, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.PurchaseRequisition;
    try {
      OBContext.setAdminMode(true);

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      BigDecimal requsitionamt = BigDecimal.ZERO;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApproval(objRequisition.getId(), roleId);
      log.debug("chkDirectApproval" + isDirectApproval);

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      if (objRequisition.getProcurementRequisitionLineList().size() > 0) {
        for (RequisitionLine a : objRequisition.getProcurementRequisitionLineList()) {
          requsitionamt = requsitionamt.add(a.getLineNetAmount());
        }
      }
      if ((objRequisition.getEutNextRole() == null)) {
        nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
            orgId, roleId, userId, Resource.PURCHASE_REQUISITION, requsitionamt);
      } else {
        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              orgId, roleId, userId, Resource.PURCHASE_REQUISITION, requsitionamt);

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(OBDal.getInstance()
                    .getConnection(), clientId, orgId, userRole.list().get(0).getUserContact()
                    .getId(), Resource.PURCHASE_REQUISITION, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(OBDal.getInstance()
                    .getConnection(), clientId, orgId, delegatedFromRole, delegatedToRole, userId,
                    Resource.PURCHASE_REQUISITION, requsitionamt);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, userId, Resource.PURCHASE_REQUISITION,
                requsitionamt);
          }

        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, Resource.PURCHASE_REQUISITION, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, userId,
                Resource.PURCHASE_REQUISITION, requsitionamt);
        }
      }
      if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        objRequisition.setUpdated(new java.util.Date());
        objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequisition.setEscmDocStatus("ESCM_IP");
        objRequisition.setEutNextRole(nextRole);
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(
              Alert.class,
              "as e where e.referenceSearchKey='" + objRequisition.getId()
                  + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              OBDal.getInstance().remove(objAlert);
            }
          }
          String Description = "Purchase Requisition Waiting for Approval";
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequisition.getId(), objRequisition.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", objRequisition.getClient().getId(),
                Description, "NEW", alertWindow);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_111'");
            if (delegationln != null && delegationln.list().size() > 0) {
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL, delegationln
                    .list().get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());

          }
        }
        // existing Recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        objRequisition.setEscmDocaction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        log.debug("doc sts:" + objRequisition.getEscmDocStatus() + "action:"
            + objRequisition.getEscmDocaction());
        count = 1; // Waiting For Approval flow

      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequisition.setUpdated(new java.util.Date());
        objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequisition.setEscmDocStatus("ESCM_AP");
        OBQuery<RequisitionLine> lines = OBDal.getInstance().createQuery(RequisitionLine.class,
            "requisition.id='" + objRequisition.getId() + "'");
        for (RequisitionLine RLines : lines.list()) {
          RLines.setEscmStatus("ESCM_AP");
        }
        Role objCreatedRole = null;
        if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(
            Alert.class,
            "as e where e.referenceSearchKey='" + objRequisition.getId()
                + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            OBDal.getInstance().remove(objAlert);
          }
        }
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
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
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        // set alert for requester
        String Description = "Purchase Requisition was Approved";
        AlertUtility.alertInsertionRole(objRequisition.getId(), objRequisition.getDocumentNo(), "",
            objRequisition.getCreatedBy().getId(), objRequisition.getClient().getId(), Description,
            "NEW", alertWindow);
        objRequisition.setEutNextRole(null);
        objRequisition.setEscmDocaction("PD");
        count = 2; // Final Approval Flow
      }

      // check current role exists in document rule ,if it is not there then delete Delete it
      // why ??? current user only already approved
      String checkQuery = "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '"
          + vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";

      OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(Requisition.class,
          checkQuery);
      if (checkRecipientQry.list().size() == 0) {
        OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class,
            "as e where e.alertRule.id='" + alertRuleId + "' and e.role.id='" + vars.getRole()
                + "'");
        if (currentRoleQuery.list().size() > 0) {
          for (AlertRecipient delObject : currentRoleQuery.list()) {
            OBDal.getInstance().remove(delObject);
          }
        }
      }

      OBDal.getInstance().save(objRequisition);
      requistionId = objRequisition.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();

        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

        /*
         * insertRequistionApprover(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
         * userId, requistionId, comments, appstatus, pendingapproval);
         */
      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.PURCHASE_REQUISITION);
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Requisition: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  @SuppressWarnings("unused")
  public boolean isDirectApproval(String RequsitionId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(*) from m_requisition req join eut_next_role rl on "
          + "req.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.m_requisition_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequsitionId);
        ps.setString(2, roleId);

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    }
  }
}
