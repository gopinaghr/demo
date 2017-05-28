package sa.elm.ob.scm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(SCMComponentProvider.PROVIDER)
public class SCMComponentProvider extends BaseComponentProvider {

  public static final String PROVIDER = "ESCM_Provider";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/copyRecord.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/PrintReport.js", false));

    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/copy_Record.css", false));
    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/print_pdf.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/IrtabDisable.js", false));

    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-porecqtyvalidation.js",
        true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-datevalidation.js", true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.scm/js/ob-escm-requisitionlinecancel.js", true));
    return globalResources;
  }
}
