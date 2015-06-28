package ameba.feature;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import ameba.lib.Akka;
import httl.Engine;
import httl.Template;
import org.apache.commons.mail.*;

import java.io.IOException;
import java.text.ParseException;

import static ameba.feature.Emails.MessageType.HTML;
import static ameba.feature.Emails.MessageType.TEXT;

/**
 * @author icode
 */
public class Emails {
    private static Engine engine;
    private static String directory;
    private static String charset;
    private static ActorRef actor;

    private Emails() {
    }

    private static void configure(Email email) throws EmailException {
        email.setHostName(EmailFeature.getHostName());
        email.setSmtpPort(EmailFeature.getSmtpPort());
        email.setAuthenticator(new DefaultAuthenticator(EmailFeature.getUserName(), EmailFeature.getUserPassword()));
        email.setSSLOnConnect(EmailFeature.isSSLEnabled());
        email.setFrom(EmailFeature.getFrom());
        email.setCharset(charset);
    }

    public static String renderTemplate(String tplPath, Object bean) throws IOException, ParseException {
        Template template = engine.getTemplate(directory + tplPath);
        return (String) template.evaluate(bean);
    }

    public static void send(Email email) throws EmailException {
        configure(email);
        email.send();
    }

    public static void asyncSend(Message message) {
        actor.tell(message, ActorRef.noSender());
    }

    public static void asyncSend(TemplateMessage message) {
        actor.tell(message, ActorRef.noSender());
    }

    /**
     * 发送文本邮件
     *
     * @param subject 主题
     * @param message 消息
     * @param tos     收件人
     * @throws EmailException
     */
    public static void sendText(String subject, String message, String[] tos) throws EmailException {
        Email email = new SimpleEmail();

        email.setSubject(subject);
        email.setMsg(message);
        email.addTo(tos);

        send(email);
    }

    /**
     * 异步发送文本邮件
     *
     * @param subject 主题
     * @param message 消息
     * @param tos     收件人
     */
    public static void asyncSendText(String subject, String message, String[] tos) {
        asyncSend(Message.create(TEXT, subject, message, tos));
    }

    /**
     * 发送文本邮件
     *
     * @param subject 主题
     * @param tpl     消息模板
     * @param bean    模型
     * @param tos     收件人
     * @throws EmailException
     */
    public static void sendText(String subject, String tpl, Object bean, String[] tos)
            throws IOException, ParseException, EmailException {
        Email email = new SimpleEmail();

        email.setMsg(renderTemplate(tpl, bean));
        email.setSubject(subject);
        email.addTo(tos);

        send(email);
    }

    /**
     * 异步发送文本邮件
     *
     * @param subject 主题
     * @param tpl     消息模板
     * @param bean    模型
     * @param tos     收件人
     */
    public static void asyncSendText(String subject, String tpl, Object bean, String[] tos) throws EmailException {
        SimpleEmail email = new SimpleEmail();
        email.setSubject(subject);
        email.addTo(tos);
        asyncSend(TemplateMessage.create(email, tpl, bean));
    }

    /**
     * 发送HTML邮件
     *
     * @param subject 主题
     * @param tpl     消息模板
     * @param bean    模型
     * @param tos     收件人
     * @throws EmailException
     */
    public static void sendHtml(String subject, String tpl, Object bean, String[] tos)
            throws EmailException, IOException, ParseException {
        HtmlEmail email = new HtmlEmail();

        email.setMsg(renderTemplate(tpl, bean));
        email.setSubject(subject);
        email.addTo(tos);

        send(email);
    }

    /**
     * 异步发送HTML邮件
     *
     * @param subject 主题
     * @param tpl     消息模板
     * @param bean    模型
     * @param tos     收件人
     */
    public static void asyncSendHtml(String subject, String tpl, Object bean, String[] tos) throws EmailException {
        HtmlEmail email = new HtmlEmail();
        email.setSubject(subject);
        email.addTo(tos);
        asyncSend(TemplateMessage.create(email, tpl, bean));
    }

    /**
     * 发送HTML邮件
     *
     * @param subject 主题
     * @param message 消息
     * @param tos     收件人
     * @throws EmailException
     */
    public static void sendHtml(String subject, String message, String[] tos) throws EmailException {
        HtmlEmail email = new HtmlEmail();

        email.setSubject(subject);
        email.setMsg(message);
        email.addTo(tos);

        send(email);
    }

    /**
     * 异步发送HTML邮件
     *
     * @param subject 主题
     * @param message 消息
     * @param tos     收件人
     */
    public static void asyncSendHtml(String subject, String message, String[] tos) {
        asyncSend(Message.create(HTML, subject, message, tos));
    }

    static void init() {
        if (engine == null) {
            synchronized (Emails.class) {
                if (engine == null) {
                    engine = Engine.getEngine("mailTemplate", EmailFeature.getTemplateProperties());
                    directory = EmailFeature.getTemplateProperties().getProperty("template.directory");
                    if (!directory.endsWith("/")) {
                        directory += "/";
                    }
                    charset = EmailFeature.getTemplateProperties().getProperty("output.encoding");
                    actor = Akka.system().actorOf(Props.create(SendWorker.class));
                }
            }
        }
    }

    public enum MessageType {
        TEXT,
        HTML,
        IMG
    }

    public static class Message {
        private MessageType type;
        private String content;
        private String subject;
        private String[] tos;

        private static Message create(MessageType type, String subject, String content, String[] tos) {
            Message msg = new Message();
            msg.type = type;
            msg.subject = subject;
            msg.content = content;
            msg.tos = tos;
            return msg;
        }

        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String[] getTos() {
            return tos;
        }

        public void setTos(String[] tos) {
            this.tos = tos;
        }
    }

    public static class TemplateMessage {
        private MessageType type;
        private String template;
        private Object model;
        private Email email;

        private static TemplateMessage create(Email email, String template) {
            return create(email, template, null);
        }

        private static TemplateMessage create(Email email, String template, Object model) {
            MessageType type = MessageType.TEXT;
            if (email instanceof ImageHtmlEmail) {
                type = MessageType.IMG;
            } else if (email instanceof HtmlEmail) {
                type = MessageType.HTML;
            }
            return create(email, type, template, model);
        }

        private static TemplateMessage create(Email email, MessageType type, String template) {
            return create(email, type, template, null);
        }

        private static TemplateMessage create(Email email, MessageType type, String template, Object model) {
            TemplateMessage msg = new TemplateMessage();
            msg.type = type;
            msg.template = template;
            msg.model = model;
            msg.email = email;
            return msg;
        }

        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }

        public Object getModel() {
            return model;
        }

        public void setModel(Object model) {
            this.model = model;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public Email getEmail() {
            return email;
        }

        public void setEmail(Email email) {
            this.email = email;
        }
    }

    private static class SendWorker extends UntypedActor {

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof Message) {
                Message msg = (Message) message;
                switch (msg.getType()) {
                    case HTML:
                        sendHtml(msg.getSubject(), msg.getContent(), msg.getTos());
                        break;
                    case TEXT:
                        sendText(msg.getSubject(), msg.getContent(), msg.getTos());
                        break;
                    default:
                        unhandled(message);
                }
            } else if (message instanceof TemplateMessage) {
                TemplateMessage msg = (TemplateMessage) message;
                switch (msg.getType()) {
                    case HTML:
                        ((HtmlEmail) msg.getEmail()).setHtmlMsg(renderTemplate(msg.getTemplate(), msg.getModel()));
                        send(msg.getEmail());
                        break;
                    case TEXT:
                        msg.getEmail().setMsg(renderTemplate(msg.getTemplate(), msg.getModel()));
                        send(msg.getEmail());
                        break;
                    default:
                        unhandled(message);
                }
            } else {
                unhandled(message);
            }
        }
    }
}
