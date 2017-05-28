package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;

/**
 * 
 * @author Gopalakrishnan on 13/03/2017
 * 
 */
public class MaterialIRLineHandler extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in Escm_Material_Reqln Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      MaterialIssueRequestLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String strRecentAction = "";
      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      OBQuery<MaterialIssueRequestHistory> appQuery = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          "as e where e.escmMaterialRequest.id='" + objRequest.getId()
              + "' order by creationDate desc");
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        MaterialIssueRequestHistory objLastLine = appQuery.list().get(0);
        if (objLastLine.getRequestreqaction().equals("REJ")) {
          strRecentAction = "REJ";
        }
        if (objLastLine.getRequestreqaction().equals("REA")) {
          strRecentAction = "REA";
        }
        if (objLastLine.getRequestreqaction().equals("REV")) {
          strRecentAction = "REV";
        }
      }
      Boolean ispreference = Preferences.existsPreference("ESCM_LineManager", true, null, null,
          null, OBContext.getOBContext().getRole().getId(), null);
      if (!ispreference) {
        if (objRequest.getAlertStatus().equals("ESCM_IP")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_InProgress"));
        }
      }
      if (objRequest.getAlertStatus().equals("ESCM_TR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Approved"));
      }
      if (!ispreference) {
        if (objRequest.getEscmMaterialrequestHistList().size() > 0
            && (!strRecentAction.equals("REJ") && !strRecentAction.equals("REA") && !strRecentAction
                .equals("REV"))) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Submitted"));
        }
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting IssueRequest Line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      Product product = OBDal.getInstance().get(Product.class, objRequestLine.getProduct().getId());
      // same product should not be in lines
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      log.debug("issitereq:" + objRequest.isSiteissuereq());
      if (objRequestLine.getDeliveredQantity().compareTo(objRequestLine.getRequestedQty()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Delivered_Qty(High)"));
      }
      if (objRequest.isSiteissuereq() != null && !objRequest.isSiteissuereq()) {
        OBQuery<MaterialIssueRequestLine> reqLine = OBDal.getInstance().createQuery(
            MaterialIssueRequestLine.class,
            "escmMaterialRequest.id='" + objRequestLine.getEscmMaterialRequest().getId()
                + "' and product.id='" + product.getId() + "' and id<>'" + objRequestLine.getId()
                + "'");
        if (reqLine.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
        }
      }
      if (objRequest.isSiteissuereq() != null && objRequest.isSiteissuereq()) {
        if (objRequestLine.getRequestedQty().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Qty_Zero"));
        }
      }
      if (product.getEscmStockType() != null) {
        if (product.getEscmStockType().getSearchKey().equals("CUS")) {
          BigDecimal fractionalPart = objRequestLine.getRequestedQty().remainder(BigDecimal.ONE);
          if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
          BigDecimal deliveredqty = objRequestLine.getDeliveredQantity().remainder(BigDecimal.ONE);
          if (deliveredqty.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
        }
      }

    } catch (Exception e) {
      log.error(" Exception while updating IssueRequest Line:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      Product product = OBDal.getInstance().get(Product.class, objRequestLine.getProduct().getId());
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      // same product should not be in lines
      if (objRequestLine.getDeliveredQantity().compareTo(objRequestLine.getRequestedQty()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Delivered_Qty(High)"));
      }

      log.debug("issitereq:" + objRequest.isSiteissuereq());
      if (objRequest.isSiteissuereq() != null && !objRequest.isSiteissuereq()) {
        OBQuery<MaterialIssueRequestLine> reqLine = OBDal.getInstance().createQuery(
            MaterialIssueRequestLine.class,
            "escmMaterialRequest.id='" + objRequestLine.getEscmMaterialRequest().getId()
                + "' and product.id='" + product.getId() + "'");
        if (reqLine.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
        }
      }
      if (objRequest.isSiteissuereq() != null && objRequest.isSiteissuereq()) {
        if (objRequestLine.getRequestedQty().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Qty_Zero"));
        }
      }
      if (product.getEscmStockType() != null) {
        if (product.getEscmStockType().getSearchKey().equals("CUS")) {
          BigDecimal fractionalPart = objRequestLine.getRequestedQty().remainder(BigDecimal.ONE);
          if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
          BigDecimal deliveredqty = objRequestLine.getDeliveredQantity().remainder(BigDecimal.ONE);
          if (deliveredqty.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
        }
      }

    } catch (Exception e) {
      log.error(" Exception while updating IssueRequest Line:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
