<%@page import="sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
    <%@ page import="java.util.List,java.util.ArrayList" %>
    <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));

     List<CustodyCardReportVO> warehouselist = (ArrayList<CustodyCardReportVO>) request.getAttribute("inpwarehouseList");
     List<CustodyCardReportVO> productlist = (ArrayList<CustodyCardReportVO>) request.getAttribute("inpproductList");

    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    toolBarStyle="../web/js/common/CommonFormRtl.css";
    }
    %>
<HTML xmlns="http://www.w3.org/1999/xhtml">
<HEAD>
<TITLE><%= Resource.getProperty("scm.itemcontrolcard.report",lang) %></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>        
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/searchs.js"></script>
<script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
<script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
<script type="text/javascript" src="../web/js/default/DateTextBox.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/calendar.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/lang/calendar-lang.js"></script>   
 <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
<style type="text/css">
  .ui-autocomplete-input {height: 21px; border-radius:0px !important; width:252px;font-size: 12px; padding: 0 0 0 3px; float: left;border:1px solid #CDD7BB;}
   .ui-button-icon-primary{left: .9em !important;} 
    ul{border:1px solid #FF9C30 !important;}      
</style>               
<script type="text/javascript">
function validate(action) {
    return true;
}
function onLoadDo() {
    /*<<<<<OB Code>>>>>*/
    this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
    setWindowTableParentElement();
    this.tabsTables = new Array(new tabTableId('tdtopTabs'));
    setTabTableParentElement();
    setBrowserAutoComplete(false);
    setFocusFirstControl(null, 'inpMailTemplate');
    resizeArea();
    updateMenuIcon('buttonMenu');
    /*<<<<<OB Code>>>>>*/
}
function onResizeDo() {
    resizeArea();
}
</script>
</HEAD>
   <BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
      <FORM id="form" method="post" action="" name="frmMain">
         <INPUT type="hidden" name="Command"></INPUT>
         <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
         <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
         <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
         <INPUT type="hidden" name="inpWarehouseid" id="inpWarehouseid" value=""></INPUT>
         <INPUT type="hidden" name="inpProductid" id="inpProductid" value=""></INPUT>
         
         <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
            <TR>
               <TD valign="top">
                  <DIV class="Main_ContentPane_NavBar" id="tdtopButtons">
                     <TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE>
                  </DIV>
                   </TD>
           <TD valign="top">
               <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Left">
                 <TR><TD class="Main_NavBa r_bg_left" id="tdleftSeparator"></TD></TR>
                 <TR><TD class="Main_ToolBar_bg_left" valign="top"></TD></TR>
                 <TR><TD class="Main_Client_bg_left"></TD></TR>
                 <TR><TD class="Main_Bottom_bg_left"></TD></TR>
               </TABLE>
           </TD> 
           <TD valign="top">
                  <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
                     <table class="Main_ContentPane_ToolBar Main_ToolBar_bg"
                        id="tdToolBar">
                        <tr>
                           <td class="Main_ToolBar_Separator_cell"><img
                              src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                           <td width="2%"><a href="javascript:void(0);"
                              onClick="onClickRefresh()" class="Main_ToolBar_Button"
                              onMouseOver="window.status='Reload Current Page';return true;"
                              onMouseOut="window.status='';return true;"
                              onclick="this.hideFocus=true" onblur="this.hideFocus=false"
                              id="buttonRefresh"><img
                              class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh"
                              src="../web/images/blank.gif" title="finance.reload"
                              border="0" id="linkButtonRefresh"></a></td>
                           <td class="Main_ToolBar_Separator_cell"><img
                              src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                           <td class="Main_ToolBar_Space"></td>
                        </tr>
                     </table>
                  </DIV>
                  <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
                     <TR id="paramParentTabContainer">
                        <td class="tabBackGroundInit"></td>
                     </TR>
                     <TR id="paramMainTabContainer">
                        <td class="tabBackGroundInit"><span style="background: none;" class="tabTitle"></span></td>
                     </TR>
                     <tr>
                        <td class="tabBackGround">
                           <div class="marginLeft">
                              <div>
                                 <span class="dojoTabcurrentfirst">
                                    <div>
                                       <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;">
                                       <%= Resource.getProperty("scm.itemcontrolcard.report",lang).replace("'", "\\\'") %>
                                       </a></span>
                                    </div>
                                 </span>
                              </div>
                           </div>
                        </td>
                     </tr>
                  </TABLE>
                  <DIV class="Main_ContentPane_Client" style="overflow: auto; display: none;" id="client">
                     <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" >
                        <TR>
                           <TD>
                              <TABLE class="dojoTabPaneWrapper" align="right" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                 <TR>
                                    <TD>
                                       <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox">
                                          <TBODY>
                                             <TR class="MessageBox_TopMargin" align="right">
                                                <TD class="MessageBox_LeftMargin"></TD>
                                                <TD></TD>
                                                <TD class="MessageBox_RightMargin"></TD>
                                             </TR>
                                             <TR>
                                                <TD class="MessageBox_LeftMargin"></TD>
                                                <TD>
                                                   <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container">
                                                      <TBODY>
                                                         <TR>
                                                            <TD class="MessageBox_LeftTrans">
                                                               <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
                                                                  <TBODY>
                                                                     <TR>
                                                                        <TD class="MessageBox_TopLeft"></TD>
                                                                     </TR>
                                                                     <TR>
                                                                        <TD class="MessageBox_Left"></TD>
                                                                     </TR>
                                                                  </TBODY>
                                                               </TABLE>
                                                            </TD>
                                                            <TD class="MessageBox_bg">
                                                               <TABLE class="MessageBox_Top">
                                                                  <TBODY>
                                                                     <TR>
                                                                        <TD>
                                                                           <SPAN>
                                                                              <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell">
                                                                                 <TBODY>
                                                                                    <TR>
                                                                                       <TD class="MessageBox_Icon_ContentCell">
                                                                                          <DIV class="MessageBox_Icon"></DIV>
                                                                                       </TD>
                                                                                       <TD style="vertical-align: top;" id="messageBoxIDContent">
                                                                                          <SPAN>
                                                                                             <DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV>
                                                                                             <DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV>
                                                                                             <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                          </SPAN>
                                                                                       </TD>
                                                                                    </TR>
                                                                                 </TBODY>
                                                                              </TABLE>
                                                                           </SPAN>
                                                                           <div id="hideMessage" style="float: right; margin-top:-13px; margin-right:10px;">
                                                                              <a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';"><%= Resource.getProperty("finance.hide",lang) %></a>
                                                                           </div>
                                                                        </TD>
                                                                     </TR>
                                                                  </TBODY>
                                                               </TABLE>
                                                            </TD>
                                                            <TD class="MessageBox_RightTrans">
                                                               <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
                                                                  <TBODY>
                                                                     <TR>
                                                                        <TD class="MessageBox_TopRight"></TD>
                                                                     </TR>
                                                                     <TR>
                                                                        <TD class="MessageBox_Right"></TD>
                                                                     </TR>
                                                                  </TBODY>
                                                               </TABLE>
                                                            </TD>
                                                         </TR>
                                                         <TR>
                                                            <TD rowspan="2" class="MessageBox_BottomLeft"></TD>
                                                            <TD class="MessageBox_BottomTrans MessageBox_bg"></TD>
                                                            <TD rowspan="2" class="MessageBox_BottomRight"></TD>
                                                         </TR>
                                                         <TR>
                                                            <TD class="MessageBox_Bottom"></TD>
                                                         </TR>
                                                      </TBODY>
                                                   </TABLE>
                                                </TD>
                                                <TD class="MessageBox_RightMargin"></TD>
                                             </TR>
                                             <TR class="MessageBox_BottomMargin">
                                                <TD class="MessageBox_LeftMargin"></TD>
                                                <TD></TD>
                                                <TD class="MessageBox_RightMargin">
                                                </TD>
                                             </TR>
                                          </TBODY>
                                       </TABLE>
                                       <div id="LoadingContent" style="display:none;">
                                          <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                                          <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading",lang).replace("'", "\\\'") %> </div>
                                       </div>
                                       <TABLE  align="center" width="100%">
                                          <TR>
                                             <TD>&nbsp;</TD>
                                          </TR>
                                            <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpProductIdLabel"> <b> <%=Resource.getProperty("scm.itemcontrolcard.itemcode", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpProductId" class="ComboKey Combo_TwoCells_width" ">
                                                  <option value="0"><%= Resource.getProperty("scm.--select--",lang).replace("'", "\\\'") %></option>
                                                    <%   if(productlist.size() > 0){
                                                      for(CustodyCardReportVO vo:productlist){ %>
                                                   <option value='<%= vo.getBeneficiaryId()%>' ><span><%= vo.getBeneficiaryname()%></span></option>
                                                   <%}}%>  
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpWarehouseIdLabel"> <b> <%=Resource.getProperty("scm.itemcontrolcard.warehouse", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpWarehouseId" class="Combo Combo_TwoCells_width">
                                                 <option value="0"><%= Resource.getProperty("scm.--select--",lang).replace("'", "\\\'") %></option>
                                                   <%   if(warehouselist.size() > 0){
                                                      for(CustodyCardReportVO vo:warehouselist){ %>
                                                   <option value='<%= vo.getBeneficiaryId()%>' ><span><%= vo.getBeneficiaryname()%></span></option>
                                                   <%}}%>
                                                </select>
                                             </td>
                                          </tr>
                                          <TR  align = "center">
                                             <td></td>
                                             <TD  id="submitButton">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="generateReport();" style="margin: 10px 0;">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("scm.printreport.submit",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                             </TD>
                                          </TR>
                                       </TABLE>
                                    </TD>
                                 </TR>
                              </TABLE>
                           </TD>
                        </TR>
                     </TABLE>
                  </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
                 <TR>
                   <TD class="Main_Bottom_bg"><IMG src="../web/images/blank.gif" border="0"></IMG></TD>
                 </TR>
               </TABLE>
           </TD>
               <TD valign="top">
                   <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Right" id="tdrightSeparator">
                     <TR><TD class="Main_NavBar_bg_right"></TD></TR>
                     <TR><TD class="Main_ToolBar_bg_right" valign="top"></TD></TR>
                     <TR><TD class="Main_Client_bg_right"></TD></TR>
                     <TR><TD class="Main_Bottom_bg_right"></TD></TR>
                   </TABLE>
               </TD>
            </TR>
         </TABLE>
      </FORM>
   </BODY>
<script type="text/javascript">onLoadDo();</script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.button.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/datejs/jquery-ui.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.autocomplete.js"></script>  
 <script type="text/javascript" src="../web/js/common/select2.min.js"></script> 
<script type="text/javascript">

function reloadWindow() {
    submitCommandForm('DEFAULT', false, null, 'ItemControlCard', '_self', null, false);
    return false;
}
function onClickRefresh() {
    document.getElementById("inpAction").value = "";
    reloadWindow();
}

 //Process to call the respective report.
function generateReport() {
    document.getElementById("inpAction").value = "Submit";  
    document.getElementById("inpWarehouseid").value = document.getElementById("inpWarehouseId").value;    
    document.getElementById("inpProductid").value = document.getElementById("inpProductId").value;

    if (document.getElementById("inpProductid").value == 0) {
        OBAlert('<%=Resource.getProperty("scm.Itemcard.select.item",lang).replace("'","\\\'") %>');
        return false;
    }
    openServletNewWindow('PRINT_PDF', true, null, 'ItemControlCard', null, false, '700', '1000', true);
    return false;
}
(function( $ ) {
    $.widget( "ui.combobox", {
        _create: function() {
            var self = this,select = this.element.hide(),selected = select.children( ":selected" ),value = selected.val() ? selected.text() : "";
            var input = this.input = $( "<input  placeholder=\"<%= Resource.getProperty("scm.select", lang) %>\">" )
                .insertAfter( select )
                .val( value )
                .css('background-color', '#FFC')
                .autocomplete({
                        delay: 0,
                        minLength: 0,
                        source: function( request, response ) {
                            var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
                            response( select.children( "option" ).map(function() {
                                var text = $( this ).text();                                
                                if ( this.value && ( !request.term || matcher.test(text) ) )
                                    return {
                                        label: text.replace(
                                                new RegExp("(?![^&;]+;)(?!<[^<>]*)(" +
                                                        $.ui.autocomplete.escapeRegex(request.term) +
                                                        ")(?![^<>]*>)(?![^&;]+;)", "gi"), 
                                                        "<strong>$1</strong>" ),
                                                        value: text,
                                                        option: this};
                                }) );
                            },
                        select: function( event, ui ) {
                            ui.item.option.selected = true;
                            self._trigger( "selected", event, {
                                item: ui.item.option
                                });                                                                         
                        },
                        change: function( event, ui ) {
                            if($(select).attr("id")=='inpProductId')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpProductId').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpProductId > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpProductId').next().val( "" );
                            }   
                            
                        },
                         search: function( event, ui ) { 
                                if($(select).attr("id")=='inpProductId'  )
                                {           
                                    if($("#inpProductId").next().val()=="")
                                        $("#inpProductId").val("");
                                }  
                    
                      }  
             
                        
                });
            if($(select).attr("id")=='inpProductId')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" );       
            else
                input.addClass( "ui-widget ui-widget-content autocomplete-widget-content ui-corner-left autocomp_hght autocomp_wdth" );
            input.data( "autocomplete" )._renderItem = function( ul, item ) {
                return $( "<li></li>" )
                .data( "item.autocomplete", item )
                .append( "<a>" + item.label + "</a>" )
                .appendTo( ul );
            };
            input.click(function() {
                this.select();
            });
            this.div = $("<div class='divComboImage'></div>")
                    .insertAfter( input )
                    .append(  $( "<button type='button'>&nbsp;</button>" )                              
                        .css({'padding-top':"0px","height":"21px"}) 
                        .attr( "tabIndex", -1 )
                        .attr( "title", "Show All Items" )                      
                        .button({icons: {primary: "ui-icon-triangle-1-s"},text: false})
                        .removeClass( "ui-corner-all" )
                        .addClass( "ui-corner-right ui-button-icon" )
                        .addClass("ui_flip_image ")
                        .click(function() {
                            if ( input.autocomplete( "widget" ).is( ":visible" ) ) {
                                input.autocomplete( "close" );
                                return;
                            }
                            input.autocomplete( "search", "" );
                            input.focus();  
                        }) 
                    )
                    .css({'float':'right'});
            },
            
            destroy: function() {
                this.input.remove();
                this.div.remove();
                this.element.show();
                $.Widget.prototype.destroy.call( this );
            }
        
    });
    
    
})( jQuery ); 
//$( "#inpWarehouseId").select2(selectBoxAjaxPaging({size: "small",placeholder: true}));
$( "#inpProductId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$("#inpWarehouseId").select2(selectBoxAjaxPaging({
    size : "med/popup"
}));
document.getElementById("select2-inpWarehouseId-container").style.backgroundColor="#F5F7F1";

   </script>
</HTML>