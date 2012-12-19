/*
 * Copyright (c) 2010.  Korwe Software
 *
 *  This file is part of TheCore.
 *
 *  TheCore is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheCore is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with TheCore.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.korwe.thecore.api;

import com.korwe.thecore.messages.CoreMessage;
import com.korwe.thecore.messages.CoreMessageSerializer;
import com.korwe.thecore.messages.CoreMessageXmlSerializer;
import org.apache.log4j.Logger;
import org.apache.qpid.transport.*;

/**
 * @author <a href="mailto:nithia.govender@korwe.com>Nithia Govender</a>
 */
public class CoreSender {

    private static final Logger LOG = Logger.getLogger(CoreSender.class);

    private final MessageQueue queue;
    private Connection connection;
    private CoreMessageSerializer serializer;

    public CoreSender(MessageQueue queue) {
        this.queue = queue;
        connection = new Connection();
        CoreConfig config = CoreConfig.getConfig();
        if (LOG.isInfoEnabled()) {
            LOG.info("Connecting to queue server " + config.getSetting("amqp_server"));
        }
        connection.connect(config.getSetting("amqp_server"), config.getIntSetting("amqp_port"),
                           config.getSetting("amqp_vhost"), config.getSetting("amqp_user"),
                           config.getSetting("amqp_password"));
        if (LOG.isInfoEnabled()) {
            LOG.info("Connected");
        }
        serializer = new CoreMessageXmlSerializer();
    }

    public void close() {
        connection.close();
    }

    public void sendMessage(CoreMessage message) {
        if (queue.isDirect()) {
            String destination = MessageQueue.DIRECT_EXCHANGE;
            String routing = queue.getQueueName();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending to " + routing);
            }

            send(message, destination, routing);
        }
        else {
            LOG.error("Message to topic queue must be explicitly addressed");
        }
    }

    public void sendMessage(CoreMessage message, String recipient) {
        if (queue.isTopic()) {
            String destination = MessageQueue.TOPIC_EXCHANGE;
            String routing = queue.getQueueName() + "." + recipient;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending to " + routing);
            }

            send(message, destination, routing);
        }
        else {
            LOG.error("Cannot send to explicitly addressed message direct point to point queue");
        }

    }

    private void send(CoreMessage message, String destination, String routing) {
        String serialized = serializer.serialize(message);
        DeliveryProperties props = new DeliveryProperties();
        props.setRoutingKey(routing);
        Session session = connection.createSession();
        session.messageTransfer(destination, MessageAcceptMode.EXPLICIT, MessageAcquireMode.PRE_ACQUIRED,
                                new Header(props), serialized);
        session.sync();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sent: " + serialized);
        }
        session.close();
    }

    public MessageQueue getQueue() {
        return queue;
    }

}