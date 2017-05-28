package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import sa.elm.ob.utility.util.UtilityDAO;

public class CopyPurchaseRequsitionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CopyPurchaseRequsitionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = new JSONObject();

    try {
      OBContext.setAdminMode();
      JSONObject jsondata = new JSONObject(content);
      String requisitionId = (String) parameters.get("requisitionId");// jsondata.getString("requisitionId");
      String userId = (String) parameters.get("userId");// jsondata.getString("userId");
      String clientId = (String) parameters.get("clientId");// jsondata.getString("clientId");
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

      // chk user have department
      User user = OBDal.getInstance().get(User.class, userId);
      if (user.getEscmSalesregion() == null) {
        JSONObject json1 = new JSONObject();
        json1.put("message", "ESCM_PurReq_UsrNotDept");
        return json1;
      }
      Boolean ispreference = Preferences.existsPreference("ESCM_ProcurementDirector", true, null,
          null, user.getId(), null, null);
      Requisition origrequisition = OBDal.getInstance().get(Requisition.class, requisitionId);
      Requisition copyrequisition = (Requisition) DalUtil.copy(origrequisition, false);
      copyrequisition.setDocumentStatus("DR");
      copyrequisition.setEscmDocStatus("DR");
      copyrequisition.setDocumentAction("CO");
      copyrequisition.setEscmDocaction("CO");
      String seqno = UtilityDAO.getSequenceNo(OBDal.getInstance().getConnection(), clientId,
          "DocumentNo_M_Requisition", true);
      copyrequisition.setDocumentNo(seqno);
      copyrequisition.setCreationDate(new java.util.Date());
      copyrequisition.setCreatedBy(user);
      copyrequisition.setUpdatedBy(user);
      copyrequisition.setUserContact(user);
      copyrequisition.setUpdated(new java.util.Date());
      copyrequisition.setEscmSalesregion(user.getEscmSalesregion());
      copyrequisition.setEutNextRole(null);
      if (!ispreference)
        copyrequisition.setEscmProcesstype(null);
      OBDal.getInstance().save(copyrequisition);

      log.debug("getProcurementRequisitionLineList:"
          + origrequisition.getProcurementRequisitionLineList().size());
      log.debug("copyrequisition:" + copyrequisition.getProcurementRequisitionLineList().size());
      if (origrequisition.getProcurementRequisitionLineList().size() > 0) {
        for (RequisitionLine line : origrequisition.getProcurementRequisitionLineList()) {
          RequisitionLine copyLines = (RequisitionLine) DalUtil.copy(line);
          copyLines.setRequisition(copyrequisition);
          copyLines.setCreationDate(new java.util.Date());
          copyLines.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdated(new java.util.Date());
          OBDal.getInstance().save(copyLines);
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(copyrequisition);
      json = jsonConverter.toJsonObject(copyrequisition, DataResolvingMode.FULL);
      OBDal.getInstance().commitAndClose();
      // json.put("message", "Success");
      return json;

    } catch (Exception e) {
      log.error("Exception in CopyPurchaseRequsitionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
