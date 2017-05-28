package sa.elm.ob.scm.ad_process.ImportDepartment.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.scm.ad_process.ImportDepartment.vo.ImportDepartmentVO;

/**
 * @author Divya Servlet implementation class Import Department in Organization
 */

public class ImportDepartmentDAO {

  Logger log4j = Logger.getLogger(ImportDepartmentDAO.class);
  private Connection conn = null;
  // Function to insert the records in transaction screens.
  SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
  int year = Calendar.getInstance().get(Calendar.YEAR);

  public ImportDepartmentDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String filePath, String orgId, VariablesSecureApp vars) {

    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;

    try {
      OBContext.setAdminMode(true);
      FileReader inpFile = new FileReader(filePath);
      BufferedReader inpReader = new BufferedReader(inpFile);
      String inpLine = "", inpDelimiter = "";
      final String firstLineHeader = "Y";

      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
      }
      Long line = (long) 0;
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      while ((inpLine = inpReader.readLine()) != null) {
        line += 1;
        List<String> result = parseCSV(inpLine, inpDelimiter);

        if (log4j.isDebugEnabled())
          log4j.debug(result);

        if (result == null)
          continue;

        String fields[] = (String[]) result.toArray(new String[0]);

        for (int i = 0; i < fields.length; i++) {
          fields[i] = fields[i].replace("\"", "");
        }
        // Inserting Data into salesregion table
        isSuccess = insertIntoDepartment(fields, org, vars, line);
        log4j.debug("isSuccess" + isSuccess);
        if (isSuccess == 0) {
          break;
        }
        result.clear();
      }
      OBDal.getInstance().flush();
      inpReader.close();
      inpFile.close();

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Records Inserted Succesfully");
      }
    } catch (Exception e) {
      isSuccess = 0;
      e.printStackTrace();
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log4j.error("Exception in Product Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  @SuppressWarnings("unchecked")
  private int insertIntoDepartment(String[] fields, Organization org, VariablesSecureApp vars,
      Long line) {
    int status = 1;
    Locator locator = null;
    SalesRegion region = null;
    try {
      // select excempt tax
      Client objClient = OBContext.getOBContext().getCurrentClient();

      if (fields != null) {

        log4j.debug("locator" + locator);
        region = OBProvider.getInstance().get(SalesRegion.class);
        region.setClient(org.getClient());
        region.setOrganization(org);
        region.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        region.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        region.setSearchKey(fields[0]);
        region.setName(fields[1]);
        if (fields[2] != null)
          region.setDescription(fields[2]);
        OBDal.getInstance().save(region);
        log4j.debug("region" + region.getId());
        // End of sales
        return 1;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
      // TODO: handle exception
    }
    return 1;

  }

  // Parsing .csv file using regular Expression

  public List<String> parseCSV(String csv, String delim) {

    final Pattern NEXT_COLUMN = nextColumnRegex(delim);
    final List<String> strings = new ArrayList<String>();
    final Matcher matcher = NEXT_COLUMN.matcher(csv);

    while (!matcher.hitEnd() && matcher.find()) {
      String match = matcher.group(1);
      strings.add(match);
    }

    return strings;
  }

  private Pattern nextColumnRegex(String separator) {

    String unquoted = "(:?[^\"" + separator + "]|\"\")*";
    String ending = "(:?" + separator + "|$)";
    return Pattern.compile('(' + unquoted + ')' + ending);
  }

  public JSONObject processValidateCsvFile(String filePath, String inpOrgId) {
    JSONObject jsonresult = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      int lineNo = 1, duplicateline = 1;
      int failed = 0;

      FileReader inpFile = new FileReader(filePath);
      BufferedReader inpReader = new BufferedReader(inpFile);
      String inpLine = "", inpDelimiter = "";
      final String firstLineHeader = "Y";

      StringBuffer description = new StringBuffer();
      HashSet<String> lines = new HashSet<String>();
      HashSet<String> lines1 = new HashSet<String>();
      HashSet<String> lines2 = new HashSet<String>();
      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
      }
      String br = "";

      while ((inpLine = inpReader.readLine()) != null) {
        lineNo += 1;

        List<String> result = parseCSV(inpLine, inpDelimiter);
        log4j.debug(" result:- " + result);

        // if (log4j.isDebugEnabled())
        // log4j.debug(result);

        if (result == null)
          continue;
        String fields[] = (String[]) result.toArray(new String[0]);
        log4j.debug(" fields- " + fields.length);
        for (int i = 0; i < fields.length; i++) {
          fields[i] = fields[i].replace("\"", "");
        }
        log4j.debug("fields[0]:" + fields[0]);
        log4j.debug("fields[1]:" + fields[1]);
        // if(failed>1)
        description.append(br);
        if (fields.length < 3) {
          failed += 1;
          description.append("Partial record at line No.<b>" + lineNo
              + "</b> in the data file.<br>");
          br = "<br>";
        } else if (fields.length > 3) {
          failed += 1;
          description.append("Line No.<b>" + lineNo
              + "</b> in the data file has more fields than expected(3).<br>");
          br = "<br>";
        } else {
          br = "";
          log4j.debug(" StringUtils- " + StringUtils.isEmpty(fields[0]));
          if (StringUtils.isEmpty(fields[0])) {
            failed += 1;
            description.append("Department Code is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String depCode = fields[0];
            OBCriteria<SalesRegion> salListCriteria = OBDal.getInstance().createCriteria(
                SalesRegion.class);
            salListCriteria.add(Restrictions.ilike(SalesRegion.PROPERTY_SEARCHKEY, depCode));
            salListCriteria.setMaxResults(1);
            log4j.debug(" sallist:- " + salListCriteria);
            List<SalesRegion> sallist = salListCriteria.list();
            System.out.println(" sallist:- " + sallist.size());
            if (sallist.size() > 0) {// PriceList not found
              failed += 1;
              description.append("Already Same Department Code  <b>" + depCode
                  + "</b> is Present in Master Data  at line No.<b>" + lineNo + "</b><br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[1])) {
            failed += 1;
            description.append("Department Name  is empty at line no.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String depName = fields[1];
            OBCriteria<SalesRegion> salListCriteria = OBDal.getInstance().createCriteria(
                SalesRegion.class);
            salListCriteria.add(Restrictions.ilike(SalesRegion.PROPERTY_NAME, depName));
            salListCriteria.setMaxResults(1);
            log4j.debug(" sallist:- " + salListCriteria);
            List<SalesRegion> sallist = salListCriteria.list();
            System.out.println(" sallist123:- " + sallist.size());
            if (sallist.size() > 0) {// PriceList not found
              failed += 1;
              description.append("Already Same Department Name  <b>" + depName
                  + "</b> is Present in Master Data at line No.<b>" + lineNo + "</b><br>");
              br = "<br>";
            }
          }
          if (StringUtils.isNotEmpty(fields[0])) {
            // chk duplicate of depcode
            String depCode = fields[0];
            if (lines.add(depCode)) {

            } else {
              lines1.add(depCode);
              log4j.debug(" lines1:- " + lines1);
            }
          }
          if (StringUtils.isNotEmpty(fields[1])) {
            // chk duplicate of depname
            String depname = fields[1];
            if (lines.add(depname)) {

            } else {
              lines2.add(depname);
              log4j.debug(" lines1:- " + lines1);
            }
          }
        }
        result.clear();
      }
      for (String s : lines1) {
        description.append(br);
        failed += 1;
        description.append("Department Code: <b>" + s + " is duplicate </b> in the data file.<br>");
        br = "<br>";
      }
      for (String s : lines2) {
        description.append(br);
        failed += 1;
        description
            .append("Department Name : <b>" + s + " is duplicate </b> in the data file.<br>");
        br = "<br>";
      }

      if (failed > 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", Integer.toString(failed));
        jsonresult.put("statusMessage", description);
      } else {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", Integer.toString(failed));
        jsonresult.put("statusMessage", "CSV Validated Succesfully");
        System.out.println(" jsonresultF:- " + jsonresult);
      }
      inpReader.close();
      inpFile.close();

    } catch (Exception e) {
      log4j.error("Exception in Import Department CSV Validate", e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonresult;

  }

  public List<ImportDepartmentVO> getDepartment(String clientId, String roleId) {
    String sqlquery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject result = new JSONObject();
    List<ImportDepartmentVO> list = new ArrayList<ImportDepartmentVO>();
    ImportDepartmentVO vo = null;
    try {
      sqlquery = " select org.ad_org_id,org.value||'-'||org.name as value from ad_org org where( org.ad_client_id=  ? or org.ad_client_id='0') and org.AD_Org_ID  in (SELECT o.ad_org_id FROM ad_org o JOIN ad_orgtype ot ON o.ad_orgtype_id=ot.ad_orgtype_id "
          + " WHERE isready  ='Y' AND istransactionsallowed='Y' OR o.ad_org_id='0') and ad_org_id in ("
          + " select org.ad_org_id from ad_role_orgaccess orgaccess join ad_org org on org.ad_org_id = orgaccess.ad_org_id where orgaccess.ad_role_id =? and orgaccess.ad_client_id=? )";
      st = conn.prepareStatement(sqlquery);
      st.setString(1, clientId);
      st.setString(2, roleId);
      st.setString(3, clientId);
      log4j.debug("st:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        vo = new ImportDepartmentVO();
        vo.setOrgId(rs.getString("ad_org_id"));
        vo.setOrgName(rs.getString("value"));
        list.add(vo);
      }
      return list;
    } catch (Exception e) {
      log4j.error("Exception in getDepartment", e);
    }
    return list;
  }
}
