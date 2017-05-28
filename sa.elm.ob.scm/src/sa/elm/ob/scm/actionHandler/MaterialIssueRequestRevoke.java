package sa.elm.ob.scm.actionHandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian
 * 
 */

public class MaterialIssueRequestRevoke implements Process {
  private static final Logger log = Logger.getLogger(MaterialIssueRequestRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    Connection connection = null;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    final String Material_ID = (String) bundle.getParams().get("Escm_Material_Request_ID")
        .toString();
    MaterialIssueRequest Mrequest = OBDal.getInstance()
        .get(MaterialIssueRequest.class, Material_ID);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = Mrequest.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();

    MaterialIssueRequest headerId = null;
    String appstatus = "", alertWindow = AlertWindow.IssueRequest;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    String Description = "", lastWaitingRoleId = "";
    try {
    OBContext.setAdminMode();

      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      OBQuery<MaterialIssueRequestHistory> history = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          " as e where e.escmMaterialRequest.id='" + Material_ID
              + "' order by e.creationDate desc ");
      history.setMaxResult(1);
      if (history.list().size() > 0) {
        MaterialIssueRequestHistory apphistory = history.list().get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      MaterialIssueRequest headerCheck = OBDal.getInstance().get(MaterialIssueRequest.class,
          Material_ID);

      if (headerCheck.getAlertStatus().equals("ESCM_TR")
          || headerCheck.getAlertStatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {
        MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
            Material_ID);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("DR");
        if (header.isSiteissuereq()) {
          header.setEscmSmirAction("CO");

        } else {
          header.setEscmAction("CO");
        }
        // task 4813 #14218
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);
        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", headerId.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
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
              "as e where e.referenceSearchKey='" + Material_ID + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              lastWaitingRoleId = objAlert.getRole().getId();
              OBDal.getInstance().save(objAlert);
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
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }
          NextRoleByRuleVO nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(OBDal
              .getInstance().getConnection(), clientId, orgId, roleId, userId, Mrequest
              .getEscmDocumenttype(), header.getRole().getId());
          EutNextRole nextRole = null;
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

          // set alert for next approver
          if (header.isSiteissuereq()) {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.revoked", Lang)
                + " " + header.getCreatedBy().getName();
          } else {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.revoked", Lang)
                + " " + header.getCreatedBy().getName();
          }
          // set revoke alert to last waiting role
          AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
              lastWaitingRoleId, "", header.getClient().getId(), Description, "NEW", alertWindow);
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility
                .alertInsertionRole(header.getId(), header.getDocumentNo(), objNextRoleLine
                    .getRole().getId(), "", header.getClient().getId(), Description, "NEW",
                    alertWindow);

            obError.setType("Success");
            obError.setTitle("Success");
            obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke"));
          }

          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        } else if (!errorFlag) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage("");
        }
        bundle.setResult(obError);

      }
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Material Issue Request Revoke :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
