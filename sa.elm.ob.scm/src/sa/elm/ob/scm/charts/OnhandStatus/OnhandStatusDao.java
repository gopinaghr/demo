package sa.elm.ob.scm.charts.OnhandStatus;

import java.sql.Connection;

import org.apache.log4j.Logger;

public class OnhandStatusDao {

  Connection conn = null;
  private static Logger log4j = Logger.getLogger(OnhandStatusDao.class);

  public OnhandStatusDao(Connection con) {
    this.conn = con;
  }
}