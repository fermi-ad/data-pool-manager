// $Id: DPMCredentials.java,v 1.12 2023/06/20 20:54:25 kingc Exp $
package gov.fnal.controls.servers.dpm;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

import gov.fnal.controls.kerberos.login.ServiceName;
import gov.fnal.controls.kerberos.KerberosLoginContext;

public class DPMCredentials
{
    private static final Pattern PRINCIPAL_NAME_PATTERN = Pattern.compile("^(.*?)/([^/]+)@.+$");

	static final private List<KerberosPrincipal> kerberosServicePrincipals = new ArrayList<>();

	static void init()
	{
		System.setProperty("gov.fnal.controls.kerberos.service", "true");

		if (System.getProperty("gov.fnal.controls.kerberos.keytab") == null)
			System.setProperty("gov.fnal.controls.kerberos.keytab", "/usr/local/etc/daeset");

		final KerberosLoginContext kerberosLoginContext = KerberosLoginContext.getInstance();

		try {
			kerberosLoginContext.login();

			for (KerberosPrincipal kerberosPrincipal : kerberosLoginContext.getSubject().getPrincipals(KerberosPrincipal.class))
				kerberosServicePrincipals.add(kerberosPrincipal);
		} catch (Exception e) {
			logger.log(Level.WARNING, "kerberos login failed", e);
		}
	}

	private DPMCredentials() { }

	static public GSSName serviceName() throws GSSException
	{
		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

        final String principalName = kerberosServicePrincipals.get(0).getName();
        final Matcher matcher = PRINCIPAL_NAME_PATTERN.matcher(principalName);

        if (!matcher.matches())
			throw new GSSException(GSSException.NO_CRED);

		final GSSManager manager = GSSManager.getInstance();
		final String name = matcher.group(1) + "@" + matcher.group(2);

		logger.log(Level.FINE, "service name: '" + name + "'");

		return manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
	}

	static GSSContext createContext() throws GSSException
	{
		final KerberosLoginContext kerberosLoginContext = KerberosLoginContext.getInstance();

		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

		return kerberosLoginContext.createAcceptorContext(new ServiceName(kerberosServicePrincipals.get(0)));
	}

	@Override
	public String toString()
	{
		return KerberosLoginContext.getInstance().toString();
	}

	public static void main(String[] args) throws GSSException
	{
		DPMCredentials.init();
		System.out.println("Service Name: " + serviceName());
	}
}

