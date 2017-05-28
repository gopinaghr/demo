package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;

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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 10/05/2017
 */
public class CustodyTransferReject implements Process {

  public static Logger log = Logger.getLogger(CustodyTransferReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus();
      Connection con = OBDal.getInstance().getConnection();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString(), sql = "";
      String pendingapproval = "", appstatus = "";
      String DocAction = inout.getEscmCtdocaction();
      String alertWindow = AlertWindow.CustodyTransfer, alertRuleId = "";
      String Lang = vars.getLanguage();
      boolean errorFlag = false;
      User usr = OBDal.getInstance().get(User.class, userId);
      int count = 0;
      if (DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // update header status
      if (!errorFlag) {
        inout.setUpdated(new java.util.Date());
        inout.setUpdatedBy(OBContext.getOBContext().getUser());
        inout.setEscmCtdocaction("CO");
        inout.setEscmDocstatus("DR");
        inout.setEutNextRole(null);
        log.debug("header:" + inout.toString());
        OBDal.getInstance().save(inout);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.CUSTODY_TRANSFER);
        // Insert into Approval History
        if (!StringUtils.isEmpty(inout.getId())) {
          appstatus = "REJ";

          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", inout.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);

        }
        // alert Process
        if (count > 0 && !StringUtils.isEmpty(inout.getId())) {
          ArrayList<CustodyCardReportVO> includereceipient = new ArrayList<CustodyCardReportVO>();
          CustodyCardReportVO vo = null;
          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(
              AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }
          // get creater role
          Role objCreatedRole = null;
          if (inout.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = inout.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
              AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              vo = new CustodyCardReportVO();
              vo.setRoleId(objAlertReceipient.getRole().getId());
              if (objAlertReceipient.getUserContact() != null)
                vo.setUserId(objAlertReceipient.getUserContact().getId());
              includereceipient.add(vo);
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          // added created user also in alert receipient
          vo = new CustodyCardReportVO();
          vo.setRoleId(objCreatedRole.getId());
          vo.setUserId(inout.getCreatedBy().getId());
          includereceipient.add(vo);
          // avoid duplicate recipient
          HashSet<CustodyCardReportVO> incluedSet = new HashSet<CustodyCardReportVO>(
              includereceipient);
          for (CustodyCardReportVO vo1 : incluedSet) {
            AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                alertWindow);
          }

          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + inout.getId() + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
            }
          }

          // set alert for requester
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.rejected",
              Lang) + " " + usr.getName();
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), "", inout
              .getCreatedBy().getId(), inout.getClient().getId(), Description, "NEW", alertWindow);
        }
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;

      }

    } catch (OBException e) {
      log.debug("Exception in custody transfer Reject :" + e);

    } finally {
      OBContext.restorePreviousMode();
    }

  }
}