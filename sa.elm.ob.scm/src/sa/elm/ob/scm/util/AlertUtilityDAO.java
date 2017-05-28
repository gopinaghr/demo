package sa.elm.ob.scm.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * @author Gopalakrishnan on 21/02/2017
 * 
 */
public class AlertUtilityDAO {
  private static final Logger log4j = Logger.getLogger(AlertUtilityDAO.class);

  /**
   * This method only for Purchase Requisition Alert Process With Preference Configuration
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  @SuppressWarnings("unchecked")
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String window) {
    String sqlQuery = "";
    SQLQuery query = null;
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + window + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
          AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      sqlQuery = "select visibleat_role_id  from ad_preference where property='" + property
          + "' and ad_client_id='" + clientId + "' ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);

      List<String> ruleList = (ArrayList<String>) query.list();
      if (ruleList != null && ruleList.size() > 0) {
        for (int i = 0; i < ruleList.size(); i++) {
          String object = (String) ruleList.get(i);
          Alert objAlert = OBProvider.getInstance().get(Alert.class);
          objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
          objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
          // imported via data set
          objAlert.setDescription(description);
          objAlert.setRole(OBDal.getInstance().get(Role.class, object.toString()));
          objAlert.setRecordID(DocumentNo);
          objAlert.setReferenceSearchKey(DocumentId);
          objAlert.setAlertStatus(status);
          OBDal.getInstance().save(objAlert);
          OBDal.getInstance().flush();
          includeRecipient.add(object.toString());
        }
        isSuccess = Boolean.TRUE;
      }
      // check and insert Recipient
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
        insertAlertRecipient(iterator.next(), null, clientId, window);
      }

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      e.printStackTrace();
      log4j.error("Exception in alertInsertionPreference", e);
    }
    return isSuccess;
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param roleId
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + Window + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      // imported via data set
      objAlert.setDescription(description);
      if (!roleId.isEmpty() && !roleId.equals("")) {
        objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
      }
      if (!userId.isEmpty() && !userId.equals("")) {
        objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
      }
      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert Recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String userId, String clientId,
      String Window) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {

      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + Window + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      AlertRecipient objAlertRecipient = OBProvider.getInstance().get(AlertRecipient.class);
      objAlertRecipient.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlertRecipient.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlertRecipient.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlertRecipient.setRole(OBDal.getInstance().get(Role.class, roleId));
      objAlertRecipient.setSendEMail(false);
      if (userId != null)
        objAlertRecipient.setUserContact(OBDal.getInstance().get(User.class, userId));
      OBDal.getInstance().save(objAlertRecipient);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in insertAlertRecipient", e);
    }
    return isSuccess;
  }
}