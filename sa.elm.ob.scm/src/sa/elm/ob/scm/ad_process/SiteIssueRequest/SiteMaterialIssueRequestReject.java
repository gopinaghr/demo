package sa.elm.ob.scm.ad_process.SiteIssueRequest;

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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 10/04/2017
 */
public class SiteMaterialIssueRequestReject implements Process {
  /**
   * This servlet class was responsible for Site Issue Request Reject Process with Approval
   * 
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(SiteMaterialIssueRequestReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Site Material issue request reject");
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
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
    String appstatus = "", alertWindow = AlertWindow.SiteIssueRequest;
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

    log.debug("Material_ID:" + Material_ID);

    if (Mrequest.getAlertStatus().equals("ESCM_AP")) {
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

          OBQuery<MaterialIssueRequestLine> lines = OBDal.getInstance().createQuery(
              MaterialIssueRequestLine.class, "escmMaterialRequest.id ='" + Material_ID + "'");
          count = lines.list().size();

          if (count > 0) {
            MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
                Material_ID);
            if (header.getEUTNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEUTNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (header.getEUTNextRole() != null) {
              String sql = "";
              sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
                  + CurrentDate
                  + "' and to_date >='"
                  + CurrentDate
                  + "' and document_type='EUT_112'";
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
              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setAlertStatus("DR");
              header.setEscmSmirAction("CO");
              // header.setSubmit(false);
              header.setEUTNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.MATERIAL_ISSUE_REQUEST);
              headerId = header;
              log.debug("headerId:" + headerId.getId());
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
                historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
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

                // check current role exists in document rule ,if it is not there then delete Delete
                // it
                // why ??? current user only already approved
                /*
                 * String checkQuery =
                 * "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '" +
                 * vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";
                 * 
                 * OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(
                 * Requisition.class, checkQuery); if (checkRecipientQry.list().size() == 0) {
                 * OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance().createQuery(
                 * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId +
                 * "' and e.role.id='" + vars.getRole() + "'"); if (currentRoleQuery.list().size() >
                 * 0) { for (AlertRecipient delObject : currentRoleQuery.list()) {
                 * OBDal.getInstance().remove(delObject); } } }
                 */
                // set alert for requester
                String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                    "scm.smir.rejected", Lang) + " " + objUser.getName();
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "", header
                    .getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                    alertWindow);

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_SMIRRejectSuccess@");
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
