package sa.elm.ob.scm.actionHandler;

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
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class ReturnTransactionReject implements Process {
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(ReturnTransactionReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    try {

      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();

    ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = inout.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    ShipmentInOut headerId = null;
    String appstatus = "", alertWindow = AlertWindow.ReturnTransaction;
    ArrayList<String> includeRecipient = new ArrayList<String>();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;

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

          OBQuery<ShipmentInOutLine> lines = OBDal.getInstance().createQuery(
              ShipmentInOutLine.class, "shipmentReceipt.id ='" + receiptId + "'");
          count = lines.list().size();

          if (count > 0) {
            ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
            if (header.getEutNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEutNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }

            if (allowUpdate) {
              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setEscmDocaction("CO");
              header.setEscmDocstatus("DR");
              header.setEutNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.Return_Transaction);
              headerId = header;
              if (!StringUtils.isEmpty(headerId.getId())) {
                appstatus = "REJ";

                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId.getId());
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.Return_Transaction_History);
                historyData.put("HeaderColumn", ApprovalTables.Return_Transaction_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.Return_Transaction_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {

                Role objCreatedRole = null;
                if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                  objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
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
                    "as e where e.referenceSearchKey='" + header.getId()
                        + "' and e.alertStatus='NEW'");

                if (alertQuery.list().size() > 0) {
                  for (Alert objAlert : alertQuery.list()) {
                    objAlert.setAlertStatus("SOLVED");
                  }
                }

                String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                    "scm.Returntrans.rejected", Lang) + " " + objUser.getName();

                boolean ex = AlertUtility.alertInsertionRole(header.getId(),
                    header.getDocumentNo(), "", header.getCreatedBy().getId(), header.getClient()
                        .getId(), Description, "NEW", alertWindow);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_ReturnrejectSuccess@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
              throw new OBException(errorMsg);
            }
          }
        } catch (Exception e) {
          log.error("exception :", e);
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);
          OBDal.getInstance().rollbackAndClose();
        }
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
