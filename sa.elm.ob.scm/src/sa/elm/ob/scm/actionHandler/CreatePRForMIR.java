package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.properties.Resource;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Divya on 17/05/2017
 */

public class CreatePRForMIR extends BaseActionHandler {

  /**
   * This servlet class was responsible for Create PR For MRI missed qty
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(CreatePRForMIR.class);
  private final OBError obError = new OBError();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();

      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      Connection conn = OBDal.getInstance().getConnection();
      final String reqId = jsonRequest.getString("Escm_Material_Request_ID");
      MaterialIssueRequest request = OBDal.getInstance().get(MaterialIssueRequest.class, reqId);
      Requisition requisition = null;
      NextRoleByRuleVO nextApproval = null;
      String lang = vars.getLanguage();
      String pendingapproval = null, comments = "";
      String alertWindowType = AlertWindow.PurchaseReqForMIR, alertRuleId = "";
      Long line = (long) 10;
      EutNextRole nextRole = null;
      String Lang = vars.getLanguage();
      PreparedStatement st = null;
      ResultSet rs = null;
      String financialyear = null;
      Boolean chkRoleIsInDocRul = true, errorFlag = false;

      // chk department is associate with particular user

      User user = OBDal.getInstance().get(User.class, vars.getUser());
      if (user.getEscmSalesregion() == null) {
        errorFlag = true;
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_UsrNotDept"));
        json.put("message", successMessage);
        return json;
      }

      // get next approval
      nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
          vars.getClient(), request.getOrganization().getId(), vars.getRole(), vars.getUser(),
          sa.elm.ob.utility.properties.Resource.PURCHASE_REQUISITION, BigDecimal.ZERO);

      String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
          vars.getClient(), request.getOrganization().getId(), vars.getUser(), vars.getRole(),
          "D8BA0A87790B4B67A86A8DF714525736");
      if (preferenceValue.equals("Y")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            vars.getClient(), request.getOrganization().getId(), vars.getUser(), vars.getRole(),
            sa.elm.ob.utility.properties.Resource.PURCHASE_REQUISITION, BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
        }
      }
      if (!errorFlag) {
        if (selectedlines.length() > 0) {
          requisition = OBProvider.getInstance().get(Requisition.class);
          requisition.setClient(request.getClient());
          requisition.setOrganization(request.getOrganization());
          requisition.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          requisition.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          requisition.setCreationDate(new java.util.Date());
          requisition.setUpdated(new java.util.Date());
          requisition.setDocumentNo(UtilityDAO.getSequenceNo(conn, vars.getClient(),
              "DocumentNo_M_Requisition", true));
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            requisition.setEscmDocStatus("ESCM_IP");
            requisition.setDocumentStatus("ESCM_IP");
            requisition.setEutNextRole(nextRole);
            requisition.setEscmDocaction("AP");
            pendingapproval = nextApproval.getStatus();
          } else {
            requisition.setEutNextRole(null);
            requisition.setEscmDocaction("PD");
            requisition.setEscmDocStatus("ESCM_AP");
          }
          requisition.setEscmMaterialRequest(request);
          requisition.setUserContact(OBDal.getInstance().get(User.class, vars.getUser()));
          requisition.setDescription(Resource.getProperty("scm.MIR.createPR.description", lang));

          if (requisition.getUserContact().getEscmSalesregion() != null)
            requisition.setEscmSalesregion(requisition.getUserContact().getEscmSalesregion());

          // financial year
          st = conn
              .prepareStatement(" select y.description from c_period p  join c_year y on y.c_year_id = p.c_year_id "
                  + " where now() between startdate and enddate  and p.ad_client_id= ? ");
          st.setString(1, vars.getClient());
          rs = st.executeQuery();
          if (rs.next()) {
            financialyear = rs.getString("description");
          }
          requisition.setEscmFinancialYear(financialyear);
          requisition.setCurrency(requisition.getOrganization().getCurrency());
          OBDal.getInstance().save(requisition);
          for (int a = 0; a < selectedlines.length(); a++) {
            line += 10;
            JSONObject selectedRow = selectedlines.getJSONObject(a);
            RequisitionLine reqline = OBProvider.getInstance().get(RequisitionLine.class);
            reqline.setClient(request.getClient());
            reqline.setOrganization(request.getOrganization());
            reqline.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            reqline.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            reqline.setCreationDate(new java.util.Date());
            reqline.setUpdated(new java.util.Date());
            reqline.setRequisition(requisition);
            reqline.setLineNo(line);
            reqline.setProduct(OBDal.getInstance().get(Product.class,
                selectedRow.getString("product")));
            reqline.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
            reqline.setQuantity(new Long(selectedRow.getString("pendingQty")));
            reqline.setQuantity(new Long(selectedRow.getString("pendingQty")));
            reqline.setEscmMaterialReqln(OBDal.getInstance().get(MaterialIssueRequestLine.class,
                selectedRow.getString("id")));
            reqline.setNeedByDate(new java.util.Date());
            reqline.setUnitPrice(new BigDecimal(1));
            OBDal.getInstance().save(reqline);

            MaterialIssueRequestLine matreqline = OBDal.getInstance().get(
                MaterialIssueRequestLine.class, selectedRow.getString("id"));
            matreqline.setPendingQty(matreqline.getPendingQty().subtract(
                new BigDecimal(selectedRow.getString("pendingQty"))));

            OBDal.getInstance().save(matreqline);

          }

          if (!StringUtils.isEmpty(requisition.getId())) {
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", vars.getClient());
            historyData.put("OrgId", request.getOrganization().getId());
            historyData.put("RoleId", vars.getRole());
            historyData.put("UserId", vars.getUser());
            historyData.put("HeaderId", request.getId());
            comments = Resource.getProperty("scm.MIR.createPR.created", lang);
            comments = comments.replace("%", requisition.getDocumentNo());
            historyData.put("Comments", comments);
            historyData.put("Status", "");
            historyData.put("NextApprover", pendingapproval);
            historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

            historyData.put("ClientId", vars.getClient());
            historyData.put("OrgId", request.getOrganization().getId());
            historyData.put("RoleId", vars.getRole());
            historyData.put("UserId", vars.getUser());
            historyData.put("HeaderId", requisition.getId());
            comments = Resource.getProperty("scm.MIR.createPR.created.for.MIR", lang);
            comments = comments.replace("%", request.getDocumentNo());
            historyData.put("Comments", comments);
            historyData.put("Status", "");
            historyData.put("NextApprover", pendingapproval);
            historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);
          }
          if (nextApproval.getNextRoleId() == null) {
            requisition.setDocumentAction("CL");
            requisition.setProcessed(true);
            requisition.setDocumentStatus("CO");
            OBDal.getInstance().save(requisition);

          }
          // alert process
          ArrayList<String> includereceipient = new ArrayList<String>();

          // getting alert ruleId
          OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(
              AlertRule.class,
              " as e where e.client.id='" + vars.getClient() + "' and e.eSCMProcessType='"
                  + alertWindowType + "'");
          if (alertrule.list().size() > 0) {
            alertRuleId = alertrule.list().get(0).getId();
          }

          // set alerts for next roles
          if (nextApproval != null && nextApproval.hasApproval()
              && nextRole.getEutNextRoleLineList().size() > 0) {
            // delete alert for approval alerts
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(
                Alert.class,
                "as e where e.referenceSearchKey='" + requisition.getId()
                    + "' and e.alertStatus='NEW'");
            if (alertQuery.list().size() > 0) {
              for (Alert objAlert : alertQuery.list()) {
                objAlert.setAlertStatus("SOLVED");
              }
            }
            String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                "scm.createPR.for.MIR.wfa", Lang) + " " + requisition.getUserContact().getName();
            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              AlertUtility.alertInsertionRole(requisition.getId(), requisition.getDocumentNo(),
                  objNextRoleLine.getRole().getId(), "", requisition.getClient().getId(),
                  Description, "NEW", alertWindowType);
              // add next role recipient
              includereceipient.add(objNextRoleLine.getRole().getId());
            }
          }
          // alert for MIR preparer
          Role objCreatedRole = null;
          if (request.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = request.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          includereceipient.add(objCreatedRole.getId());

          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
              AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includereceipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includereceipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, requisition.getClient()
                .getId(), alertWindowType);
          }
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          String message = OBMessageUtils.messageBD("ESCM_CreatePRForMIR_Success");
          message = message.replace("%", requisition.getDocumentNo());
          if (pendingapproval != null) {
            message = message
                + " "
                + sa.elm.ob.scm.properties.Resource.getProperty(
                    "scm.createPR.for.MIR.submitted.to", Lang);
            message = message.replace("@", pendingapproval);
          }
          successMessage.put("text", message);
          json.put("message", successMessage);
          return json;
        } else {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
          json.put("message", successMessage);
          return json;
        }
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PRForMIR_RoleNotPre"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in CreatePRForMIR :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}