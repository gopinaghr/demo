package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;

public class InitialReceiptsEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      EscmInitialReceipt.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      EscmInitialReceipt IReceipt = (EscmInitialReceipt) event.getTargetInstance();
      final Property Ir_Return_Qty = entities[0]
          .getProperty(EscmInitialReceipt.PROPERTY_RETURNQUANTITY);
      final Property Return_Qty = entities[0].getProperty(EscmInitialReceipt.PROPERTY_RETURNQTY);
      final Property Delivey_Qty = entities[0]
          .getProperty(EscmInitialReceipt.PROPERTY_DELIVEREDQTY);
      final Property Rejected_Qty = entities[0]
          .getProperty(EscmInitialReceipt.PROPERTY_REJECTEDQTY);
      final Property Accepted_Qty = entities[0]
          .getProperty(EscmInitialReceipt.PROPERTY_ACCEPTEDQTY);
      final Property FailureReason = entities[0]
          .getProperty(EscmInitialReceipt.PROPERTY_FAILUREREASON);

      // duplicate product
      ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class,
          IReceipt.getGoodsShipment().getId());
      if (header.getEscmReceivingtype().equals("IR") || header.getEscmReceivingtype().equals("SR")) {
        OBQuery<EscmInitialReceipt> reqLine = OBDal.getInstance().createQuery(
            EscmInitialReceipt.class,
            "goodsShipment.id='" + IReceipt.getGoodsShipment().getId() + "' and product.id='"
                + IReceipt.getProduct().getId() + "' and id<>'" + IReceipt.getId() + "'");
        if (reqLine.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_PO_DuplicateProduct"));
        }
      }
      // Receipt Transaction Date should not allow future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (!IReceipt.getGoodsShipment().getDocumentStatus().equals("DR")) {
        if (event.getPreviousState(Ir_Return_Qty).equals(event.getCurrentState(Ir_Return_Qty))
            && event.getPreviousState(Return_Qty).equals(event.getCurrentState(Return_Qty))
            && event.getPreviousState(Delivey_Qty).equals(event.getCurrentState(Delivey_Qty))
            && event.getPreviousState(Rejected_Qty).equals(event.getCurrentState(Rejected_Qty))
            && event.getPreviousState(Accepted_Qty).equals(event.getCurrentState(Accepted_Qty))
            && (event.getPreviousState(FailureReason) != null && event.getPreviousState(
                FailureReason).equals(event.getCurrentState(FailureReason)))) {
          throw new OBException(OBMessageUtils.messageBD("Escm_No_save_IR"));
        }
      }
      if (IReceipt.getDeliverydate() != null) {
        if (dateFormat.parse(dateFormat.format(IReceipt.getDeliverydate())).compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IR_Deliveydate"));
        }
      }
      if (IReceipt.getGoodsShipment().getEscmReceivingtype().equals("IR")
          || IReceipt.getGoodsShipment().getEscmReceivingtype().equals("SR")) {
        if (IReceipt.getQuantity() == null
            || IReceipt.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IR_Quantity"));
        }
      }
      if (header.getEscmReceivingtype().equals("SR")) {
        if (IReceipt.getUnitprice() == null
            || IReceipt.getUnitprice().compareTo(BigDecimal.ZERO) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Unitprice"));
        }
      }
    } catch (OBException e) {
      log.error("Exception while updating Initial Receipt: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      e.printStackTrace();
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
      EscmInitialReceipt IReceipt = (EscmInitialReceipt) event.getTargetInstance();
      // duplicate product
      ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class,
          IReceipt.getGoodsShipment().getId());
      if (header.getEscmReceivingtype().equals("IR") || header.getEscmReceivingtype().equals("SR")) {
        OBQuery<EscmInitialReceipt> reqLine = OBDal.getInstance().createQuery(
            EscmInitialReceipt.class,
            "goodsShipment.id='" + IReceipt.getGoodsShipment().getId() + "' and product.id='"
                + IReceipt.getProduct().getId() + "'");
        if (reqLine.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_PO_DuplicateProduct"));
        }
      }
      // Receipt Transaction Date should not allow future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (!IReceipt.getGoodsShipment().getDocumentStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_No_save_IR"));
      }
      if (IReceipt.getDeliverydate() != null) {
        if (IReceipt.getDeliverydate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IR_Deliveydate"));
        }
      }
      if (IReceipt.getGoodsShipment().getEscmReceivingtype().equals("IR")
          || IReceipt.getGoodsShipment().getEscmReceivingtype().equals("SR")) {
        if (IReceipt.getQuantity() == null
            || IReceipt.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IR_Quantity"));
        }
      }
      if (header.getEscmReceivingtype().equals("SR")) {
        if (IReceipt.getUnitprice() == null
            || IReceipt.getUnitprice().compareTo(BigDecimal.ZERO) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Unitprice"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while creating Initial Receipt: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      e.printStackTrace();
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
      EscmInitialReceipt IReceipt = (EscmInitialReceipt) event.getTargetInstance();
      if (!IReceipt.getGoodsShipment().getDocumentStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_No_save_IR"));
      }
    } catch (OBException e) {
      log.error(" Exception while deleting Initial Receipt: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
