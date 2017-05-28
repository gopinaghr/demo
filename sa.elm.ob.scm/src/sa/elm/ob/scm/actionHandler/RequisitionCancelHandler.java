package sa.elm.ob.scm.actionHandler;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class RequisitionCancelHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(RequisitionCancelHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      User user = OBDal.getInstance().get(User.class, vars.getUser());
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("m_requisitionline_id");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      String headerReason = jsonparams.getString("reason");
      String inpdocumentno = jsonRequest.getString("inpdocumentno");
      String clientId = vars.getClient();
      String alertWindow = AlertWindow.PurchaseRequisition;
      String appstatus = "";
      String LineNo = "";
      // find all
      int Count = 0;
      String HeaderId = "";
      if (selectedlines.length() > 0) {
        JSONObject selectedRowall = selectedlines.getJSONObject(0);
        HeaderId = selectedRowall.getString("requisition");
        OBQuery<RequisitionLine> Lines = OBDal.getInstance().createQuery(RequisitionLine.class,
            "requisition.id='" + HeaderId + "' and escmStatus='ESCM_AP'");
        if (Lines.list() != null) {
          Count = Lines.list().size();
        }

        Date currentDate = new Date();
        int a = 0;
        for (a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          String LineId = selectedRow.getString("id");
          String CancelReason = selectedRow.getString("escmCancelReason");
          if (!LineNo.equals(""))
            LineNo = LineNo + "," + selectedRow.getString("lineNo");
          else
            LineNo = selectedRow.getString("lineNo");
          RequisitionLine Line = OBDal.getInstance().get(RequisitionLine.class, LineId);
          Line.setEscmStatus("ESCM_CA");
          Line.setEscmCancelReason(CancelReason);
          Line.setEscmCancelDate(currentDate);
          Line.setEscmCancelledby(user);
          OBDal.getInstance().save(Line);
        }
        if (a != 0 || Count != 0) {
          if (a == Count) {
            Requisition Header = OBDal.getInstance().get(Requisition.class, HeaderId);
            Header.setEscmCancelDate(currentDate);
            Header.setEscmCancelledby(user);
            Header.setEscmCancelReason(headerReason);
            Header.setEscmDocStatus("ESCM_CA");
            Header.setDocumentStatus("ESCM_CA");
            OBDal.getInstance().save(Header);
          }
        }
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION);
        // alert
        // delete alert for approval alerts
        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + HeaderId + "' and e.alertStatus='NEW'"); if
         * (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
         * OBDal.getInstance().remove(objAlert); } }
         */
        // get Requisition Alert
        OBQuery<AlertRule> queryAlertRule = OBDal.getInstance()
            .createQuery(
                AlertRule.class,
                "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                    + "'");
        if (queryAlertRule.list().size() > 0) {
          AlertRule objRule = queryAlertRule.list().get(0);
          objRule.getId();
        }
        Requisition objRequisition = OBDal.getInstance().get(Requisition.class, HeaderId);

        String description = "";

        if (a == Count) {
          description = "Purchase Requisition Cancelled";
          appstatus = "CA";
          JSONObject historyData = new JSONObject();
          historyData.put("ClientId", clientId);
          historyData.put("OrgId", objRequisition.getOrganization().getId());
          historyData.put("RoleId", vars.getRole());
          historyData.put("UserId", vars.getUser());
          historyData.put("HeaderId", HeaderId);
          historyData.put("Comments", headerReason);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);
          Utility.InsertApprovalHistory(historyData);
        } else {
          description = "Purchase Requisition " + inpdocumentno + " Lines are Cancelled " + LineNo;
        }
        // set alert for Budget Controller
        AlertUtility.alertInsertionPreference(objRequisition.getId(), objRequisition
            .getDocumentNo(), "ESCM_BudgetControl", objRequisition.getClient().getId(),
            description, "NEW", alertWindow);
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_Requisition_Cancelled"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
