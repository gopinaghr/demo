package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

/**
 * 
 * @author Gopalakrishnan on 16/02/2017
 * 
 */
public class RequisitionLineEvent extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in M_Requisition_Line Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      RequisitionLine.ENTITY_NAME) };

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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();
      String reqlineId = objRequisitionLine.getId();
      String reqId = objRequisitionLine.getRequisition().getId();
      final String userId = OBContext.getOBContext().getUser().getId();
      final String roleId = OBContext.getOBContext().getRole().getId();
      final String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, reqId);
      log.debug("objRequisition:" + objRequisition);
      Boolean ispreference = false;
      /*
       * Preferences.existsPreference("ESCM_LineManager", true, null, null, null,
       * OBContext.getOBContext().getRole().getId(), null); log.debug("isprefer:" + ispreference);
       */
      String preferenceValue = "";
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_LineManager", true, clientId,
            objRequisition.getOrganization().getId(), userId, roleId, "800092");
      } catch (PropertyException e) {
      }
      if (preferenceValue.equals("Y"))
        ispreference = true;

      OBQuery<RequisitionLine> linequery = OBDal.getInstance().createQuery(RequisitionLine.class,
          " as e where e.requisition.id='" + reqId + "' and e.id <>'" + reqlineId + "'");
      if (linequery.list().size() == 0 && objRequisition != null
          && objRequisition.getEscmDocStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_AtleastOneRow"));
      }

      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_IP")
          && !ispreference) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_InProgress"));
      }
      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Approved"));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting RequisitionLine  : " + e);
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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();
      final Property qty = entities[0].getProperty(RequisitionLine.PROPERTY_QUANTITY);

      if (!event.getCurrentState(qty).equals(event.getPreviousState(qty))) {

        if (new BigDecimal(objRequisitionLine.getQuantity()).compareTo(BigDecimal.ZERO) == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
        }

      }
      if (objRequisitionLine.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Unitprice_Negative"));
      }
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      if (objRequisitionLine.getNeedByDate().before(cal.getTime())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_PastDate_Requistion"));
      }
    } catch (Exception e) {
      log.error(" Exception while updating RequisitionLine  : " + e);
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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();

      if (new BigDecimal(objRequisitionLine.getQuantity()).compareTo(BigDecimal.ZERO) == 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
      }

      if (objRequisitionLine.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Unitprice_Negative"));
      }
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      if (objRequisitionLine.getNeedByDate().before(cal.getTime())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_PastDate_Requistion"));
      }
    } catch (Exception e) {
      log.error(" Exception while updating RequisitionLine  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
