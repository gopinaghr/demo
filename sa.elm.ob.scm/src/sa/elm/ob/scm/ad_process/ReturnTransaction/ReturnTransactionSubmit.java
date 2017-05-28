package sa.elm.ob.scm.ad_process.ReturnTransaction;

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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 */

public class ReturnTransactionSubmit extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ReturnTransactionSubmit.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;
    String productname = null;
    String Status = "";

    try {

      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus();
      String DocAction = inout.getEscmDocaction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      PreparedStatement st = null;
      Date currentDate = new Date();
      int count = 0;
      MaterialTransaction trans = null;
      String query = null;
      String status = null;
      String product = null;
      String errorMsg = "";

      Boolean chkRoleIsInDocRul, chkSubRolIsInFstRolofDR = false;
      String comments = (String) bundle.getParams().get("notes").toString();
      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        if (line.getEscmCustodyTransactionList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTran_LineQtyZero@");
          bundle.setResult(result);
          return;
        }
      }
      // checking for already the custody tag present in CT or RT in inprogress status
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
          status = null;
          OBQuery<Escm_custody_transaction> transaction = OBDal
              .getInstance()
              .createQuery(
                  Escm_custody_transaction.class,
                  " as e where e.goodsShipmentLine.id in ( "
                      + " select line.id  from MaterialMgmtShipmentInOutLine line  left join line.shipmentReceipt hd  where (hd.escmReceivingtype='INR'  or hd.escmIscustodyTransfer='Y') and hd.escmDocstatus='ESCM_IP' and hd.id <> '"
                      + inout.getId() + "') and e.escmMrequestCustody.id='"
                      + tran.getEscmMrequestCustody().getId() + "' ");
          if (transaction.list().size() > 0) {
            errorFlag = true;
            status = null;
            for (Escm_custody_transaction tra : transaction.list()) {
              if (status == null) {
                status = OBMessageUtils.messageBD("ESCM_CT_TagAlreadyUsed");
                status = status.replace("%", tra.getGoodsShipmentLine().getShipmentReceipt()
                    .getDocumentNo());
              } else {
                status += tra.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
              }
              if (product == null) {
                product = tra.getGoodsShipmentLine().getProduct().getName();
              } else {
                if (!product.equals(tra.getGoodsShipmentLine().getProduct().getName())) {
                  product = product + " ," + tra.getGoodsShipmentLine().getProduct().getName();
                }
              }
            }

          }
          tran.setErrorreason(status);

        }
      }
      if (errorFlag) {
        errorMsg = OBMessageUtils.messageBD("ESCM_ProcessFailed(Tag Status)").replace("%", product);
        throw new OBException(errorMsg);
      }
      // checking for beneficiary
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
          if (tran.getEscmMrequestCustody().getBeneficiaryType() != null
              && !tran.getEscmMrequestCustody().getBeneficiaryType().equals("MA")) {
            if (tran.getEscmMrequestCustody().getBeneficiaryIDName() != null
                || tran.getEscmMrequestCustody().getBeneficiaryType() != null)
              if (!tran.getEscmMrequestCustody().getBeneficiaryIDName()
                  .equals(line.getShipmentReceipt().getEscmBname())
                  || !tran.getEscmMrequestCustody().getBeneficiaryType()
                      .equals(line.getShipmentReceipt().getEscmBtype())) {
                errorFlag = true;
                status = OBMessageUtils.messageBD("ESCM_Notbelong");
                status = status.replace("%", inout.getEscmBname().getCommercialName());
                tran.setErrorreason(status);
              }
          }
        }
      }
      if (errorFlag) {
        errorMsg = OBMessageUtils.messageBD("ESCM_Not_Beneficiary").replace("@",
            inout.getEscmBname().getCommercialName());
        throw new OBException(errorMsg);

      }
      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.Return_Transaction, BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }
      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(OBDal.getInstance()
            .getConnection(), clientId, orgId, userId, roleId, Resource.Return_Transaction,
            BigDecimal.ZERO);
        if (!chkSubRolIsInFstRolofDR) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }
      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }
      /*
       * // Try to approve already completed record if (inout.getEscmReceivingtype().equals("INR"))
       * { for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) { for
       * (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) { if
       * (tran.getEscmMrequestCustody().getAlertStatus().equals("RET")) { errorFlag = true; break; }
       * } } } if (errorFlag) { OBDal.getInstance().rollbackAndClose(); OBError result =
       * OBErrorBuilder .buildMessage(null, "error", "@ESCM_IssRet_TagAlrProcessed@");
       * bundle.setResult(result); return; }
       */
      if ((!vars.getUser().equals(inout.getCreatedBy().getId())) && (DocStatus.equals("DR"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {
        if (DocStatus.equals("DR") && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, inout, appstatus,
            comments, currentDate, vars, nextApproval, Lang);
        if (count == 2) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else if (count == 1) {
          if (inout.getEscmSpecno() == null) {
            String sequence = Utility.getSpecificationSequence(inout.getOrganization().getId(),
                "INR");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              inout.setEscmSpecno(sequence);
            }
          }
          for (ShipmentInOutLine inoutline : inout.getMaterialMgmtShipmentInOutLineList()) {
            if (inout.getEscmReceivingtype().equals("INR")) {
              trans = OBProvider.getInstance().get(MaterialTransaction.class);
              trans.setOrganization(inoutline.getOrganization());
              trans.setClient(inoutline.getClient());
              trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setCreationDate(new java.util.Date());
              trans.setUpdated(new java.util.Date());
              if (inout.getEscmReceivingtype().equals("INR"))
                trans.setMovementType("V+");
              else if (inout.getEscmReceivingtype().equals("IRT"))
                trans.setMovementType("V-");

              OBQuery<Locator> locator = OBDal.getInstance().createQuery(
                  Locator.class,
                  " as e where e.warehouse.id='" + inout.getWarehouse().getId()
                      + "' and e.default='Y' ");
              locator.setMaxResult(1);
              if (locator.list().size() > 0) {
                trans.setStorageBin(locator.list().get(0));
                // log4j.debug("getStorageBin:" + trans.getStorageBin());
              } else {
                errorFlag = true;
                OBError result = OBErrorBuilder
                    .buildMessage(null, "error", "@ESCM_Locator(Empty)@");
                bundle.setResult(result);
                return;
              }

              trans.setProduct(inoutline.getProduct());
              trans.setMovementDate(inout.getMovementDate());
              if (inout.getEscmReceivingtype().equals("INR")) {
                trans.setMovementQuantity(inoutline.getMovementQuantity());
                trans.setEscmTransactiontype("INR");
              }
              trans.setGoodsShipmentLine(inoutline);
              trans.setUOM(OBDal.getInstance().get(UOM.class, inoutline.getUOM().getId()));

              OBDal.getInstance().save(trans);
            }
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              if (inout.getEscmReceivingtype().equals("INR"))
                objCustody.setAlertStatus("RET");
              else {
                objCustody.setBeneficiaryType(inout.getEscmBtype());
                objCustody.setBeneficiaryIDName(inout.getEscmBname());
              }
              OBDal.getInstance().save(objCustody);
              // update custody transaction status
              if (inout.getEscmReceivingtype().equals("INR"))
                objCustodytran.setTransactiontype("RE");

              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in ('" + objCustodytran.getId()
                  + "' )    and escm_mrequest_custody_id ='" + objCustody.getId()
                  + "' and isprocessed = 'Y' order by created desc limit 1";
              st = conn.prepareStatement(query);
              rs = st.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(inout.getMovementDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              OBDal.getInstance().save(objCustodytran);
            }
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exeception in Return Transaction Submit:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, ShipmentInOut objRequest, String appstatus, String comments, Date currentDate,
      VariablesSecureApp vars, NextRoleByRuleVO nextApproval, String Lang) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.ReturnTransaction;
    String strRoleId = "";
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApproval(objRequest.getId(), roleId);

      strRoleId = objRequest.getEscmAdRole().getId();
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      if ((objRequest.getEutNextRole() == null)) {
        nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
            clientId, orgId, roleId, userId, Resource.Return_Transaction, strRoleId);

      } else {
        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
              clientId, orgId, roleId, userId, Resource.Return_Transaction, strRoleId);
        }
      }
      if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setEscmDocaction("AP");
        objRequest.setEscmDocstatus("ESCM_IP");
        objRequest.setEutNextRole(nextRole);
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(
              Alert.class,
              "as e where e.referenceSearchKey='" + objRequest.getId()
                  + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
            }
          }
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.Returntrans.wfa",
              Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", objRequest.getClient().getId(), Description,
                "NEW", alertWindow);

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
        objRequest.setEscmDocaction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (objRequest.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequest.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey='" + objRequest.getId() + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
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
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty(
            "scm.Returntrans.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(), "",
            objRequest.getCreatedBy().getId(), objRequest.getClient().getId(), Description, "NEW",
            alertWindow);
        objRequest.setEscmDocaction("PD");
        objRequest.setEscmDocstatus("CO");
        objRequest.setEutNextRole(null);
        count = 1;

      }
      OBDal.getInstance().save(objRequest);
      requistionId = objRequest.getId();
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
        historyData.put("HistoryTable", ApprovalTables.Return_Transaction_History);
        historyData.put("HeaderColumn", ApprovalTables.Return_Transaction_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Return_Transaction_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.Return_Transaction);

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in RT: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(req.m_inout_id) from m_inout req join eut_next_role rl on "
          + "req.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.m_inout_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequestId);
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