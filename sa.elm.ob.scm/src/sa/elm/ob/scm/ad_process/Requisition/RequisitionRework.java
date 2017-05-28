package sa.elm.ob.scm.ad_process.Requisition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 15/02/2017
 */
public class RequisitionRework implements Process {
  /**
   * This servlet class is responsible for Requisition Reject Process.
   * 
   */
  private static final Logger log = Logger.getLogger(RequisitionRework.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("rework the Requisition");
    @SuppressWarnings("unused")
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objRequisition.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("notes").toString();
    String headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseRequisition;
    ;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    String Lang = vars.getLanguage();
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;

    log.debug("Requisition Id:" + strRequisitionId);

    if (objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Escm_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }

    try {
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          if (objRequisition.getEscmMaterialRequest() == null) {
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
              String sql = "";
              sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
                  + CurrentDate
                  + "' and to_date >='"
                  + CurrentDate
                  + "' and document_type='EUT_111'";
              st = conn.prepareStatement(sql);
              rs = st.executeQuery();
              if (rs.next()) {
                String roleid = rs.getString("ad_role_id");
                if (roleid.equals(roleId)) {
                  allowDelegation = true;
                }
              }
            }
            if (allowUpdate || allowDelegation) {
              objRequisition.setUpdated(new java.util.Date());
              objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
              objRequisition.setEscmDocStatus("DR");
              objRequisition.setEscmDocaction("CO");
              objRequisition.setEutNextRole(null);
              OBDal.getInstance().save(objRequisition);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.PURCHASE_REQUISITION);
              headerId = objRequisition.getId();
              if (!StringUtils.isEmpty(objRequisition.getId())) {
                appstatus = "REJ";

                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId);
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(objRequisition.getId())) {
                Role objCreatedRole = null;
                if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {
                  objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0)
                      .getRole();
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
                  AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
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

                // check current role exists in document rule ,if it is not there then delete Delete
                // it
                // why ??? current user only already approved
                String checkQuery = "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '"
                    + vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";

                OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(
                    Requisition.class, checkQuery);
                if (checkRecipientQry.list().size() == 0) {
                  OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance().createQuery(
                      AlertRecipient.class,
                      "as e where e.alertRule.id='" + alertRuleId + "' and e.role.id='"
                          + vars.getRole() + "'");
                  if (currentRoleQuery.list().size() > 0) {
                    for (AlertRecipient delObject : currentRoleQuery.list()) {
                      OBDal.getInstance().remove(delObject);
                    }
                  }
                }
                // set alert for requester
                String Description = "Purchase Requisition was Rejected";
                AlertUtility.alertInsertionRole(objRequisition.getId(),
                    objRequisition.getDocumentNo(), "", objRequisition.getCreatedBy().getId(),
                    objRequisition.getClient().getId(), Description, "NEW", alertWindow);

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@ESCM_Requisition_Rework@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
              throw new OBException(errorMsg);
            }
          } else {

            OBQuery<AlertRule> queryAlertRule1 = OBDal.getInstance().createQuery(AlertRule.class,
                "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='PRFMIR'");
            if (queryAlertRule1.list().size() > 0) {
              AlertRule objRule = queryAlertRule1.list().get(0);
              alertRuleId = objRule.getId();
            }

            // upddate the header status as rejected
            objRequisition.setUpdated(new java.util.Date());
            objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
            objRequisition.setEscmDocStatus("ESCM_REJ");
            objRequisition.setEscmDocaction("PD");
            objRequisition.setEutNextRole(null);
            OBDal.getInstance().save(objRequisition);
            OBDal.getInstance().flush();

            Role objCreatedRole = null;
            if (objRequisition.getEscmMaterialRequest().getCreatedBy().getADUserRolesList().size() > 0) {
              objCreatedRole = objRequisition.getEscmMaterialRequest().getCreatedBy()
                  .getADUserRolesList().get(0).getRole();
            }
            includeRecipient.add(objCreatedRole.getId());
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

            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }
            String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                "scm.createPR.for.MIR.rej", Lang)
                + " "
                + OBContext.getOBContext().getUser().getName();

            AlertUtility.alertInsertionRole(objRequisition.getId(), objRequisition.getDocumentNo(),
                objCreatedRole.getId(), "", objRequisition.getClient().getId(), Description, "NEW",
                AlertWindow.PurchaseReqForMIR);

            HashSet<String> incluedSet1 = new HashSet<String>(includeRecipient);
            Iterator<String> iterator1 = incluedSet1.iterator();
            while (iterator1.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator1.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }

            headerId = objRequisition.getId();
            if (!StringUtils.isEmpty(objRequisition.getId())) {
              appstatus = "REJ";

              JSONObject historyData = new JSONObject();

              historyData.put("ClientId", clientId);
              historyData.put("OrgId", orgId);
              historyData.put("RoleId", roleId);
              historyData.put("UserId", userId);
              historyData.put("HeaderId", headerId);
              historyData.put("Comments", comments);
              historyData.put("Status", appstatus);
              historyData.put("NextApprover", "");
              historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
              historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
              historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

              count = Utility.InsertApprovalHistory(historyData);
            }

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_Requisition_Rework@");
            bundle.setResult(result);
            return;

          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);

          OBDal.getInstance().rollbackAndClose();

        }
      }

      else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }

      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception in Requisition rework:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
