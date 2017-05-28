package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.utility.util.Utility;

public class PoReceiptEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ShipmentInOut.ENTITY_NAME) };

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
      ShipmentInOut poReceipt = (ShipmentInOut) event.getTargetInstance();
      final Property isCustody = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMISCUSTODYTRANSFER);
      final Property docno = entities[0].getProperty(ShipmentInOut.PROPERTY_ORDERREFERENCE);
      final Property acctdate = entities[0].getProperty(ShipmentInOut.PROPERTY_ACCOUNTINGDATE);
      final Property supplier = entities[0].getProperty(ShipmentInOut.PROPERTY_BUSINESSPARTNER);
      final Property doctype = entities[0].getProperty(ShipmentInOut.PROPERTY_DOCUMENTTYPE);
      final Property benetype = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMBTYPE);
      final Property benename = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMBNAME);
      final Property issuereason = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMISSUEREASON);

      final Property sender = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMCTSENDER);
      final Property senderline = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMCTSENDLINEMNG);
      final Property receiver = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMCTRECEIVER);
      final Property reclinemangager = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMCTRECLINEMNG);

      final Property objPartnerAddress = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_PARTNERADDRESS);
      final Property transactiontype = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMRECEIVINGTYPE);
      // Receipt Transaction Date should not allow future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      // check bp location
      if (poReceipt.getPartnerAddress() == null) {
        Country objCountry = OBDal.getInstance().get(Country.class, "296");
        org.openbravo.model.common.geography.Location objLocation = OBProvider.getInstance().get(
            org.openbravo.model.common.geography.Location.class);
        objLocation.setCountry(objCountry);
        OBDal.getInstance().save(objLocation);
        Location objBpLocation = OBProvider.getInstance().get(Location.class);
        objBpLocation.setLocationAddress(objLocation);
        objBpLocation.setName("Default Address");
        objBpLocation.setBusinessPartner(poReceipt.getBusinessPartner());
        OBDal.getInstance().save(objBpLocation);
        event.setCurrentState(objPartnerAddress, objBpLocation);
      }

      if (poReceipt.getMovementDate() != null) {
        if (poReceipt.getMovementDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Rececipt_transaction_date"));
        }
      }
      if (event.getCurrentState(isCustody) != null) {
        if (event.getCurrentState(isCustody).toString().equals("true")) {
          if (poReceipt.getEscmTobeneficiary() == null || poReceipt.getEscmTobenefiName() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_ToBeneficiary(empty)"));
          }
        }
        /*
         * Task No.4812 if (poReceipt.getEscmFromemployee() == null ||
         * StringUtils.isEmpty(poReceipt.getEscmFromemployee())) { throw new
         * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)"));
         * 
         * } if (poReceipt.getEscmToemployee() == null ||
         * StringUtils.isEmpty(poReceipt.getEscmToemployee())) { throw new
         * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)")); } if
         * (poReceipt.getEscmCusapprover() == null) { throw new
         * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)")); }
         */
        /*
         * if (poReceipt.getEscmCtsender() == null || poReceipt.getEscmCtsendlinemng() == null ||
         * poReceipt.getEscmCtreclinemng() == null || poReceipt.getEscmCtreceiver() == null) { throw
         * new OBException(OBMessageUtils.messageBD("ESCM_CustodyApp_Fields(empty)")); }
         */
        if ((event.getCurrentState(sender) != null && (!event.getCurrentState(sender).equals(
            event.getPreviousState(sender))))
            || (event.getCurrentState(sender) != null && (!event.getCurrentState(senderline)
                .equals(event.getPreviousState(senderline))))
            || (event.getCurrentState(sender) != null && (!event.getCurrentState(receiver).equals(
                event.getPreviousState(receiver))))
            || (event.getCurrentState(sender) != null && (!event.getCurrentState(reclinemangager)
                .equals(event.getPreviousState(reclinemangager))))) {
          if ((poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtsendlinemng()))
              || (poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtreceiver()))
              || (poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtreclinemng()))
              || (poReceipt.getEscmCtsendlinemng().equals(poReceipt.getEscmCtreceiver()))
              || (poReceipt.getEscmCtreclinemng().equals(poReceipt.getEscmCtreceiver()))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_CTSameUerNotAllowed"));
          }
        }
      }
      if ((event.getCurrentState(docno) != null && !event.getCurrentState(docno).equals(
          event.getPreviousState(docno)))
          || !event.getCurrentState(acctdate).equals(event.getPreviousState(acctdate))
          || !event.getCurrentState(supplier).equals(event.getPreviousState(supplier))
          || !event.getCurrentState(doctype).equals(event.getPreviousState(doctype))) {
        SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
        String accdate = convertTohijriDate(dateYearFormat.format(poReceipt.getAccountingDate()));

        SQLQuery bpartnerQuery = OBDal
            .getInstance()
            .getSession()
            .createSQLQuery(
                " select c_bpartner_id as bpartnerid,c_bpartner_location_id as locationid,c_doctype_id,"
                    + " eut_convert_to_hijri(to_char(dateacct,'yyyy-MM-dd'))  as dateacct from m_inout "
                    + " where em_escm_receivingtype in ('SR','IR','INS','DEL','RET') and poreference= '"
                    + poReceipt.getOrderReference() + "' and ad_client_id= '"
                    + poReceipt.getClient().getId() + "' and ad_org_id= '"
                    + poReceipt.getOrganization().getId() + "' order by created desc limit 1");

        if (bpartnerQuery.list().size() > 0) {
          Object[] objects = (Object[]) bpartnerQuery.list().get(0);
          log.debug("ConvertedDate:" + objects[0].toString());
          log.debug("ConvertedDate:" + objects[1].toString());
          log.debug("ConvertedDate:" + objects[2].toString());
          log.debug("ConvertedDate:" + objects[3].toString());

          if ((!poReceipt.getBusinessPartner().getId().equals(objects[0].toString()))
              || (!poReceipt.getDocumentType().getId().equals(objects[2].toString()))
              || (!accdate.equals(objects[3].toString()))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PORecDocSupDiff"));
          }
        }
      }
      if (event.getCurrentState(issuereason) != null
          && !event.getCurrentState(issuereason).equals(event.getPreviousState(issuereason))) {
        if (poReceipt.getEscmReceivingtype().equals("IRT")
            && (poReceipt.getEscmIssuereason().equals("IS") || poReceipt.getEscmIssuereason()
                .equals("RW"))) {

          if (poReceipt.getEscmBtype() == null || poReceipt.getEscmBname() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_IRT_BenTypName_Mand"));
          }
        }
        if (poReceipt.getMaterialMgmtShipmentInOutLineList().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IRTChgIssueReason"));
        }
      }

      if (poReceipt.getEscmBtype() != null
          && (poReceipt.getEscmBtype().equals("D") || poReceipt.getEscmBtype().equals("E") || poReceipt
              .getEscmBtype().equals("S"))) {
        if (poReceipt.getEscmBname() == null) {
          if (!poReceipt.getEscmReceivingtype().equals("INR")) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
          } else {
            throw new OBException(OBMessageUtils.messageBD("ESCM_RT_Returner_IDName"));
          }
        }
      }
      if (poReceipt.getEscmReceivingtype().equals("INR")) {
        if (poReceipt.getEscmBtype() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_RT_Returner_Type"));
          //
        }
      }
      if (event.getCurrentState(transactiontype).equals("LD")) {
        if (poReceipt.getEscmBtype() == null || poReceipt.getEscmBname() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IRT_BenTypName_Mand"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating PO Receipt:" + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
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
      Long code = Long.valueOf("0001");
      String newCode = "";
      String sequence = "";
      Boolean sequenceexists = false;
      ShipmentInOut poReceipt = (ShipmentInOut) event.getTargetInstance();
      final Property SpecNo = entities[0].getProperty(ShipmentInOut.PROPERTY_DOCUMENTNO);
      final Property DocNo = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMDOCNO);
      final Property transactiontype = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMRECEIVINGTYPE);
      final Property isCustody = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMISCUSTODYTRANSFER);
      final Property refdate = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMREFDATE);
      final Property objBusinessPartner = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_BUSINESSPARTNER);
      final Property objPartnerAddress = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_PARTNERADDRESS);
      final Property returnaddlines = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMTRANADDLINE);
      final Property issretaddlines = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMRETURNADDLINES);
      final Property retprocess = entities[0].getProperty(ShipmentInOut.PROPERTY_ESCMRETURNPROCESS);
      final Property iscustaddlines = entities[0]
          .getProperty(ShipmentInOut.PROPERTY_ESCMCUSTODYADDLINES);

      DateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
      String stryearDate = yearFormat.format(poReceipt.getMovementDate());
      event.setCurrentState(refdate, stryearDate);

      // check bp location
      if (poReceipt.getPartnerAddress() == null) {
        Country objCountry = OBDal.getInstance().get(Country.class, "296");
        org.openbravo.model.common.geography.Location objLocation = OBProvider.getInstance().get(
            org.openbravo.model.common.geography.Location.class);
        objLocation.setCountry(objCountry);
        OBDal.getInstance().save(objLocation);
        Location objBpLocation = OBProvider.getInstance().get(Location.class);
        objBpLocation.setLocationAddress(objLocation);
        objBpLocation.setName("Default Address");
        objBpLocation.setBusinessPartner(poReceipt.getBusinessPartner());
        OBDal.getInstance().save(objBpLocation);
        event.setCurrentState(objPartnerAddress, objBpLocation);
      }
      // Receipt Transaction Date should not allow future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (poReceipt.getMovementDate() != null) {
        if (poReceipt.getMovementDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Rececipt_transaction_date"));
        }
      }
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      String hijiridate = convertTohijriDate(dateYearFormat.format(todaydate));
      int year = Integer.parseInt(hijiridate.split("-")[2]);
      log.debug("Year:" + year);
      if (event.getCurrentState(isCustody) != null) {
        if (event.getCurrentState(isCustody).toString().equals("true")) {
          if (event.getCurrentState(objBusinessPartner) == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_DefaultBpartner(Empty)"));
          }
          if (poReceipt.getEscmTobeneficiary() == null || poReceipt.getEscmTobenefiName() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_ToBeneficiary(empty)"));
          }
          /*
           * Task No.4812 if (poReceipt.getEscmFromemployee() == null ||
           * StringUtils.isEmpty(poReceipt.getEscmFromemployee())) { throw new
           * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)"));
           * 
           * } if (poReceipt.getEscmToemployee() == null ||
           * StringUtils.isEmpty(poReceipt.getEscmToemployee())) { throw new
           * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)")); } if
           * (poReceipt.getEscmCusapprover() == null) { throw new
           * OBException(OBMessageUtils.messageBD("ESCM_Custody_Fields(empty)")); }
           */
          /*
           * if (poReceipt.getEscmCtsender() == null || poReceipt.getEscmCtsendlinemng() == null ||
           * poReceipt.getEscmCtreclinemng() == null || poReceipt.getEscmCtreceiver() == null) {
           * throw new OBException(OBMessageUtils.messageBD("ESCM_CustodyApp_Fields(empty)")); }
           */
          if ((poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtsendlinemng()))
              || (poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtreceiver()))
              || (poReceipt.getEscmCtsender().equals(poReceipt.getEscmCtreclinemng()))
              || (poReceipt.getEscmCtsendlinemng().equals(poReceipt.getEscmCtreceiver()))
              || (poReceipt.getEscmCtreclinemng().equals(poReceipt.getEscmCtreceiver()))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_CTSameUerNotAllowed"));
          }
          // 1439R0001
          sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "CT");
          sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(),
              "CT", sequence);
          if (!sequenceexists) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
          }
          event.setCurrentState(iscustaddlines, true);
        }
      }
      if (event.getCurrentState(transactiontype).equals("IR")
          && event.getCurrentState(isCustody) == null) {/*
                                                         * // 1439R0001 OBQuery<ShipmentInOut>
                                                         * lineIR =
                                                         * OBDal.getInstance().createQuery(ShipmentInOut
                                                         * .class,
                                                         * "as e where e.escmReceivingtype='IR'  order by e.creationDate desc"
                                                         * ); lineIR.setMaxResult(1); if
                                                         * (lineIR.list().size() > 0) { code =
                                                         * lineIR.list().get(0).getEscmDocno(); if
                                                         * (code != null) { newCode =
                                                         * String.valueOf(
                                                         * year).concat("R").concat(String
                                                         * .format("%04d", code + 1));
                                                         * event.setCurrentState(DocNo, code + 1); }
                                                         * else { newCode =
                                                         * String.valueOf(year).concat("R")
                                                         * .concat(String.format("%04d",
                                                         * Long.valueOf("0001")));
                                                         * event.setCurrentState(DocNo,
                                                         * Long.valueOf("0001")); } } else { newCode
                                                         * =
                                                         * String.valueOf(year).concat("R").concat(
                                                         * String.format("%04d", code));
                                                         * event.setCurrentState(DocNo, code); }
                                                         */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "IR");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "IR",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }

      if (event.getCurrentState(transactiontype).equals("INS")) {/*
                                                                  * // 1439F0001
                                                                  * OBQuery<ShipmentInOut> lineINS =
                                                                  * OBDal.getInstance().createQuery(
                                                                  * ShipmentInOut.class,
                                                                  * "as e where e.escmReceivingtype='INS'  order by e.creationDate desc"
                                                                  * ); lineINS.setMaxResult(1); if
                                                                  * (lineINS.list().size() > 0) {
                                                                  * code =
                                                                  * lineINS.list().get(0).getEscmDocno
                                                                  * (); if (code != null) { newCode
                                                                  * =
                                                                  * String.valueOf(year).concat("F"
                                                                  * ).concat(String.format("%04d",
                                                                  * code + 1));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code + 1); } else { newCode =
                                                                  * String.valueOf(year).concat("F")
                                                                  * .concat(String.format("%04d",
                                                                  * Long.valueOf("0001")));
                                                                  * event.setCurrentState(DocNo,
                                                                  * Long.valueOf("0001")); } } else
                                                                  * { newCode =
                                                                  * String.valueOf(year).
                                                                  * concat("F").
                                                                  * concat(String.format("%04d",
                                                                  * code));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code); }
                                                                  */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "INS");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "INS",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }

      if (event.getCurrentState(transactiontype).equals("SR")) {/*
                                                                 * // 1439D0001
                                                                 * OBQuery<ShipmentInOut> lineSR =
                                                                 * OBDal.getInstance().createQuery(
                                                                 * ShipmentInOut.class,
                                                                 * "as e where e.escmReceivingtype='SR'  order by e.creationDate desc"
                                                                 * ); lineSR.setMaxResult(1); if
                                                                 * (lineSR.list().size() > 0) { code
                                                                 * =
                                                                 * lineSR.list().get(0).getEscmDocno
                                                                 * (); if (code != null) { newCode =
                                                                 * String
                                                                 * .valueOf(year).concat("D").concat
                                                                 * (String.format("%04d", code +
                                                                 * 1)); event.setCurrentState(DocNo,
                                                                 * code + 1); } else { newCode =
                                                                 * String.valueOf(year).concat("D")
                                                                 * .concat(String.format("%04d",
                                                                 * Long.valueOf("0001")));
                                                                 * event.setCurrentState(DocNo,
                                                                 * Long.valueOf("0001")); } } else {
                                                                 * newCode =
                                                                 * String.valueOf(year).concat
                                                                 * ("D").concat
                                                                 * (String.format("%04d", code));
                                                                 * event.setCurrentState(DocNo,
                                                                 * code); }
                                                                 */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "SR");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "SR",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }

      if (event.getCurrentState(transactiontype).equals("DEL")) {
        /*
         * // 1439M0001 OBQuery<ShipmentInOut> lineDEL =
         * OBDal.getInstance().createQuery(ShipmentInOut.class,
         * "as e where e.escmReceivingtype='DEL'  order by e.creationDate desc");
         * lineDEL.setMaxResult(1); if (lineDEL.list().size() > 0) { code =
         * lineDEL.list().get(0).getEscmDocno(); if (code != null) { newCode =
         * String.valueOf(year).concat("M").concat(String.format("%04d", code + 1));
         * event.setCurrentState(DocNo, code + 1); } else { newCode =
         * String.valueOf(year).concat("M") .concat(String.format("%04d", Long.valueOf("0001")));
         * event.setCurrentState(DocNo, Long.valueOf("0001")); } } else { newCode =
         * String.valueOf(year).concat("M").concat(String.format("%04d", code));
         * event.setCurrentState(DocNo, code); }
         */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "DEL");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "DEL",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }

      if (event.getCurrentState(transactiontype).equals("RET")) {
        /*
         * // 1439V0001 OBQuery<ShipmentInOut> lineRET =
         * OBDal.getInstance().createQuery(ShipmentInOut.class,
         * "as e where e.escmReceivingtype='RET'  order by e.creationDate desc");
         * lineRET.setMaxResult(1); if (lineRET.list().size() > 0) { code =
         * lineRET.list().get(0).getEscmDocno(); if (code != null) { newCode =
         * String.valueOf(year).concat("V").concat(String.format("%04d", code + 1));
         * event.setCurrentState(DocNo, code + 1); } else { newCode =
         * String.valueOf(year).concat("V") .concat(String.format("%04d", Long.valueOf("0001")));
         * event.setCurrentState(DocNo, Long.valueOf("0001")); } } else { newCode =
         * String.valueOf(year).concat("V").concat(String.format("%04d", code));
         * event.setCurrentState(DocNo, code); }
         */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "RET");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "RET",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }
      if (event.getCurrentState(transactiontype).equals("IRT")) {/*
                                                                  * // 1439R0001
                                                                  * OBQuery<ShipmentInOut> lineIR =
                                                                  * OBDal.getInstance().createQuery(
                                                                  * ShipmentInOut.class,
                                                                  * "as e where e.escmReceivingtype='IRT'  and e.organization.id='"
                                                                  * +
                                                                  * poReceipt.getOrganization().getId
                                                                  * () + "'  and e.client.id='" +
                                                                  * poReceipt.getClient().getId() +
                                                                  * "' order by e.creationDate desc"
                                                                  * ); lineIR.setMaxResult(1); if
                                                                  * (lineIR.list().size() > 0) {
                                                                  * code =
                                                                  * lineIR.list().get(0).getEscmDocno
                                                                  * (); if (code != null) { newCode
                                                                  * =
                                                                  * String.valueOf(year).concat("T"
                                                                  * ).concat(String.format("%04d",
                                                                  * code + 1));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code + 1); } else { newCode =
                                                                  * String.valueOf(year).concat("T")
                                                                  * .concat(String.format("%04d",
                                                                  * Long.valueOf("0001")));
                                                                  * event.setCurrentState(DocNo,
                                                                  * Long.valueOf("0001")); } } else
                                                                  * { newCode =
                                                                  * String.valueOf(year).
                                                                  * concat("T").
                                                                  * concat(String.format("%04d",
                                                                  * code));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code); }
                                                                  */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "IRT");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "IRT",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }
      if (event.getCurrentState(transactiontype).equals("INR")) {/*
                                                                  * // 1439R0001
                                                                  * OBQuery<ShipmentInOut> lineIR =
                                                                  * OBDal.getInstance().createQuery(
                                                                  * ShipmentInOut.class,
                                                                  * "as e where e.escmReceivingtype='INR' and e.organization.id='"
                                                                  * +
                                                                  * poReceipt.getOrganization().getId
                                                                  * () + "'  and e.client.id='" +
                                                                  * poReceipt.getClient().getId() +
                                                                  * "' order by e.creationDate desc"
                                                                  * ); lineIR.setMaxResult(1); if
                                                                  * (lineIR.list().size() > 0) {
                                                                  * code =
                                                                  * lineIR.list().get(0).getEscmDocno
                                                                  * (); if (code != null) { newCode
                                                                  * =
                                                                  * String.valueOf(year).concat("K"
                                                                  * ).concat(String.format("%04d",
                                                                  * code + 1));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code + 1); } else { newCode =
                                                                  * String.valueOf(year).concat("K")
                                                                  * .concat(String.format("%04d",
                                                                  * Long.valueOf("0001")));
                                                                  * event.setCurrentState(DocNo,
                                                                  * Long.valueOf("0001")); } } else
                                                                  * { newCode =
                                                                  * String.valueOf(year).
                                                                  * concat("K").
                                                                  * concat(String.format("%04d",
                                                                  * code));
                                                                  * event.setCurrentState(DocNo,
                                                                  * code); }
                                                                  */
        // set new Spec No
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "INR");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "INR",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
      }
      if (event.getCurrentState(transactiontype).equals("LD")) {
        /*
         * // 1439U0001 OBQuery<ShipmentInOut> lineIR = OBDal.getInstance().createQuery(
         * ShipmentInOut.class, "as e where e.escmReceivingtype='LD' and e.organization.id='" +
         * poReceipt.getOrganization().getId() + "'  and e.client.id='" +
         * poReceipt.getClient().getId() + "' order by e.creationDate desc");
         * lineIR.setMaxResult(1); if (lineIR.list().size() > 0) { code =
         * lineIR.list().get(0).getEscmDocno(); if (code != null) { newCode =
         * String.valueOf(year).concat("U").concat(String.format("%04d", code + 1));
         * event.setCurrentState(DocNo, code + 1); } else { newCode =
         * String.valueOf(year).concat("U") .concat(String.format("%04d", Long.valueOf("0001")));
         * event.setCurrentState(DocNo, Long.valueOf("0001")); } } else { newCode =
         * String.valueOf(year).concat("U").concat(String.format("%04d", code));
         * event.setCurrentState(DocNo, code); } // set new Spec No event.setCurrentState(SpecNo,
         * newCode);
         */
        sequence = Utility.getTransactionSequence(poReceipt.getOrganization().getId(), "LD");
        sequenceexists = Utility.chkTransactionSequence(poReceipt.getOrganization().getId(), "LD",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
        if (poReceipt.getEscmBtype() == null || poReceipt.getEscmBname() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IRT_BenTypName_Mand"));
        }
      }
      if (poReceipt.getOrderReference() != null) {

        String accdate = convertTohijriDate(dateYearFormat.format(poReceipt.getAccountingDate()));
        log.debug("accdate:" + accdate);
        SQLQuery bpartnerQuery = OBDal
            .getInstance()
            .getSession()
            .createSQLQuery(
                " select c_bpartner_id as bpartnerid,c_bpartner_location_id as locationid,c_doctype_id, eut_convert_to_hijri(to_char(dateacct,'yyyy-MM-dd'))  as dateacct from m_inout "
                    + " where em_escm_receivingtype in ('SR','IR','INS','DEL','RET') and poreference= '"
                    + poReceipt.getOrderReference()
                    + "' and ad_client_id= '"
                    + poReceipt.getClient().getId()
                    + "' and ad_org_id= '"
                    + poReceipt.getOrganization().getId() + "' order by created desc limit 1");
        if (bpartnerQuery.list().size() > 0) {
          Object[] objects = (Object[]) bpartnerQuery.list().get(0);
          log.debug("ConvertedDate:" + objects[0].toString());
          log.debug("ConvertedDate:" + objects[1].toString());
          log.debug("ConvertedDate:" + objects[3].toString());

          if ((!poReceipt.getBusinessPartner().getId().equals(objects[0].toString()))
              || (!poReceipt.getDocumentType().getId().equals(objects[2].toString()))
              || (!accdate.equals(objects[3].toString()))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PORecDocSupDiff"));
          }
        }
      }
      if (poReceipt.getEscmReceivingtype().equals("IRT")) {
        event.setCurrentState(retprocess, true);
        event.setCurrentState(issretaddlines, true);
      } else if (poReceipt.getEscmReceivingtype().equals("INR")) {
        event.setCurrentState(retprocess, true);
        event.setCurrentState(returnaddlines, true);
      }
      if (poReceipt.getEscmReceivingtype().equals("IRT")
          && (poReceipt.getEscmIssuereason().equals("IS") || poReceipt.getEscmIssuereason().equals(
              "RW"))) {
        if (poReceipt.getEscmBtype() == null || poReceipt.getEscmBname() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IRT_BenTypName_Mand"));
        }
      }
      if (poReceipt.getEscmReceivingtype().equals("INR")) {
        if (poReceipt.getEscmBtype() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_RT_Returner_Type"));
        }
      }
      if (poReceipt.getEscmBtype() != null
          && (poReceipt.getEscmBtype().equals("D") || poReceipt.getEscmBtype().equals("E") || poReceipt
              .getEscmBtype().equals("S"))) {

        if (poReceipt.getEscmBname() == null) {
          if (!poReceipt.getEscmReceivingtype().equals("INR")) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
          } else {
            throw new OBException(OBMessageUtils.messageBD("ESCM_RT_Returner_IDName"));
          }

        }

      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(SpecNo, sequence);
      }

    } catch (OBException e) {
      log.error(" Exception while creating PO Receipt: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
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
      ShipmentInOut poReceipt = (ShipmentInOut) event.getTargetInstance();
      if (poReceipt.getEscmReceivingtype().equals("INR")
          || poReceipt.getEscmReceivingtype().equals("IR")) {
        if (poReceipt.getEscmSpecno() != null && StringUtils.isNotEmpty(poReceipt.getEscmSpecno())) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Spec_Generated(Error)"));
        }
      }
      if (poReceipt != null && !poReceipt.getEscmDocstatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting RequisitionLine  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal
          .getInstance()
          .getSession()
          .createSQLQuery(
              "select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
                  + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
        log.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

}
