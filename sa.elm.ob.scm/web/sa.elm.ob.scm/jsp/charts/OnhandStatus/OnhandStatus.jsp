<!--
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): halgadaibi
 ************************************************************************
-->
<%@page import="org.openbravo.client.myob.WidgetURL"%>
<%@page import="org.openbravo.client.myob.WidgetInstance"%>
<%@page import="org.openbravo.dal.core.OBContext"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="sa.elm.ob.finance.properties.Resource"
    errorPage="/web/jsp/ErrorPage.jsp"%>
<%
  String textDir = "LTR";
            String right = "right";
            String left = "left";
            String lang = ((String) session.getAttribute("#AD_LANGUAGE"));
            String style = "../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
            if (lang.equals("ar_SA") || lang.equals("ar")) {
                // style = "../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
                textDir = "RTL";
                right = "left";
                left = "right";
            }
%>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<TITLE>Onhand Status</TITLE>
<LINK rel="shortcut icon" href="../web/images/favicon.ico"
    type="image/x-icon"></LINK>
<LINK rel="stylesheet" type="text/css" href="<%=style%>" id="paramCSS"></LINK>
<script type="text/javascript" id="paramLanguage">var defaultLang="<%=((String) session.getAttribute("#AD_LANGUAGE"))%>";</script>

<style type="text/css">
</style>

  <title>title</title>
  <script src="../web/sa.elm.ob.scm/js/highcharts.js"></script>
  <script src="../web/sa.elm.ob.scm/js/exporting.js"></script>
  <script src="../web/sa.elm.ob.scm/js/drilldown.js"></script>
  <script src="../web/sa.elm.ob.scm/js/jquery-3.2.0.min.js"></script>
  
</head>
<body>

 <div class="radio col-md-offset-5" dir="rtl" align="center">
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios1" value="option1" checked>
    1
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios2" value="option2">
    2
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios3" value="option3">
    3
  </label>
</div>

<div id="container" style="min-width: 310px; height: 400px; margin: 0 auto"></div>

  <script>

  var chart;

  drawChart('../web/sa.elm.ob.scm/json/OnhandStatus/dataDrill.json');


  function drawChart(file){
      var options = {
          chart: {
              renderTo: 'container',
              type: 'column'
          },
          plotOptions: {
                column: {
                    pointPadding: 0.05,
                    borderWidth: 0
                }
            },
          title: {
            text: 'Per Years'
          },
          xAxis: {
            type: 'category',
            title: {
              text: "Years"
            }
          },
          yAxis: {
            min: 0,
            title: {
              text: "Counts"
            }
          },
          series: [{}],
          drilldown:{series:[{}]}
      };

      $.getJSON(file, function(json) {
          options.series[0] = json[0];
          options.series[0].colors = ['#d5d5d5', '#0f97a6'];
          options.drilldown.series[0] = json[1];
          options.drilldown.series[1] = json[2];
          chart = new Highcharts.Chart(options);

      });
}
  </script>
</body>
<script type="text/javascript">
</script>
</HTML>