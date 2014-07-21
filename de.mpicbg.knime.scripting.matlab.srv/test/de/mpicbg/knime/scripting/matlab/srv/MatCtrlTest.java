package de.mpicbg.knime.scripting.matlab.srv;
import java.util.concurrent.ArrayBlockingQueue;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;


public class MatCtrlTest {
	
	public static void main(String[] args) throws MatlabConnectionException, InterruptedException, MatlabInvocationException {
		
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
		MatlabProxyFactory factory = new MatlabProxyFactory(options);
        final ArrayBlockingQueue<MatlabProxy> proxyQueue = new ArrayBlockingQueue<MatlabProxy>(1);
        factory.requestProxy(new MatlabProxyFactory.RequestCallback()
        {
            @Override
            public void proxyCreated(MatlabProxy proxy)
            {
                proxyQueue.add(proxy);
            }
        });
        
		MatlabProxy proxy = proxyQueue.take();
		proxy.eval("a = 104");
		Object res = proxy.getVariable("a");
		System.out.println(res);
		proxy.disconnect();
	}	

}
