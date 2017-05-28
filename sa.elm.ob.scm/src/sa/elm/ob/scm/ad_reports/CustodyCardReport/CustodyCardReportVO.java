package sa.elm.ob.scm.ad_reports.CustodyCardReport;

public class CustodyCardReportVO {

  public String beneficiaryvalue;
  public String beneficiaryname;
  public String beneficiaryId;
  public String roleId;
  public String userId;

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  public void setBeneficiaryId(String beneficiaryId) {
    this.beneficiaryId = beneficiaryId;
  }

  public String getBeneficiaryvalue() {
    return beneficiaryvalue;
  }

  public void setBeneficiaryvalue(String beneficiaryvalue) {
    this.beneficiaryvalue = beneficiaryvalue;
  }

  public String getBeneficiaryname() {
    return beneficiaryname;
  }

  public void setBeneficiaryname(String beneficiaryname) {
    this.beneficiaryname = beneficiaryname;
  }

}
