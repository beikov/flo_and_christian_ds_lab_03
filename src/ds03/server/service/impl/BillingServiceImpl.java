package ds03.server.service.impl;

import java.rmi.RemoteException;
import java.util.Properties;

import ds03.server.service.BillingService;
import ds03.server.service.BillingServiceSecure;
import ds03.server.util.PasswordUtils;
import ds03.server.util.PropertiesUtils;
import ds03.util.RegistryUtils;

public class BillingServiceImpl implements BillingService {

	private static final long serialVersionUID = 1L;
	private static final Properties users = PropertiesUtils
			.getProperties("user.properties");
	private static final BillingServiceSecure BILLING_SERVICE_SECURE = RegistryUtils
			.exportObject(new BillingServiceSecureImpl());

	@Override
	public BillingServiceSecure login(String username, String password)
			throws RemoteException {
		if (username == null || username.isEmpty()) {
			throw new RemoteException("Invalid username given");
		}
		if (password == null || password.isEmpty()) {
			throw new RemoteException("Invalid password given");
		}

		if (!PasswordUtils.matches(password, users.getProperty(username))) {
			return null;
		}

		return BILLING_SERVICE_SECURE;
	}

}
