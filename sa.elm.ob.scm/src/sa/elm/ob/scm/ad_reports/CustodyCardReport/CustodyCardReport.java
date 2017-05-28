package sa.elm.ob.scm.ad_reports.CustodyCardReport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * @author Divya on 23/03/2017
 * 
 */
public class CustodyCardReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/CustodyCardReport/CustodyCardReport.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      OBContext.setAdminMode();
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? "" : request
          .getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      String inpClientId = "";
      inpClientId = vars.getClient();
      String inpOrgId = vars.getOrg();
      PreparedStatement st = null;
      ResultSet rs = null;
      JSONArray jsonArray = null;
      log4j.debug("action");
      if (action.equals("")) {
        List<CustodyCardReportVO> ls = new ArrayList<CustodyCardReportVO>();
        CustodyCardReportVO vo = null;
        String sql = "";
        if (vars.getLanguage().equals("ar_SA")) {
          sql = "select coalesce(tr.name,list.name) as name ,list.value ";
        } else {
          sql = "select list.name ,list.value ";
        }
        sql = sql
            + " from ad_ref_list list left join ad_ref_list_trl tr on list.ad_ref_list_id = tr.ad_ref_list_id where ad_reference_id='E585F9EEA3024736B3E30F9F6A7C9A09' and list.value  in ('D','E','S') order by list.seqno ";
        st = con.prepareStatement(sql);
        rs = st.executeQuery();
        while (rs.next()) {
          vo = new CustodyCardReportVO();
          vo.setBeneficiaryname(rs.getString("name"));
          vo.setBeneficiaryvalue(rs.getString("value"));
          ls.add(vo);
        }

        request.setAttribute("inpBeneficiaryTypeList", ls);

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBeneficiaryType = request.getParameter("inpBeneficiarytype");
        String inpBeneficiaryId = request.getParameter("inpBeneficiaryid");

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpBeneficiaryType", inpBeneficiaryType);
        parameters.put("inpBeneficiaryId", inpBeneficiaryId);
        parameters.put("inpAD_Client_ID", inpClientId);
        parameters.put("inpAD_Org_ID", inpOrgId);

        strReportName = "@basedesign@/sa/elm/ob/scm/ad_reports/CustodyCardReport/CustodyCardReport.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } /*
         * else if (action.equals("getbeneficiary")) { JSONObject jsonResponse = null; String type =
         * request.getParameter("inptype"); jsonArray = new JSONArray();
         * 
         * st = con .prepareStatement(
         * " select escm_beneficiary_v_id as id ,name from escm_beneficiary_v where btype= ?  and ad_org_id in( select ad_org_id  from ad_role_orgaccess   where ad_role_id  = ? and ad_client_id = ? )  "
         * + "and ad_client_id=?  order by value asc "); st.setString(1, type); st.setString(2,
         * vars.getRole()); st.setString(3, inpClientId); st.setString(4, inpClientId); rs =
         * st.executeQuery(); log4j.debug("getbeneficiary:" + st.toString()); while (rs.next()) {
         * jsonResponse = new JSONObject(); jsonResponse.put("id", rs.getString("id"));
         * jsonResponse.put("name", rs.getString("name")); jsonArray.put(jsonResponse); }
         * 
         * response.setCharacterEncoding("UTF-8"); response.getWriter().write(jsonArray.toString());
         * }
         */

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in CustodyCardReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}