package helpers;

import net.sf.cglib.proxy.Enhancer;
import org.assertj.core.api.AbstractSoftAssertions;
import org.assertj.core.api.SoftAssertionError;
import org.assertj.core.util.VisibleForTesting;

import java.util.List;


public class SoftAssertions extends AbstractSoftAssertions  {

public Boolean isChecked = false;

    ErrorCollector myCollector;

    public SoftAssertions() {
        myCollector = new ErrorCollector();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T, V> V proxy(Class<V> assertClass, Class<T> actualClass, T actual) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(assertClass);
        enhancer.setCallback(myCollector);
        Class[] argumentTypes = org.assertj.core.util.Arrays.array(actualClass);
        Object[] arguments = org.assertj.core.util.Arrays.array(actual);
        return (V) enhancer.create(argumentTypes, arguments);
    }


    public void assertAll() {
        List<Throwable> errors = myCollector.errorsAsList();
//        for (e in errors) {
//            e.printStackTrace()
//        }
        if (!errors.isEmpty()) {
            throw new SoftAssertionError(org.assertj.core.groups.Properties.extractProperty("message", String.class).from(errors));
        }
    }

    public Boolean checkIsEmptyAndClear() {
        isChecked = true;
        List<Throwable> errors = myCollector.errorsAsList();
        Boolean result = errors.isEmpty();
        myCollector.errors().clear();
        return result;
    }

//    public Statement apply(final Statement base, Description description) {
//        return new Statement() {
//            @Override
//            public void evaluate() throws Throwable {
//                base.evaluate();
//                MultipleFailureException.assertEmpty(myCollector.errorsAsList());
//            }
//        };
//    }

    @VisibleForTesting
    ErrorCollector getMyCollector() {
        return myCollector;
    }


}
