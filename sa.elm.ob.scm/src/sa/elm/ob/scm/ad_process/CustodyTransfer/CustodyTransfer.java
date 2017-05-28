package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 20/03/2017
 */

public class CustodyTransfer extends DalBaseProcess {

  /**
   * This servlet class was responsible for Custody Transfer Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(CustodyTransfer.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    ResultSet rs = null;
    boolean errorFlag = false;
    String lastproductid = "";

    log.debug("entering into CustodyTransfer Submit");
    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus(), errorMsge = "", NextUserId = null;
      PreparedStatement st = null;
      int count = 0;
      NextRoleByRuleVO nextApproval = null;
      String query = null;
      Connection con = OBDal.getInstance().getConnection();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString(), sql = "";
      String pendingapproval = "", appstatus = "";
      String DocAction = inout.getEscmCtdocaction();
      String alertWindowType = AlertWindow.CustodyTransfer, alertRuleId = "";
      String Lang = vars.getLanguage();
      String status = null, product = null;
      String InvCtrl_Role = "";
      User usr = OBDal.getInstance().get(User.class, vars.getUser());

      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }

      // check all custody line transaction belongs to current beneficiary
      if (inout.getMaterialMgmtShipmentInOutLineList().size() > 0) {
        for (ShipmentInOutLine objLine : inout.getMaterialMgmtShipmentInOutLineList()) {
          if (objLine.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          if (objLine.getEscmCustodyTransactionList().size() == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          for (Escm_custody_transaction objTransaction : objLine.getEscmCustodyTransactionList()) {
            if (objTransaction.getEscmMrequestCustody().getBeneficiaryIDName() != inout
                .getEscmBname()) {
              errorFlag = true;
              objTransaction.setErrorreason("Not Belongs to "
                  + inout.getEscmBname().getCommercialName());
              OBDal.getInstance().save(objTransaction);
              OBDal.getInstance().flush();
              errorMsge = OBMessageUtils.messageBD("ESCM_Not_Beneficiary").replace("@",
                  inout.getEscmBname().getCommercialName());
            }
          }
        }
      }
      if (!errorFlag) {
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
            log.debug("wherecl:" + transaction.getWhereAndOrderBy());
            log.debug("size:" + transaction.list().size());
            if (transaction.list().size() > 0) {
              errorFlag = true;
              for (Escm_custody_transaction trans : transaction.list()) {
                if (status == null) {
                  status = OBMessageUtils.messageBD("ESCM_CT_TagAlreadyUsed");
                  status = status.replace("%", trans.getGoodsShipmentLine().getShipmentReceipt()
                      .getDocumentNo());
                  if (product == null) {
                    lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                    product = trans.getGoodsShipmentLine().getProduct().getName();
                  } else {
                    if (!lastproductid.equals(trans.getGoodsShipmentLine().getProduct().getId())) {
                      lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                      product = product + "," + trans.getGoodsShipmentLine().getProduct().getName();
                    }
                  }
                } else {
                  status += trans.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
                }
              }

            }
            tran.setErrorreason(status);
            OBDal.getInstance().save(line);
          }
        }
        if (errorFlag) {
          status = OBMessageUtils.messageBD("ESCM_ProcessFailed(Tag Status)");
          status = status.replace("%", product);
          // status += product;
          errorMsge = status;
        }
      }
      // get Current ApproverUser Id for a record &&
      // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
      if (inout.getEutNextRole() != null
          && !vars.getUser().equals(inout.getEscmCtreclinemng().getId())) {
        OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as line where line.eUTNextRole.id='" + inout.getEutNextRole().getId() + "'");
        if (line.list().size() > 0) {
          NextUserId = line.list().get(0).getUserContact().getId();
        }
      }
      if (!vars.getUser().equals(inout.getEscmCtreclinemng().getId()) || NextUserId == null) {
        // checking next user for approve the record
        nextApproval = NextRoleByRule.getCustTranNextRole(con, clientId, orgId, roleId, userId,
            Resource.CUSTODY_TRANSFER, inout, NextUserId);
      }
      if (!errorFlag) {
        // final approval process
        if (DocAction.equals("AP") && vars.getUser().equals(inout.getEscmCtreclinemng().getId())) {
          if (inout.getEscmSpecno() == null) {
            String sequence = Utility.getSpecificationSequence(inout.getOrganization().getId(),
                "CT");
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
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              objCustody.setAlertStatus("IU");
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              OBDal.getInstance().save(objCustody);
              objCustodytran.setTransactiontype("TR");
              objCustodytran.setTransactionDate(inout.getMovementDate());
              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in ('" + objCustodytran.getId()
                  + "' )    and escm_mrequest_custody_id ='" + objCustody.getId()
                  + "' and isprocessed='Y' order by created desc limit 1";
              st = conn.prepareStatement(query);
              rs = st.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(inout.getMovementDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              objCustodytran.setBname(inout.getEscmTobenefiName());
              objCustodytran.setBtype(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              OBDal.getInstance().save(objCustodytran);
            }
          }
          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          inout.setEscmDocstatus("CO");
          inout.setDocumentStatus("CO");
          inout.setDocumentAction("--");
          inout.setEscmCtdocaction("PD");
          inout.setEutNextRole(null);
          OBDal.getInstance().save(inout);
          count = 1;
          OBDal.getInstance().flush();
          appstatus = "AP";

          // alert process
          ArrayList<CustodyCardReportVO> includereceipient = new ArrayList<CustodyCardReportVO>();
          CustodyCardReportVO vo = null;
          // delete alert for approval alerts
          OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
              " as e  where e.referenceSearchKey='" + inout.getId() + "' and e.alertStatus='NEW'");
          log.debug("getWhereAndOrderBy:" + alertnew.getWhereAndOrderBy());
          log.debug("alertnew:" + alertnew.list().size());
          // set all the previoust new status as solved
          if (alertnew.list().size() > 0) {
            for (Alert alert : alertnew.list()) {
              alert.setAlertStatus("SOLVED");
            }
          }

          // getting alert receipient
          OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(
              AlertRule.class,
              " as e where e.client.id='" + clientId + "' and e.eSCMProcessType='"
                  + alertWindowType + "'");
          if (alertrule.list().size() > 0) {
            alertRuleId = alertrule.list().get(0).getId();
          }
          OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
              " as e where e.alertRule.id='" + alertRuleId + "'");
          if (alertrec.list().size() > 0) {
            for (AlertRecipient rec : alertrec.list()) {
              vo = new CustodyCardReportVO();
              vo.setRoleId(rec.getRole().getId());
              if (rec.getUserContact() != null)
                vo.setUserId(rec.getUserContact().getId());
              includereceipient.add(vo);
              OBDal.getInstance().remove(rec);
            }
          }
          Role objCreatedRole = null;
          if (inout.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = inout.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          vo = new CustodyCardReportVO();
          vo.setRoleId(objCreatedRole.getId());
          vo.setUserId(inout.getCreatedBy().getId());
          includereceipient.add(vo);

          // final approved need to send alert to inventory control
          OBQuery<Preference> invRole = OBDal.getInstance().createQuery(
              Preference.class,
              "as e where e.property='ESCM_Inventory_Control' and e.searchKey='Y' and e.client.id='"
                  + clientId + "'");
          if (invRole.list().size() > 0) {
            vo = new CustodyCardReportVO();
            InvCtrl_Role = invRole.list().get(0).getVisibleAtRole().getId();
            vo.setRoleId(invRole.list().get(0).getVisibleAtRole().getId());
            vo.setUserId(null);
            includereceipient.add(vo);
          }

          // avoid duplicate recipient
          HashSet<CustodyCardReportVO> incluedSet = new HashSet<CustodyCardReportVO>(
              includereceipient);

          // insert alert receipients
          for (CustodyCardReportVO vo1 : incluedSet) {
            AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                alertWindowType);
          }
          // set alert for requester
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.approved",
              Lang) + " " + usr.getName();
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), "", inout
              .getCreatedBy().getId(), inout.getClient().getId(), Description, "NEW",
              alertWindowType);
          // set alert for inventory control
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), InvCtrl_Role, "",
              inout.getClient().getId(), Description, "NEW", alertWindowType);
        }

        // other than approval process
        else {

          // update header status as inprogress and assign next approval
          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          inout.setEutNextRole(OBDal.getInstance().get(EutNextRole.class,
              nextApproval.getNextRoleId()));
          inout.setEscmDocstatus("ESCM_IP");
          inout.setEscmCtdocaction("AP");
          OBDal.getInstance().save(inout);

          if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
            appstatus = "SUB";
          } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
            appstatus = "AP";
          }
          pendingapproval = nextApproval.getStatus();

          // alert process
          // getting alertruleID
          ArrayList<CustodyCardReportVO> includereceipient = new ArrayList<CustodyCardReportVO>();
          CustodyCardReportVO vo = null;
          // ArrayList<String> includereceipientsuser = new ArrayList<String>();
          OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(
              AlertRule.class,
              " as e where e.client.id='" + clientId + "' and e.eSCMProcessType='"
                  + alertWindowType + "'");
          if (alertrule.list().size() > 0) {
            alertRuleId = alertrule.list().get(0).getId();
          }
          // set the alert for next approvals
          EutNextRole nextrole = OBDal.getInstance().get(EutNextRole.class,
              nextApproval.getNextRoleId());
          if (nextrole.getEutNextRoleLineList().size() > 0) {
            OBQuery<Alert> alertnew = OBDal.getInstance()
                .createQuery(
                    Alert.class,
                    " as e  where e.referenceSearchKey='" + inout.getId()
                        + "' and e.alertStatus='NEW'");
            // set all the previoust new status as solved
            if (alertnew.list().size() > 0) {
              for (Alert alert : alertnew.list()) {
                alert.setAlertStatus("SOLVED");
              }
            }
          }
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.wfa", Lang)
              + " " + inout.getCreatedBy().getName();

          // insert alert
          for (EutNextRoleLine line : nextrole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), line.getRole()
                .getId(), line.getUserContact().getId(), clientId, Description, "NEW",
                alertWindowType);
            vo = new CustodyCardReportVO();
            vo.setRoleId(line.getRole().getId());
            vo.setUserId(line.getUserContact().getId());
            includereceipient.add(vo);
            // includereceipientsuser.add(line.getUserContact().getId());
          }
          // getting alert receipient
          OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
              " as e where e.alertRule.id='" + alertRuleId + "'");
          if (alertrec.list().size() > 0) {
            for (AlertRecipient rec : alertrec.list()) {
              vo = new CustodyCardReportVO();
              vo.setRoleId(rec.getRole().getId());
              if (rec.getUserContact() != null)
                vo.setUserId(rec.getUserContact().getId());
              includereceipient.add(vo);
              // includereceipientsuser.add(rec.getUserContact().getId());
              OBDal.getInstance().remove(rec);
            }
          }
          // avoid duplicate receipient
          HashSet<CustodyCardReportVO> includerolset = new HashSet<CustodyCardReportVO>(
              includereceipient);

          // HashSet<String> includerolset = new HashSet<String>(includereceipientsrole);
          // Iterator<CustodyCardReportVO> iterator = includerolset.iterator();
          // while (iterator.hasNext()) {
          for (CustodyCardReportVO vo1 : includerolset) {
            AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                alertWindowType);
          }

        }
        if (!StringUtils.isEmpty(inout.getId())) {
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", inout.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);

          Utility.InsertApprovalHistory(historyData);

        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed:" + errorMsge);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(inout);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exeception in Custody Transfer Submit:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}