/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ga.gramadóir;



import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.uno.XComponentContext;

/**
 * This class is a factory that creates only a single instance,
 * or a singleton, of the Main class. Used for performance
 * reasons and to allow various parts of code to interact.
 *
 * @author Ciarán Campbell
 */
public class SingletonFactory implements XSingleComponentFactory, XServiceInfo {

  private transient ga.gramadóir.Main instance;

  @Override
  public final Object createInstanceWithArgumentsAndContext(final Object[] arguments,
      final XComponentContext xContext) throws com.sun.star.uno.Exception {
    return createInstanceWithContext(xContext);
  }

  @Override
  public final Object createInstanceWithContext(final XComponentContext xContext) throws com.sun.star.uno.Exception {
    if (instance == null) {
      instance = new ga.gramadóir.Main(xContext);
    } else {
      instance.changeContext(xContext);
    }
    return instance;
  }

  @Override
  public final String getImplementationName() {
    return Main.class.getName();
  }

  @Override
  public final boolean supportsService(String serviceName) {
    for (String s : getSupportedServiceNames()) {
      if (s.equals(serviceName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final String[] getSupportedServiceNames() {
    return Main.getServiceNames();
  }
}

