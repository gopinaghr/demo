package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.hcm.EmploymentInfo;

/**
 * 
 * 
 * 
 */

@SuppressWarnings("serial")
public class EscmBiddatesCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String inpquesdate = vars.getStringParameter("inpquelastdate");

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    Connection conn = OBDal.getInstance().getConnection();
    OBQuery<EmploymentInfo> empInfo = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    try {

      if (inpLastFieldChanged.equals("inpquelastdate")) {
        st = OBDal
            .getInstance()
            .getConnection()
            .prepareStatement(
                "select to_char(eut_convertto_gregorian('" + inpquesdate
                    + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpcorgredate", rs.getString("eut_convertto_gregorian"));

        }
      }
    }

    catch (Exception e) {
      log4j.error("Exception in EscmBiddatesCallout  :", e);
      throw new OBException(e);
    }

  }
}
