package ameba.feature;

import ameba.mvc.template.httl.HttlMvcFeature;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Map;
import java.util.Properties;

/**
 * @author ICode
 * @since 13-8-23 下午8:10
 */
public class EmailFeature implements Feature {

    private static final Logger logger = LoggerFactory.getLogger(EmailFeature.class);
    //    mail.host.name=
    private static String hostName;
    //    mail.smtp.port=
    private static Integer smtpPort;
    //    mail.ssl.enable=true
    private static Boolean SSLEnabled = true;
    //    mail.user.name=
    private static String userName;
    //    mail.user.password=
    private static String userPassword;
    //    mail.from=
    private static String from;
    private static Properties templateProperties;

    public static String getHostName() {
        return hostName;
    }

    public static Integer getSmtpPort() {
        return smtpPort;
    }

    public static boolean isSSLEnabled() {
        return SSLEnabled;
    }

    public static String getUserName() {
        return userName;
    }

    public static String getUserPassword() {
        return userPassword;
    }

    public static String getFrom() {
        return from;
    }

    public static Properties getTemplateProperties() {
        return templateProperties;
    }

    @Override
    public boolean configure(FeatureContext context) {
        Map<String, Object> configuration = context.getConfiguration().getProperties();
        hostName = PropertiesHelper.getValue(configuration, "mail.host.name", String.class, null);
        if (StringUtils.isBlank(hostName)) {
            logger.warn("mail.host.name未设置");
        }

        smtpPort = PropertiesHelper.getValue(configuration, "mail.smtp.port", Integer.class, null);
        if (smtpPort == null) {
            logger.warn("mail.smtp.port未设置");
        }

        SSLEnabled = PropertiesHelper.getValue(configuration, "mail.ssl.enable", Boolean.class, null);

        userName = PropertiesHelper.getValue(configuration, "mail.user.name", String.class, null);
        if (StringUtils.isBlank(userName)) {
            logger.warn("mail.user.name未设置");
        }

        userPassword = PropertiesHelper.getValue(configuration, "mail.user.password", String.class, null);
        if (StringUtils.isBlank(userPassword)) {
            logger.warn("mail.user.password未设置");
        }

        from = PropertiesHelper.getValue(configuration, "mail.from", String.class, null);
        if (StringUtils.isBlank(from)) {
            logger.warn("mail.from未设置");
        }
        templateProperties = HttlMvcFeature.getTemplateProperties();
        for (String key : configuration.keySet()) {
            if (key.startsWith("mail.template.")) {
                templateProperties.put(key.replaceFirst("^mail\\.", ""), configuration.get(key));
                break;
            }
        }

        Emails.init();
        return true;
    }
}
