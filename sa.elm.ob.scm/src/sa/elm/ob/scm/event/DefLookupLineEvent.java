package sa.elm.ob.scm.event;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.hcm.ehcmgrade;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

public class DefLookupLineEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ESCMDefLookupsTypeLn.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();

      OBQuery<ESCMDefLookupsTypeLn> typelist = OBDal.getInstance().createQuery(
          ESCMDefLookupsTypeLn.class,
          " as e where ( e.commercialName='" + lookup.getCommercialName() + "' or e.searchKey = '"
              + lookup.getSearchKey() + "' )  and e.client.id='" + lookup.getClient().getId()
              + "' and  e.escmDeflookupsType.id='" + lookup.getEscmDeflookupsType().getId() + "'");
      log.debug("typelist.size" + typelist.list().size());
      if (typelist.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_LookupLine_Unique"));
      }

    } catch (OBException e) {
      log.debug("exception while creating lookup" + e);
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

      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();
      final Property name = entities[0].getProperty(ehcmgrade.PROPERTY_SEARCHKEY);
      final Property code = entities[0].getProperty(ehcmgrade.PROPERTY_COMMERCIALNAME);
      if (!event.getCurrentState(name).equals(event.getPreviousState(name))
          || !event.getCurrentState(code).equals(event.getPreviousState(code))) {
        OBQuery<ESCMDefLookupsTypeLn> typelist = OBDal.getInstance()
            .createQuery(
                ESCMDefLookupsTypeLn.class,
                " as e  where ( e.commercialName='" + lookup.getCommercialName()
                    + "' or e.searchKey = '" + lookup.getSearchKey() + "' )  and e.client.id='"
                    + lookup.getClient().getId() + "' and  e.escmDeflookupsType.id='"
                    + lookup.getEscmDeflookupsType().getId() + "' and e.id <> '" + lookup.getId()
                    + "'");
        log.debug("typelist.size" + typelist.list().size());
        if (typelist.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_LookupLine_Unique"));
        }
      }
    } catch (OBException e) {
      log.debug("exception while updating lookup" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();
      /*
       * if (lookup.getMaterialMgmtShipmentInOutEMEscmIssuereasonList().size() > 0) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_Lookup(withIssueReturn)")); }
       */
      if (lookup.getMaterialMgmtShipmentInOutEMEscmReturnreasonList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup(WithReturn)"));
      }
    } catch (Exception e) {
      log.error(" Exception while Deleting LookUp Line  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
